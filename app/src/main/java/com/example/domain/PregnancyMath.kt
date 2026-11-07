package com.example.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pregnancy dating from the first day of the last period (LMP), Naegele's rule adjusted for the
 * user's own cycle length: ovulation happens (cycle length - 14) days after the LMP, so a longer
 * cycle moves the due date later. For information only; an ultrasound dating is more accurate.
 */
object PregnancyMath {

    const val FULL_TERM_DAYS = 280
    /** After this many days the LMP is clearly not the start of a current pregnancy. */
    const val MAX_PLAUSIBLE_DAYS = 42 * 7

    enum class Status { NO_LMP, FUTURE_LMP, OK, TOO_OLD }

    data class Estimate(
        val status: Status,
        /** 1..40, the pregnancy week to show first (1 when unknown). */
        val week: Int,
        val dueDate: LocalDate?,
        val daysUntilDue: Long?
    )

    fun estimate(lmp: LocalDate?, cycleLength: Int, today: LocalDate): Estimate {
        if (lmp == null) return Estimate(Status.NO_LMP, 1, null, null)
        if (lmp.isAfter(today)) return Estimate(Status.FUTURE_LMP, 1, null, null)
        val shift = (CyclePredictor.clampCycleLength(cycleLength) - 28).toLong()
        val due = lmp.plusDays(FULL_TERM_DAYS + shift)
        val gestationalDays = ChronoUnit.DAYS.between(lmp, today) - shift
        if (ChronoUnit.DAYS.between(lmp, today) > MAX_PLAUSIBLE_DAYS) {
            return Estimate(Status.TOO_OLD, 1, null, null)
        }
        val week = (gestationalDays.coerceAtLeast(0) / 7 + 1).toInt().coerceIn(1, 40)
        return Estimate(Status.OK, week, due, ChronoUnit.DAYS.between(today, due).coerceAtLeast(0))
    }
}
