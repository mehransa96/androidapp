package com.example.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * "Partner mode": which days are sensitive and what message to prepare for the partner.
 * Pure Kotlin so it can be unit-tested without Android.
 *
 * Nothing is ever sent automatically. On a sensitive day the app shows a notification with a
 * ready-made message, and the user decides whether to send it (SMS app or any messenger).
 */
object PartnerAlerts {

    enum class Event(val code: String, val title: String, val emoji: String, val defaultOn: Boolean) {
        PERIOD_SOON("period_soon", "چند روز قبل از پریود", "⏳", true),
        PERIOD_START("period_start", "روز شروع پریود", "🩸", true),
        PMS_START("pms_start", "شروع روزهای PMS", "🍂", true),
        FERTILE_START("fertile_start", "شروع روزهای باروری", "🌸", false);

        companion object {
            fun fromCode(code: String): Event? = values().firstOrNull { it.code == code }
        }
    }

    /** How explicit the prepared message is. DISCREET never mentions period, PMS or fertility. */
    enum class Tone(val code: String, val title: String) {
        WARM("warm", "صمیمی و کامل"),
        DISCREET("discreet", "محرمانه (بدون ذکر جزئیات)");

        companion object {
            fun fromCode(code: String?): Tone = values().firstOrNull { it.code == code } ?: WARM
        }
    }

    data class Upcoming(val event: Event, val date: LocalDate, val daysFromToday: Long)

    /** Days before the predicted period that PMS reminders start (matches CyclePredictor.dayStatus). */
    const val PMS_DAYS_BEFORE = 5
    const val MIN_DAYS_BEFORE = 1
    const val MAX_DAYS_BEFORE = 5

    /**
     * Next occurrence (today or later) of every enabled event, soonest first, within [horizonDays].
     * [lastPeriodStart] and [cycleLength] come from the real logged data (CyclePredictor.predict).
     */
    fun upcoming(
        lastPeriodStart: LocalDate?,
        cycleLength: Int,
        today: LocalDate,
        enabled: Set<Event>,
        daysBeforePeriod: Int,
        horizonDays: Int = 60
    ): List<Upcoming> {
        if (lastPeriodStart == null || enabled.isEmpty()) return emptyList()
        val length = CyclePredictor.clampCycleLength(cycleLength)
        val before = daysBeforePeriod.coerceIn(MIN_DAYS_BEFORE, MAX_DAYS_BEFORE)
        val ovulation = CyclePredictor.ovulationDay(length)
        val result = mutableListOf<Upcoming>()
        for (event in enabled) {
            val cycleDay = when (event) {
                Event.PERIOD_START -> 1
                Event.PERIOD_SOON -> length + 1 - before
                Event.PMS_START -> length + 1 - PMS_DAYS_BEFORE
                Event.FERTILE_START -> ovulation - 5
            }
            // nextOccurrenceOfCycleDay is strictly after its "today", so ask from yesterday to include today.
            val date = CyclePredictor.nextOccurrenceOfCycleDay(lastPeriodStart, length, cycleDay, today.minusDays(1))
            val days = ChronoUnit.DAYS.between(today, date)
            if (days in 0..horizonDays.toLong()) result += Upcoming(event, date, days)
        }
        // When "period soon" and "PMS" fall on the same day, keep only one notification for that day.
        return result
            .sortedWith(compareBy<Upcoming> { it.date }.thenBy { it.event.ordinal })
            .distinctBy { it.date }
    }

    fun message(event: Event, partnerName: String, tone: Tone, daysBeforePeriod: Int): String {
        val name = partnerName.trim().ifBlank { "عزیزم" }
        val before = daysBeforePeriod.coerceIn(MIN_DAYS_BEFORE, MAX_DAYS_BEFORE)
        return when (tone) {
            Tone.WARM -> when (event) {
                Event.PERIOD_SOON ->
                    "$name جان سلام ❤️ طبق تقویمم حدود $before روز دیگه پریودم شروع می‌شه. این روزها ممکنه زودتر خسته یا حساس بشم؛ ممنون که حواست بهم هست 🌸"
                Event.PERIOD_START ->
                    "$name جان سلام ❤️ امروز روزهای قاعدگی‌ام شروع شده و کمی درد و خستگی دارم. یه دمنوش گرم، کمک تو کارهای خونه و صبوریت خیلی بهم آرامش می‌ده 🌸"
                Event.PMS_START ->
                    "$name جان سلام 🍂 از امروز وارد روزهای PMS می‌شم و ممکنه کم‌حوصله‌تر یا حساس‌تر باشم. اگه گاهی بی‌حوصله‌ام به دل نگیر؛ همراهی و مهربونیت برام یه دنیا ارزش داره ❤️"
                Event.FERTILE_START ->
                    "$name جان سلام 🌸 طبق تقویمم از امروز روزهای باروری‌ام شروع شده. خواستم در جریان باشی تا با هم برنامه‌ریزی کنیم 💕"
            }
            Tone.DISCREET -> when (event) {
                Event.PERIOD_SOON, Event.PMS_START ->
                    "$name جان سلام 🌸 این چند روز کمی بیشتر به آرامش و همراهی‌ات نیاز دارم. ممنون که هستی ❤️"
                Event.PERIOD_START ->
                    "$name جان سلام 🌸 امروز حالم کمی گرفته است و به استراحت و مهربونیت نیاز دارم. ممنون که هوامو داری ❤️"
                Event.FERTILE_START ->
                    "$name جان سلام 🌸 یه یادآوری از تقویم مشترکمون دارم؛ امشب با هم صحبت کنیم؟ 💕"
            }
        }
    }

    /** A message for an ordinary (non-sensitive) day. */
    fun everydayMessage(partnerName: String, tone: Tone): String {
        val name = partnerName.trim().ifBlank { "عزیزم" }
        return when (tone) {
            Tone.WARM -> "$name جان سلام 🌱 امروز حس خوب و انرژی دارم! خوشحالم که کنارمی؛ امشب یه کار دونفره بکنیم؟ 💕"
            Tone.DISCREET -> "$name جان سلام 🌱 امروز حالم خوبه و دلم برات تنگ شده. ممنون که هستی ❤️"
        }
    }

    /** The event that describes [today], used to pick the message shown in the partner tab. */
    fun eventForToday(isPeriod: Boolean, isPms: Boolean, isFertile: Boolean, daysUntilPeriod: Int, daysBeforePeriod: Int): Event? = when {
        isPeriod -> Event.PERIOD_START
        daysUntilPeriod in 1..daysBeforePeriod.coerceIn(MIN_DAYS_BEFORE, MAX_DAYS_BEFORE) -> Event.PERIOD_SOON
        isPms -> Event.PMS_START
        isFertile -> Event.FERTILE_START
        else -> null
    }

    /**
     * Normalises an Iranian or international mobile number for an `smsto:` link.
     * Returns null when the input cannot be a phone number.
     */
    fun normalizePhone(raw: String): String? {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        val ascii = raw.map { c ->
            val p = persianDigits.indexOf(c)
            val a = arabicDigits.indexOf(c)
            when {
                p >= 0 -> '0' + p
                a >= 0 -> '0' + a
                else -> c
            }
        }.joinToString("")
        val plus = ascii.trim().startsWith("+")
        val digits = ascii.filter { it.isDigit() }
        val normalized = when {
            plus -> "+$digits"
            digits.startsWith("00") -> "+" + digits.drop(2)
            digits.length == 10 && digits.startsWith("9") -> "0$digits" // 912... without leading zero
            else -> digits
        }
        val count = normalized.count { it.isDigit() }
        return if (count in 7..15) normalized else null
    }
}
