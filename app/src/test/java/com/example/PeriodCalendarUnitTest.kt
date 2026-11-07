package com.example

import com.example.utils.JalaliCalendar
import com.example.utils.JalaliDate
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PeriodCalendarUnitTest {

    @Test
    fun testJalaliDateConversionAndComparison() {
        val gregorianDate = LocalDate.of(2026, 5, 15)
        val jalaliDate = JalaliCalendar.gregorianToJalali(gregorianDate)
        
        assertNotNull(jalaliDate)
        assertTrue(jalaliDate.year >= 1405)
        assertTrue(jalaliDate.month in 1..12)
        assertTrue(jalaliDate.day in 1..31)

        val backToGregorian = jalaliDate.toLocalDate()
        assertEquals(gregorianDate, backToGregorian)
    }

    @Test
    fun testPeriodRangeDuration() {
        val start = LocalDate.of(2026, 5, 10)
        val end = LocalDate.of(2026, 5, 15)

        val durationDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
        assertEquals(6, durationDays)

        val startJalali = JalaliCalendar.gregorianToJalali(start)
        val endJalali = JalaliCalendar.gregorianToJalali(end)
        assertTrue(startJalali <= endJalali)
    }

    @Test
    fun nowruzDatesMatchTheOfficialCalendar() {
        assertEquals(JalaliDate(1403, 1, 1), JalaliCalendar.gregorianToJalali(LocalDate.of(2024, 3, 20)))
        assertEquals(JalaliDate(1404, 1, 1), JalaliCalendar.gregorianToJalali(LocalDate.of(2025, 3, 21)))
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.gregorianToJalali(LocalDate.of(2026, 3, 21)))
        assertEquals(LocalDate.of(2024, 3, 20), JalaliCalendar.jalaliToGregorian(1403, 1, 1))
        // 30 Esfand exists only in a leap year (1403 is leap, 1404 is not).
        assertEquals(JalaliDate(1403, 12, 30), JalaliCalendar.gregorianToJalali(LocalDate.of(2025, 3, 20)))
        assertEquals(30, JalaliDate(1403, 12, 1).lengthOfMonth)
        assertEquals(29, JalaliDate(1404, 12, 1).lengthOfMonth)
    }

    @Test
    fun everyDayRoundTripsAndMonthLengthsAgreeWithTheConverter() {
        var date = LocalDate.of(2000, 1, 1)
        val end = LocalDate.of(2060, 12, 31)
        var previous = JalaliCalendar.gregorianToJalali(date.minusDays(1))
        while (!date.isAfter(end)) {
            val j = JalaliCalendar.gregorianToJalali(date)
            assertEquals(date, j.toLocalDate())
            // Consecutive days: either the next day of the month or day 1 after the month's last day.
            if (j.day == 1) {
                assertEquals(previous.lengthOfMonth, previous.day)
            } else {
                assertEquals(previous.day + 1, j.day)
            }
            previous = j
            date = date.plusDays(1)
        }
    }

    @Test
    fun firstWeekdayUsesSaturdayAsColumnZero() {
        // 1 Farvardin 1403 was a Wednesday -> column 4 (Sat=0 ... Fri=6).
        assertEquals(4, JalaliDate(1403, 1, 1).firstDayOfWeek())
        // 1 Farvardin 1404 was a Friday -> column 6.
        assertEquals(6, JalaliDate(1404, 1, 1).firstDayOfWeek())
    }
}
