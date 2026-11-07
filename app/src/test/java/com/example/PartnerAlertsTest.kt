package com.example

import com.example.domain.PartnerAlerts
import com.example.domain.PartnerAlerts.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PartnerAlertsTest {

    private val lastStart = LocalDate.of(2026, 11, 1) // cycle day 1
    private val allEvents = Event.values().toSet()

    @Test
    fun upcoming_findsEveryEventInTheCurrentCycle() {
        val today = LocalDate.of(2026, 11, 3)
        val list = PartnerAlerts.upcoming(lastStart, 28, today, allEvents, daysBeforePeriod = 2)
        val byEvent = list.associate { it.event to it.date }
        assertEquals(LocalDate.of(2026, 11, 29), byEvent[Event.PERIOD_START])
        assertEquals(LocalDate.of(2026, 11, 27), byEvent[Event.PERIOD_SOON])
        assertEquals(LocalDate.of(2026, 11, 24), byEvent[Event.PMS_START])
        // ovulation day 14 -> fertile window starts on cycle day 9
        assertEquals(LocalDate.of(2026, 11, 9), byEvent[Event.FERTILE_START])
        assertEquals(list.sortedBy { it.date }, list)
    }

    @Test
    fun upcoming_includesToday() {
        val today = LocalDate.of(2026, 11, 24) // PMS starts today
        val list = PartnerAlerts.upcoming(lastStart, 28, today, setOf(Event.PMS_START), 2)
        assertEquals(0L, list.single().daysFromToday)
    }

    @Test
    fun upcoming_rollsToNextCycleOnceDayHasPassed() {
        val today = LocalDate.of(2026, 11, 10)
        val list = PartnerAlerts.upcoming(lastStart, 28, today, setOf(Event.FERTILE_START), 2)
        assertEquals(LocalDate.of(2026, 12, 7), list.single().date)
    }

    @Test
    fun upcoming_sameDayIsOnlyNotifiedOnce() {
        // 5 days before the period, "period soon" and "PMS" fall on the same day.
        val list = PartnerAlerts.upcoming(lastStart, 28, LocalDate.of(2026, 11, 3), setOf(Event.PERIOD_SOON, Event.PMS_START), 5)
        assertEquals(1, list.size)
    }

    @Test
    fun upcoming_withoutDataOrEventsIsEmpty() {
        assertTrue(PartnerAlerts.upcoming(null, 28, lastStart, allEvents, 2).isEmpty())
        assertTrue(PartnerAlerts.upcoming(lastStart, 28, lastStart, emptySet(), 2).isEmpty())
    }

    @Test
    fun discreetMessagesNeverMentionTheCycle() {
        val words = listOf("پریود", "قاعدگی", "PMS", "باروری")
        for (event in Event.values()) {
            val text = PartnerAlerts.message(event, "علی", PartnerAlerts.Tone.DISCREET, 2)
            assertTrue(text.contains("علی"))
            words.forEach { assertFalse("$event: $text", text.contains(it)) }
        }
    }

    @Test
    fun warmMessageUsesDaysBefore() {
        val text = PartnerAlerts.message(Event.PERIOD_SOON, "", PartnerAlerts.Tone.WARM, 3)
        assertTrue(text.contains("3 روز"))
        assertTrue(text.startsWith("عزیزم"))
    }

    @Test
    fun normalizePhone_handlesPersianDigitsAndFormats() {
        assertEquals("09121234567", PartnerAlerts.normalizePhone("۰۹۱۲ ۱۲۳ ۴۵۶۷"))
        assertEquals("+989121234567", PartnerAlerts.normalizePhone("+98 912 123 4567"))
        assertEquals("+989121234567", PartnerAlerts.normalizePhone("00989121234567"))
        assertEquals("09121234567", PartnerAlerts.normalizePhone("9121234567"))
        assertNull(PartnerAlerts.normalizePhone("12"))
        assertNull(PartnerAlerts.normalizePhone("abc"))
    }

    @Test
    fun eventForToday_prefersPeriodThenSoonThenPms() {
        assertEquals(Event.PERIOD_START, PartnerAlerts.eventForToday(true, false, false, 0, 2))
        assertEquals(Event.PERIOD_SOON, PartnerAlerts.eventForToday(false, true, false, 2, 2))
        assertEquals(Event.PMS_START, PartnerAlerts.eventForToday(false, true, false, 4, 2))
        assertEquals(Event.FERTILE_START, PartnerAlerts.eventForToday(false, false, true, 15, 2))
        assertNull(PartnerAlerts.eventForToday(false, false, false, 20, 2))
    }
}
