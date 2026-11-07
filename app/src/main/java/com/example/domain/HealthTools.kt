package com.example.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure, Android-free logic behind the "health tools" (cycle insights, blood pressure, pill pack,
 * fertility chance, breast self-exam). Everything here is unit-tested and only gives general
 * information; the UI always pairs it with a "this is not a diagnosis" note.
 */

// ---------------------------------------------------------------------------------------------
// Cycle health insights and irregularity flags
// ---------------------------------------------------------------------------------------------

object CycleInsights {

    /** Typical adult range used by gynaecology guidelines (ACOG / NHS): 21..35 days. */
    const val TYPICAL_MIN_CYCLE = 21
    const val TYPICAL_MAX_CYCLE = 35
    /** Bleeding for more than 7 days is worth discussing with a doctor. */
    const val LONG_PERIOD_DAYS = 8
    /** Cycle-to-cycle differences above this are commonly described as irregular. */
    const val IRREGULAR_VARIATION_DAYS = 8
    /** No period for 90 days (outside pregnancy) should be checked by a doctor. */
    const val MISSED_PERIOD_DAYS = 90

    enum class Severity { INFO, WATCH, SEE_DOCTOR }

    data class Cycle(
        val start: LocalDate,
        /** Days until the next period started; null for the current (open) cycle. */
        val length: Int?,
        val bleedingDays: Int
    )

    data class Flag(val code: String, val severity: Severity, val title: String, val message: String)

    data class Result(
        val cycles: List<Cycle>,
        val averageCycle: Int?,
        val shortestCycle: Int?,
        val longestCycle: Int?,
        val variation: Int?,
        val averageBleeding: Int?,
        val daysSinceLastStart: Int?,
        val flags: List<Flag>
    ) {
        val completedCycles: Int get() = cycles.count { it.length != null }
    }

    /**
     * Splits logged bleeding days into periods (same rule as [CyclePredictor]) and derives
     * statistics plus plain-language flags. Needs no minimum; flags only appear when the data
     * supports them.
     */
    fun analyze(bleedingDays: Collection<LocalDate>, today: LocalDate = LocalDate.now()): Result {
        val days = bleedingDays.filter { !it.isAfter(today) }.distinct().sorted()
        if (days.isEmpty()) {
            return Result(emptyList(), null, null, null, null, null, null, emptyList())
        }
        // Group consecutive-ish bleeding days into periods.
        val runs = mutableListOf<MutableList<LocalDate>>(mutableListOf(days.first()))
        for (i in 1 until days.size) {
            if (ChronoUnit.DAYS.between(days[i - 1], days[i]) > CyclePredictor.NEW_PERIOD_GAP_DAYS) {
                runs.add(mutableListOf())
            }
            runs.last().add(days[i])
        }
        val cycles = runs.mapIndexed { index, run ->
            val start = run.first()
            val bleeding = ChronoUnit.DAYS.between(start, run.last()).toInt() + 1
            val length = runs.getOrNull(index + 1)?.let { ChronoUnit.DAYS.between(start, it.first()).toInt() }
            Cycle(start, length, bleeding)
        }
        // Very short/long gaps are almost always missing data, not real cycles.
        val lengths = cycles.mapNotNull { it.length }.filter { it in 15..90 }.takeLast(12)
        val informativeBleeding = cycles.map { it.bleedingDays }.filter { it > 1 }.takeLast(12)
        val avg = if (lengths.isNotEmpty()) Math.round(lengths.average()).toInt() else null
        val shortest = lengths.minOrNull()
        val longest = lengths.maxOrNull()
        val variation = if (lengths.size >= 2) longest!! - shortest!! else null
        val avgBleeding = if (informativeBleeding.isNotEmpty()) Math.round(informativeBleeding.average()).toInt() else null
        val sinceLast = ChronoUnit.DAYS.between(cycles.last().start, today).toInt()

        val flags = mutableListOf<Flag>()
        if (sinceLast >= MISSED_PERIOD_DAYS) {
            flags += Flag(
                "missed", Severity.SEE_DOCTOR,
                "بیش از ۹۰ روز بدون ثبت پریود",
                "اگر باردار نیستید و پریود را فراموش نکرده‌اید ثبت کنید، نبود قاعدگی به مدت سه ماه بهتر است با پزشک بررسی شود."
            )
        }
        if (avg != null && lengths.size >= 2 && avg < TYPICAL_MIN_CYCLE) {
            flags += Flag(
                "short", Severity.SEE_DOCTOR,
                "چرخه‌های کوتاه‌تر از ۲۱ روز",
                "میانگین چرخه شما $avg روز است. چرخه‌های مکرراً کوتاه‌تر از ۲۱ روز ارزش مشورت با متخصص زنان را دارد."
            )
        }
        if (avg != null && lengths.size >= 2 && avg > TYPICAL_MAX_CYCLE) {
            flags += Flag(
                "long", Severity.SEE_DOCTOR,
                "چرخه‌های طولانی‌تر از ۳۵ روز",
                "میانگین چرخه شما $avg روز است. چرخه‌های مکرراً طولانی (مثلاً در سندرم تخمدان پلی‌کیستیک یا مشکلات تیروئید) بهتر است بررسی شود."
            )
        }
        if (variation != null && lengths.size >= 3 && variation >= IRREGULAR_VARIATION_DAYS) {
            flags += Flag(
                "irregular", Severity.WATCH,
                "نوسان زیاد بین چرخه‌ها",
                "اختلاف کوتاه‌ترین و بلندترین چرخه شما $variation روز است؛ پیش‌بینی‌ها در این حالت دقت کمتری دارند. استرس، خواب، ورزش شدید و تغییر وزن می‌توانند اثر بگذارند."
            )
        }
        val longBleeds = cycles.takeLast(6).count { it.bleedingDays >= LONG_PERIOD_DAYS }
        if (longBleeds >= 2) {
            flags += Flag(
                "long_bleeding", Severity.SEE_DOCTOR,
                "خونریزی طولانی‌تر از ۷ روز",
                "در $longBleeds دوره اخیر خونریزی بیش از ۷ روز ثبت شده است. خونریزی طولانی یا بسیار شدید می‌تواند باعث کم‌خونی شود؛ با پزشک مشورت کنید."
            )
        }
        if (flags.isEmpty() && lengths.size >= 3) {
            flags += Flag(
                "ok", Severity.INFO,
                "چرخه شما در محدوده معمول است",
                "طول و نظم چرخه‌های ثبت‌شده در محدوده رایج قرار دارد. ثبت منظم داده‌ها دقت پیش‌بینی را بیشتر می‌کند."
            )
        }
        return Result(cycles, avg, shortest, longest, variation, avgBleeding, sinceLast, flags)
    }
}

// ---------------------------------------------------------------------------------------------
// Blood pressure (2017 ACC/AHA categories)
// ---------------------------------------------------------------------------------------------

object BloodPressure {
    enum class Category(val label: String, val advice: String) {
        LOW("پایین", "اگر سرگیجه، تاری دید یا ضعف دارید با پزشک مشورت کنید."),
        NORMAL("طبیعی", "فشار خون شما در محدوده طبیعی است."),
        ELEVATED("بالاتر از طبیعی", "سبک زندگی سالم (کم‌نمک، ورزش، خواب کافی) را ادامه دهید و فشار را دوباره اندازه بگیرید."),
        STAGE_1("پرفشاری مرحله ۱", "چند بار در روزهای مختلف اندازه بگیرید و نتیجه را به پزشک نشان دهید."),
        STAGE_2("پرفشاری مرحله ۲", "لازم است به‌زودی با پزشک مشورت کنید."),
        CRISIS("بحرانی", "چند دقیقه استراحت کنید و دوباره بسنجید. اگر هنوز بالاست یا سردرد شدید، درد قفسه سینه، تنگی نفس یا تاری دید دارید فوراً به اورژانس (۱۱۵) مراجعه کنید.")
    }

    fun isValid(systolic: Int, diastolic: Int): Boolean =
        systolic in 60..260 && diastolic in 30..160 && systolic > diastolic

    fun classify(systolic: Int, diastolic: Int): Category = when {
        systolic > 180 || diastolic > 120 -> Category.CRISIS
        systolic >= 140 || diastolic >= 90 -> Category.STAGE_2
        systolic >= 130 || diastolic >= 80 -> Category.STAGE_1
        systolic >= 120 -> Category.ELEVATED
        systolic < 90 || diastolic < 60 -> Category.LOW
        else -> Category.NORMAL
    }
}

// ---------------------------------------------------------------------------------------------
// Combined oral contraceptive pill pack
// ---------------------------------------------------------------------------------------------

object PillPack {
    enum class Type(val code: String, val label: String, val activeDays: Int, val packDays: Int) {
        P21_7("21_7", "۲۱ قرص + ۷ روز استراحت", 21, 28),
        P24_4("24_4", "۲۴ قرص فعال + ۴ قرص بی‌اثر", 24, 28),
        P28("28", "۲۸ قرص فعال (پیوسته)", 28, 28);

        companion object {
            fun fromCode(code: String?): Type = entries.firstOrNull { it.code == code } ?: P21_7
        }
    }

    data class Day(
        /** 1-based day in the current pack. */
        val dayInPack: Int,
        /** 1-based pack number since the start date. */
        val packNumber: Int,
        /** True when an active (hormone) pill is due today. */
        val isActivePill: Boolean,
        val daysUntilNextPack: Int
    )

    /** Null when [today] is before the start date. */
    fun dayFor(packStart: LocalDate, type: Type, today: LocalDate): Day? {
        val diff = ChronoUnit.DAYS.between(packStart, today)
        if (diff < 0) return null
        val dayInPack = (diff % type.packDays).toInt() + 1
        val pack = (diff / type.packDays).toInt() + 1
        return Day(
            dayInPack = dayInPack,
            packNumber = pack,
            isActivePill = dayInPack <= type.activeDays,
            daysUntilNextPack = type.packDays - dayInPack + 1
        )
    }

    /** Active-pill days in [from]..[to] that have no "taken" entry (for the missed-pill hint). */
    fun missedActiveDays(
        packStart: LocalDate,
        type: Type,
        takenDays: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate
    ): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var d = if (from.isBefore(packStart)) packStart else from
        while (!d.isAfter(to)) {
            val day = dayFor(packStart, type, d)
            if (day != null && day.isActivePill && d !in takenDays) result += d
            d = d.plusDays(1)
        }
        return result
    }
}

// ---------------------------------------------------------------------------------------------
// Chance of pregnancy (calendar estimate, never contraception)
// ---------------------------------------------------------------------------------------------

object FertilityChance {
    enum class Level(val label: String) { LOW("کم"), MEDIUM("متوسط"), HIGH("بالا") }

    /**
     * Calendar-method estimate for one day. Ovulation day and the two days before it carry the
     * highest chance; the rest of the 6-day window is medium; other days are low but never zero.
     */
    fun level(cycleDay: Int, cycleLength: Int, isPeriod: Boolean): Level {
        if (isPeriod) return Level.LOW
        val ovulation = CyclePredictor.ovulationDay(cycleLength)
        return when (cycleDay) {
            in (ovulation - 2)..ovulation -> Level.HIGH
            in (ovulation - 5)..(ovulation + 1) -> Level.MEDIUM
            else -> Level.LOW
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Monthly breast self-exam
// ---------------------------------------------------------------------------------------------

object BreastSelfExam {
    /** A few days after bleeding ends breasts are least tender: cycle day 7 is a good default. */
    const val CYCLE_DAY = 7

    /**
     * Next recommended exam date strictly after [today]. With a known cycle it follows cycle day
     * [CYCLE_DAY]; without one (or when cycles are absent) it falls on the same day each month.
     */
    fun nextDate(lastPeriodStart: LocalDate?, cycleLength: Int, today: LocalDate, fallbackDayOfMonth: Int = 1): LocalDate {
        if (lastPeriodStart != null && ChronoUnit.DAYS.between(lastPeriodStart, today) <= 90) {
            return CyclePredictor.nextOccurrenceOfCycleDay(lastPeriodStart, cycleLength, CYCLE_DAY, today)
        }
        val day = fallbackDayOfMonth.coerceIn(1, 28)
        val thisMonth = today.withDayOfMonth(day)
        return if (thisMonth.isAfter(today)) thisMonth else thisMonth.plusMonths(1)
    }

    /** True when the last exam is older than ~5 weeks (or never done). */
    fun isDue(lastExam: LocalDate?, today: LocalDate): Boolean =
        lastExam == null || ChronoUnit.DAYS.between(lastExam, today) >= 35
}
