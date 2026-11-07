package com.example

import com.example.domain.BloodPressure
import com.example.domain.BreastSelfExam
import com.example.domain.CycleInsights
import com.example.domain.FertilityChance
import com.example.domain.PillPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HealthToolsTest {

    private val today = LocalDate.of(2026, 11, 5)

    private fun period(start: LocalDate, days: Int) = (0 until days).map { start.plusDays(it.toLong()) }

    @Test
    fun insights_emptyData_hasNoFlags() {
        val r = CycleInsights.analyze(emptyList(), today)
        assertTrue(r.cycles.isEmpty())
        assertTrue(r.flags.isEmpty())
        assertNull(r.averageCycle)
    }

    @Test
    fun insights_regularCycles_reportOk() {
        val starts = (0 until 4).map { today.minusDays(100).plusDays(28L * it) }
        val days = starts.flatMap { period(it, 5) }
        val r = CycleInsights.analyze(days, today)
        assertEquals(4, r.cycles.size)
        assertEquals(3, r.completedCycles)
        assertEquals(28, r.averageCycle)
        assertEquals(0, r.variation)
        assertEquals(5, r.averageBleeding)
        assertEquals(listOf("ok"), r.flags.map { it.code })
    }

    @Test
    fun insights_irregularAndLongCycles_areFlagged() {
        val s1 = today.minusDays(150)
        val starts = listOf(s1, s1.plusDays(30), s1.plusDays(30 + 45), s1.plusDays(30 + 45 + 40))
        val r = CycleInsights.analyze(starts.flatMap { period(it, 4) }, today)
        val codes = r.flags.map { it.code }
        assertTrue(codes.contains("long"))
        assertTrue(codes.contains("irregular"))
        assertEquals(15, r.variation)
    }

    @Test
    fun insights_longBleedingAndMissedPeriod_areFlagged() {
        val s1 = today.minusDays(200)
        val days = period(s1, 9) + period(s1.plusDays(28), 10)
        val r = CycleInsights.analyze(days, today)
        val codes = r.flags.map { it.code }
        assertTrue(codes.contains("long_bleeding"))
        assertTrue(codes.contains("missed"))
    }

    @Test
    fun insights_futureDaysIgnored() {
        val r = CycleInsights.analyze(listOf(today.plusDays(3)), today)
        assertTrue(r.cycles.isEmpty())
    }

    @Test
    fun bloodPressure_categories() {
        assertEquals(BloodPressure.Category.NORMAL, BloodPressure.classify(115, 75))
        assertEquals(BloodPressure.Category.ELEVATED, BloodPressure.classify(125, 75))
        assertEquals(BloodPressure.Category.STAGE_1, BloodPressure.classify(118, 85))
        assertEquals(BloodPressure.Category.STAGE_2, BloodPressure.classify(145, 85))
        assertEquals(BloodPressure.Category.CRISIS, BloodPressure.classify(185, 100))
        assertEquals(BloodPressure.Category.CRISIS, BloodPressure.classify(150, 125))
        assertEquals(BloodPressure.Category.LOW, BloodPressure.classify(85, 55))
        assertTrue(BloodPressure.isValid(120, 80))
        assertFalse(BloodPressure.isValid(80, 120))
        assertFalse(BloodPressure.isValid(400, 80))
    }

    @Test
    fun pillPack_dayCalculation() {
        val start = LocalDate.of(2026, 11, 1)
        val type = PillPack.Type.P21_7
        assertNull(PillPack.dayFor(start, type, start.minusDays(1)))
        val d1 = PillPack.dayFor(start, type, start)!!
        assertEquals(1, d1.dayInPack); assertEquals(1, d1.packNumber); assertTrue(d1.isActivePill)
        val d22 = PillPack.dayFor(start, type, start.plusDays(21))!!
        assertEquals(22, d22.dayInPack); assertFalse(d22.isActivePill)
        val next = PillPack.dayFor(start, type, start.plusDays(28))!!
        assertEquals(1, next.dayInPack); assertEquals(2, next.packNumber)
        assertTrue(PillPack.dayFor(start, PillPack.Type.P28, start.plusDays(27))!!.isActivePill)
        assertEquals(PillPack.Type.P24_4, PillPack.Type.fromCode("24_4"))
        assertEquals(PillPack.Type.P21_7, PillPack.Type.fromCode("bogus"))
    }

    @Test
    fun pillPack_missedDaysSkipBreakWeek() {
        val start = LocalDate.of(2026, 10, 1)
        val taken = (0 until 20).map { start.plusDays(it.toLong()) }.toSet()
        val missed = PillPack.missedActiveDays(start, PillPack.Type.P21_7, taken, start, start.plusDays(27))
        assertEquals(listOf(start.plusDays(20)), missed)
    }

    @Test
    fun fertilityChance_peaksBeforeOvulation() {
        // 28-day cycle: ovulation on day 14.
        assertEquals(FertilityChance.Level.HIGH, FertilityChance.level(14, 28, false))
        assertEquals(FertilityChance.Level.HIGH, FertilityChance.level(12, 28, false))
        assertEquals(FertilityChance.Level.MEDIUM, FertilityChance.level(9, 28, false))
        assertEquals(FertilityChance.Level.MEDIUM, FertilityChance.level(15, 28, false))
        assertEquals(FertilityChance.Level.LOW, FertilityChance.level(20, 28, false))
        assertEquals(FertilityChance.Level.LOW, FertilityChance.level(13, 28, true))
    }

    @Test
    fun breastExam_followsCycleDaySeven() {
        val lastStart = today.minusDays(3)
        assertEquals(lastStart.plusDays(6), BreastSelfExam.nextDate(lastStart, 28, today))
        val lastStart2 = today.minusDays(10)
        assertEquals(lastStart2.plusDays(28 + 6), BreastSelfExam.nextDate(lastStart2, 28, today))
        // No usable cycle: same day every month.
        assertEquals(LocalDate.of(2026, 12, 1), BreastSelfExam.nextDate(null, 28, today))
        assertTrue(BreastSelfExam.isDue(null, today))
        assertFalse(BreastSelfExam.isDue(today.minusDays(10), today))
        assertTrue(BreastSelfExam.isDue(today.minusDays(40), today))
    }
}
