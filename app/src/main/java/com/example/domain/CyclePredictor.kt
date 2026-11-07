package com.example.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure, Android-free menstrual cycle prediction logic.
 *
 * All the cycle maths of the app lives here so it can be unit-tested and shared by the
 * ViewModel, the background reschedule worker and the notification helpers.
 *
 * Medical model (standard calendar method, for information only, not contraception):
 *  - A new period starts when a bleeding day follows a gap of more than [NEW_PERIOD_GAP_DAYS] days.
 *  - Cycle length = average of the last [MAX_CYCLES_FOR_AVERAGE] plausible cycles (21..45 days).
 *  - The luteal phase is ~14 days, so ovulation ≈ cycleLength - 14 (never before day 10).
 *  - Fertile window = 5 days before ovulation up to 1 day after it.
 *  - PMS window = the last 5 days before the next expected period.
 */
object CyclePredictor {

    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_LENGTH = 5
    const val MIN_CYCLE_LENGTH = 21
    const val MAX_CYCLE_LENGTH = 45
    const val LUTEAL_PHASE_DAYS = 14
    const val NEW_PERIOD_GAP_DAYS = 8
    const val MAX_CYCLES_FOR_AVERAGE = 6
    /** How long a missing period is reported as "late" before the projection takes over again. */
    const val MAX_LATE_DAYS = 45
    private const val MIN_PLAUSIBLE_INTERVAL = 15
    private const val MAX_PLAUSIBLE_INTERVAL = 60

    enum class Phase { MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL }

    data class Prediction(
        val hasEnoughData: Boolean,
        val averageCycleLength: Int,
        val averagePeriodLength: Int,
        val lastPeriodStart: LocalDate?,
        val predictedNextStart: LocalDate,
        /** Negative when the period is late. */
        val daysRemaining: Int,
        val cyclesUsed: Int,
        /** Max - min of the cycles used; > 7 days usually means an irregular cycle. */
        val cycleVariationDays: Int,
        val isLate: Boolean
    ) {
        val isIrregular: Boolean get() = cyclesUsed >= 3 && cycleVariationDays > 7
    }

    data class DayStatus(
        val cycleDay: Int,
        val daysUntilNextPeriod: Int,
        val predictedPeriodStart: LocalDate,
        val isPeriod: Boolean,
        val isFertile: Boolean,
        val isPms: Boolean,
        val phase: Phase,
        /** The expected period has not been logged yet; [daysLate] counts the days since it was due. */
        val isLate: Boolean = false,
        val daysLate: Int = 0
    )

    fun clampCycleLength(length: Int): Int =
        if (length in MIN_CYCLE_LENGTH..MAX_CYCLE_LENGTH) length else DEFAULT_CYCLE_LENGTH

    fun ovulationDay(cycleLength: Int): Int =
        (clampCycleLength(cycleLength) - LUTEAL_PHASE_DAYS).coerceAtLeast(10)

    /** Groups individual bleeding days into the first day of each period. */
    fun periodStarts(bleedingDays: Collection<LocalDate>): List<LocalDate> {
        val sorted = bleedingDays.distinct().sorted()
        if (sorted.isEmpty()) return emptyList()
        val starts = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            if (ChronoUnit.DAYS.between(sorted[i - 1], sorted[i]) > NEW_PERIOD_GAP_DAYS) {
                starts.add(sorted[i])
            }
        }
        return starts
    }

    /**
     * Groups individual bleeding days into periods (first and last day of each), oldest first.
     * The same grouping rule as [periodStarts], so history and forecasts always agree.
     */
    fun periodEpisodes(bleedingDays: Collection<LocalDate>): List<Pair<LocalDate, LocalDate>> {
        val sorted = bleedingDays.distinct().sorted()
        if (sorted.isEmpty()) return emptyList()
        val episodes = mutableListOf<Pair<LocalDate, LocalDate>>()
        var runStart = sorted.first()
        var prev = sorted.first()
        for (i in 1 until sorted.size) {
            val day = sorted[i]
            if (ChronoUnit.DAYS.between(prev, day) > NEW_PERIOD_GAP_DAYS) {
                episodes.add(runStart to prev)
                runStart = day
            }
            prev = day
        }
        episodes.add(runStart to prev)
        return episodes
    }

    /**
     * The next ovulation day that is today or later, using the same model as the calendar and the
     * reminders (ovulation = cycle day [ovulationDay]). Null when no period was logged yet.
     */
    fun nextOvulation(prediction: Prediction, today: LocalDate): LocalDate? {
        val lastStart = prediction.lastPeriodStart ?: return null
        val day = ovulationDay(prediction.averageCycleLength)
        return nextOccurrenceOfCycleDay(lastStart, prediction.averageCycleLength, day, today.minusDays(1))
    }

    /** Fertile window around an ovulation day: 5 days before up to 1 day after (same as [dayStatus]). */
    fun fertileWindow(ovulation: LocalDate): Pair<LocalDate, LocalDate> =
        ovulation.minusDays(5) to ovulation.plusDays(1)

    /** Average number of bleeding days per period, from real data (fallback 5). */
    fun averagePeriodLength(bleedingDays: Collection<LocalDate>): Int {
        val sorted = bleedingDays.distinct().sorted()
        if (sorted.isEmpty()) return DEFAULT_PERIOD_LENGTH
        val lengths = mutableListOf<Int>()
        var runStart = sorted.first()
        var prev = sorted.first()
        for (i in 1 until sorted.size) {
            val day = sorted[i]
            if (ChronoUnit.DAYS.between(prev, day) > NEW_PERIOD_GAP_DAYS) {
                lengths.add(ChronoUnit.DAYS.between(runStart, prev).toInt() + 1)
                runStart = day
            }
            prev = day
        }
        lengths.add(ChronoUnit.DAYS.between(runStart, prev).toInt() + 1)
        // Single logged days (only the start was entered) carry no length information.
        val informative = lengths.filter { it > 1 }
        if (informative.isEmpty()) return DEFAULT_PERIOD_LENGTH
        return informative.takeLast(MAX_CYCLES_FOR_AVERAGE).average().toInt().coerceIn(2, 10)
    }

    fun predict(
        bleedingDays: Collection<LocalDate>,
        defaultCycleLength: Int,
        today: LocalDate = LocalDate.now()
    ): Prediction {
        val fallbackLength = clampCycleLength(defaultCycleLength)
        // Future-dated entries are ignored: they cannot be real bleeding days.
        val pastDays = bleedingDays.filter { !it.isAfter(today) }
        val starts = periodStarts(pastDays)
        if (starts.isEmpty()) {
            return Prediction(
                hasEnoughData = false,
                averageCycleLength = fallbackLength,
                averagePeriodLength = DEFAULT_PERIOD_LENGTH,
                lastPeriodStart = null,
                predictedNextStart = today.plusDays(fallbackLength.toLong()),
                daysRemaining = fallbackLength,
                cyclesUsed = 0,
                cycleVariationDays = 0,
                isLate = false
            )
        }

        val intervals = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in MIN_PLAUSIBLE_INTERVAL..MAX_PLAUSIBLE_INTERVAL }
            .takeLast(MAX_CYCLES_FOR_AVERAGE)

        val hasEnoughData = intervals.isNotEmpty()
        val average = if (hasEnoughData) {
            Math.round(intervals.average()).toInt().coerceIn(MIN_CYCLE_LENGTH, MAX_CYCLE_LENGTH)
        } else {
            fallbackLength
        }
        val lastStart = starts.last()
        val next = lastStart.plusDays(average.toLong())
        val remaining = ChronoUnit.DAYS.between(today, next).toInt()

        return Prediction(
            hasEnoughData = hasEnoughData,
            averageCycleLength = average,
            averagePeriodLength = averagePeriodLength(pastDays),
            lastPeriodStart = lastStart,
            predictedNextStart = next,
            daysRemaining = remaining,
            cyclesUsed = intervals.size,
            cycleVariationDays = if (intervals.size >= 2) intervals.max() - intervals.min() else 0,
            isLate = remaining < 0
        )
    }

    /** 1-based day of the (projected) cycle a date falls on. */
    fun cycleDayFor(date: LocalDate, lastPeriodStart: LocalDate, cycleLength: Int): Int {
        val length = clampCycleLength(cycleLength)
        val diff = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt()
        return Math.floorMod(diff, length) + 1
    }

    fun phaseForCycleDay(cycleDay: Int, cycleLength: Int, periodLength: Int = DEFAULT_PERIOD_LENGTH): Phase {
        val ovulation = ovulationDay(cycleLength)
        return when {
            cycleDay <= periodLength -> Phase.MENSTRUAL
            cycleDay < ovulation - 1 -> Phase.FOLLICULAR
            cycleDay <= ovulation + 1 -> Phase.OVULATION
            else -> Phase.LUTEAL
        }
    }

    /**
     * Status of [date] relative to the prediction. [today] decides what is history and what is
     * forecast: an expected period that was not logged by [today] is reported as late, and the
     * forecast after a late [today] assumes the period can start from tomorrow.
     */
    fun dayStatus(
        date: LocalDate,
        prediction: Prediction,
        loggedBleedingToday: Boolean,
        today: LocalDate = date
    ): DayStatus {
        val length = prediction.averageCycleLength
        val lastStart = prediction.lastPeriodStart
        if (lastStart == null) {
            return DayStatus(
                cycleDay = 1,
                daysUntilNextPeriod = length,
                predictedPeriodStart = date.plusDays(length.toLong()),
                isPeriod = loggedBleedingToday,
                isFertile = false,
                isPms = false,
                phase = if (loggedBleedingToday) Phase.MENSTRUAL else Phase.FOLLICULAR
            )
        }
        val expectedStart = lastStart.plusDays(length.toLong())
        var anchor = lastStart
        // A due period that was not logged is shown as late instead of silently starting a new
        // projected cycle. After a very long gap the data is probably just missing, so fall back
        // to the plain projection.
        if (ChronoUnit.DAYS.between(expectedStart, today) in 0..MAX_LATE_DAYS.toLong()) {
            if (!date.isAfter(today) && !date.isBefore(expectedStart)) {
                if (!loggedBleedingToday) {
                    val overdue = ChronoUnit.DAYS.between(expectedStart, date).toInt()
                    return DayStatus(
                        cycleDay = length + overdue + 1,
                        daysUntilNextPeriod = 0,
                        predictedPeriodStart = date,
                        isPeriod = false,
                        isFertile = false,
                        isPms = false,
                        phase = Phase.LUTEAL,
                        isLate = overdue > 0,
                        daysLate = overdue
                    )
                }
            } else if (date.isAfter(today)) {
                // Forecast: the late period may start tomorrow, later cycles follow from there.
                anchor = today.plusDays(1).minusDays(length.toLong())
            }
        }
        val cycleDay = cycleDayFor(date, anchor, length)
        val daysUntil = length - cycleDay + 1
        val ovulation = ovulationDay(length)
        val predictedBleeding = cycleDay <= prediction.averagePeriodLength
        val isPeriod = loggedBleedingToday || predictedBleeding
        val isFertile = !isPeriod && cycleDay in (ovulation - 5)..(ovulation + 1)
        val isPms = !isPeriod && !isFertile && daysUntil in 1..5
        val phase = if (loggedBleedingToday) Phase.MENSTRUAL
        else phaseForCycleDay(cycleDay, length, prediction.averagePeriodLength)
        return DayStatus(
            cycleDay = cycleDay,
            daysUntilNextPeriod = daysUntil,
            predictedPeriodStart = date.plusDays(daysUntil.toLong()),
            isPeriod = isPeriod,
            isFertile = isFertile,
            isPms = isPms,
            phase = phase
        )
    }

    /** One completed cycle: the first day of its period and the days until the next period began. */
    data class CompletedCycle(val start: LocalDate, val lengthDays: Int) {
        /** Outside 21..45 days: usually a missed log or a skipped period, shown but not trusted. */
        val isTypical: Boolean get() = lengthDays in MIN_CYCLE_LENGTH..MAX_CYCLE_LENGTH
    }

    /**
     * The real lengths of the last [max] completed cycles, oldest first, measured from one logged
     * period start to the next. Used by the cycle-length chart (it used to draw invented values).
     */
    fun recentCycles(bleedingDays: Collection<LocalDate>, today: LocalDate = LocalDate.now(), max: Int = MAX_CYCLES_FOR_AVERAGE): List<CompletedCycle> {
        val starts = periodStarts(bleedingDays.filter { !it.isAfter(today) })
        return starts.zipWithNext { a, b -> CompletedCycle(a, ChronoUnit.DAYS.between(a, b).toInt()) }
            .takeLast(max.coerceAtLeast(1))
    }

    /** First occurrence of [cycleDay] (1-based) that is strictly after [today]. */
    fun nextOccurrenceOfCycleDay(lastPeriodStart: LocalDate, cycleLength: Int, cycleDay: Int, today: LocalDate): LocalDate {
        val length = clampCycleLength(cycleLength).toLong()
        val base = lastPeriodStart.plusDays((cycleDay - 1).toLong())
        if (base.isAfter(today)) return base
        val cyclesAhead = (ChronoUnit.DAYS.between(base, today) / length) + 1
        return base.plusDays(cyclesAhead * length)
    }
}
