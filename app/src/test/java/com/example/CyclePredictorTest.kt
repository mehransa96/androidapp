package com.example

import com.example.domain.CyclePredictor
import com.example.domain.CyclePredictor.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {

    private val today = LocalDate.of(2026, 3, 20)

    private fun period(start: LocalDate, days: Int) = (0 until days).map { start.plusDays(it.toLong()) }

    @Test
    fun `no data falls back to the user cycle length`() {
        val p = CyclePredictor.predict(emptyList(), 30, today)
        assertFalse(p.hasEnoughData)
        assertNull(p.lastPeriodStart)
        assertEquals(30, p.averageCycleLength)
        assertEquals(today.plusDays(30), p.predictedNextStart)
    }

    @Test
    fun `invalid default cycle length is clamped to 28`() {
        assertEquals(28, CyclePredictor.predict(emptyList(), 90, today).averageCycleLength)
        assertEquals(28, CyclePredictor.predict(emptyList(), 5, today).averageCycleLength)
    }

    @Test
    fun `average of real cycles is used for prediction`() {
        val s1 = LocalDate.of(2026, 1, 1)
        val s2 = s1.plusDays(30)
        val s3 = s2.plusDays(30)
        val days = period(s1, 5) + period(s2, 4) + period(s3, 6)
        val p = CyclePredictor.predict(days, 28, today)
        assertTrue(p.hasEnoughData)
        assertEquals(30, p.averageCycleLength)
        assertEquals(s3, p.lastPeriodStart)
        assertEquals(s3.plusDays(30), p.predictedNextStart)
        assertEquals(5, p.averagePeriodLength)
        assertEquals(2, p.cyclesUsed)
    }

    @Test
    fun `consecutive bleeding days are one period`() {
        val s = LocalDate.of(2026, 3, 1)
        assertEquals(listOf(s), CyclePredictor.periodStarts(period(s, 7)))
    }

    @Test
    fun `future dated entries are ignored`() {
        val past = LocalDate.of(2026, 3, 1)
        val p = CyclePredictor.predict(period(past, 5) + today.plusDays(10), 28, today)
        assertEquals(past, p.lastPeriodStart)
    }

    @Test
    fun `late period is detected`() {
        val start = today.minusDays(35)
        val p = CyclePredictor.predict(period(start, 5), 28, today)
        assertTrue(p.isLate)
        assertEquals(-7, p.daysRemaining)
    }

    @Test
    fun `irregular cycles are flagged`() {
        val s1 = LocalDate.of(2025, 10, 1)
        val s2 = s1.plusDays(24)
        val s3 = s2.plusDays(38)
        val s4 = s3.plusDays(26)
        val p = CyclePredictor.predict(listOf(s1, s2, s3, s4), 28, today)
        assertTrue(p.isIrregular)
    }

    @Test
    fun `ovulation depends on cycle length`() {
        assertEquals(14, CyclePredictor.ovulationDay(28))
        assertEquals(18, CyclePredictor.ovulationDay(32))
        assertEquals(10, CyclePredictor.ovulationDay(21))
    }

    @Test
    fun `phases follow the cycle`() {
        assertEquals(Phase.MENSTRUAL, CyclePredictor.phaseForCycleDay(2, 28))
        assertEquals(Phase.FOLLICULAR, CyclePredictor.phaseForCycleDay(8, 28))
        assertEquals(Phase.OVULATION, CyclePredictor.phaseForCycleDay(14, 28))
        assertEquals(Phase.LUTEAL, CyclePredictor.phaseForCycleDay(22, 28))
    }

    @Test
    fun `day status marks fertile and pms windows`() {
        val start = LocalDate.of(2026, 3, 1)
        val p = CyclePredictor.predict(period(start, 5), 28, start.plusDays(1))
        val fertile = CyclePredictor.dayStatus(start.plusDays(12), p, loggedBleedingToday = false)
        assertTrue(fertile.isFertile)
        val pms = CyclePredictor.dayStatus(start.plusDays(25), p, loggedBleedingToday = false)
        assertTrue(pms.isPms)
        assertEquals(3, pms.daysUntilNextPeriod)
    }

    @Test
    fun `a missed period is reported as late instead of a new cycle`() {
        val start = LocalDate.of(2026, 3, 1)
        val p = CyclePredictor.predict(period(start, 5), 28, start.plusDays(1))
        val dueDay = CyclePredictor.dayStatus(start.plusDays(28), p, loggedBleedingToday = false)
        assertEquals(0, dueDay.daysUntilNextPeriod)
        assertFalse(dueDay.isPeriod)
        assertFalse(dueDay.isLate)
        val late = CyclePredictor.dayStatus(start.plusDays(31), p, loggedBleedingToday = false)
        assertTrue(late.isLate)
        assertEquals(3, late.daysLate)
        assertEquals(32, late.cycleDay)
        assertFalse(late.isPeriod)
        assertFalse(late.isFertile)
    }

    @Test
    fun `logging bleeding on a late day ends the late state`() {
        val start = LocalDate.of(2026, 3, 1)
        val p = CyclePredictor.predict(period(start, 5), 28, start.plusDays(1))
        val status = CyclePredictor.dayStatus(start.plusDays(31), p, loggedBleedingToday = true)
        assertFalse(status.isLate)
        assertTrue(status.isPeriod)
    }

    @Test
    fun `next occurrence is always after today`() {
        val start = LocalDate.of(2026, 1, 1)
        val next = CyclePredictor.nextOccurrenceOfCycleDay(start, 28, 14, today)
        assertTrue(next.isAfter(today))
        assertEquals(0L, java.time.temporal.ChronoUnit.DAYS.between(start.plusDays(13), next) % 28)
    }

    @Test
    fun `bleeding days are grouped into periods with their real end day`() {
        val first = LocalDate.of(2026, 1, 1)
        val second = LocalDate.of(2026, 1, 29)
        val days = period(first, 5) + period(second, 4) + listOf(first.plusDays(2)) // duplicate ignored
        val episodes = CyclePredictor.periodEpisodes(days)
        assertEquals(listOf(first to first.plusDays(4), second to second.plusDays(3)), episodes)
        assertTrue(CyclePredictor.periodEpisodes(emptyList()).isEmpty())
    }

    @Test
    fun `next ovulation is today or later and matches the calendar model`() {
        val start = LocalDate.of(2026, 3, 1)
        val p = CyclePredictor.predict(period(start, 5), 28, today)
        // Cycle day 14 of the current cycle (2026-03-14) is already past on 2026-03-20.
        val next = CyclePredictor.nextOvulation(p, today)!!
        assertEquals(LocalDate.of(2026, 4, 11), next)
        assertEquals(LocalDate.of(2026, 3, 14), CyclePredictor.nextOvulation(p, LocalDate.of(2026, 3, 14)))
        val (from, to) = CyclePredictor.fertileWindow(next)
        assertEquals(next.minusDays(5), from)
        assertEquals(next.plusDays(1), to)
        // The fertile window agrees with dayStatus.
        assertTrue(CyclePredictor.dayStatus(from, p, false, today).isFertile)
        assertTrue(CyclePredictor.dayStatus(to, p, false, today).isFertile)
        assertNull(CyclePredictor.nextOvulation(CyclePredictor.predict(emptyList(), 28, today), today))
    }
}
