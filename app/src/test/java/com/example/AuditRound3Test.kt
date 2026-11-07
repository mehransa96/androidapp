package com.example

import com.example.domain.CyclePredictor
import com.example.domain.PregnancyMath
import com.example.domain.TimeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AuditRound3Test {

    private val today = LocalDate.of(2026, 11, 7)
    private fun period(start: LocalDate, days: Int = 5) = (0 until days).map { start.plusDays(it.toLong()) }

    // ---- Medicine time ----

    @Test fun timeAcceptsPersianAndCommonFormats() {
        assertEquals("14:30", TimeText.normalize("۱۴:۳۰"))
        assertEquals("14:30", TimeText.normalize("14.30"))
        assertEquals("09:00", TimeText.normalize("9"))
        assertEquals("09:05", TimeText.normalize("0905"))
        assertEquals("21:00", TimeText.normalize(" ٢١:٠٠ "))
        assertEquals(14 to 30, TimeText.parse("14:30"))
    }

    @Test fun timeRejectsInvalid() {
        assertNull(TimeText.normalize(""))
        assertNull(TimeText.normalize("25:00"))
        assertNull(TimeText.normalize("12:75"))
        assertNull(TimeText.normalize("abc"))
        assertNull(TimeText.normalize("1:2:3"))
    }

    // ---- Real cycle lengths for the chart ----

    @Test fun recentCyclesAreRealIntervals() {
        val days = period(LocalDate.of(2026, 7, 1)) + period(LocalDate.of(2026, 7, 29)) +
            period(LocalDate.of(2026, 8, 30)) + period(LocalDate.of(2026, 10, 1))
        val cycles = CyclePredictor.recentCycles(days, today)
        assertEquals(listOf(28, 32, 32), cycles.map { it.lengthDays })
        assertEquals(LocalDate.of(2026, 7, 1), cycles.first().start)
        assertTrue(cycles.all { it.isTypical })
    }

    @Test fun recentCyclesNeedTwoPeriodsAndFlagOutliers() {
        assertTrue(CyclePredictor.recentCycles(period(LocalDate.of(2026, 10, 1)), today).isEmpty())
        val withGap = period(LocalDate.of(2026, 6, 1)) + period(LocalDate.of(2026, 8, 5))
        val c = CyclePredictor.recentCycles(withGap, today).single()
        assertEquals(65, c.lengthDays)
        assertFalse(c.isTypical)
    }

    @Test fun predictionUsesTheGivenToday() {
        val days = period(LocalDate.of(2026, 10, 1))
        val before = CyclePredictor.predict(days, 28, LocalDate.of(2026, 10, 28))
        val after = CyclePredictor.predict(days, 28, LocalDate.of(2026, 10, 30))
        assertEquals(1, before.daysRemaining)
        assertFalse(before.isLate)
        assertEquals(-1, after.daysRemaining)
        assertTrue(after.isLate)
    }

    // ---- Pregnancy dating ----

    @Test fun pregnancyWithoutLmpGuessesNothing() {
        val e = PregnancyMath.estimate(null, 28, today)
        assertEquals(PregnancyMath.Status.NO_LMP, e.status)
        assertNull(e.dueDate)
        assertEquals(1, e.week)
    }

    @Test fun pregnancyDueDateFollowsCycleLength() {
        val lmp = today.minusDays(70) // 10 weeks
        val normal = PregnancyMath.estimate(lmp, 28, today)
        assertEquals(PregnancyMath.Status.OK, normal.status)
        assertEquals(11, normal.week)
        assertEquals(lmp.plusDays(280), normal.dueDate)
        val long = PregnancyMath.estimate(lmp, 35, today)
        assertEquals(lmp.plusDays(287), long.dueDate)
        assertEquals(10, long.week)
    }

    @Test fun pregnancyWithVeryOldLmpIsNotTrusted() {
        val e = PregnancyMath.estimate(today.minusDays(400), 28, today)
        assertEquals(PregnancyMath.Status.TOO_OLD, e.status)
        assertNull(e.dueDate)
    }
}
