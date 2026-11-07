package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One health observation that is not part of the classic daily log: intimacy, cervical mucus,
 * ovulation/pregnancy tests, blood pressure, a taken contraceptive pill or a breast self-exam.
 *
 * These entries are intentionally kept on the device only (never uploaded to the cloud).
 */
@Entity(tableName = "health_entries")
data class HealthEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "YYYY-MM-DD" */
    val dateStr: String,
    /** One of [HealthEntryType] codes. */
    val type: String,
    /** Type specific code, e.g. "PROTECTED", "EGG_WHITE", "POSITIVE". */
    val value: String = "",
    /** Numeric payload (e.g. systolic pressure). */
    val num1: Int? = null,
    /** Second numeric payload (e.g. diastolic pressure / pulse). */
    val num2: Int? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object HealthEntryType {
    const val INTIMACY = "INTIMACY"
    const val MUCUS = "MUCUS"
    const val OVULATION_TEST = "OVULATION_TEST"
    const val PREGNANCY_TEST = "PREGNANCY_TEST"
    const val BLOOD_PRESSURE = "BLOOD_PRESSURE"
    const val PILL = "PILL"
    const val BREAST_EXAM = "BREAST_EXAM"
    /** Hours of sleep, stored in [HealthEntry.num1] as minutes. */
    const val SLEEP = "SLEEP"
    /** Steps walked that day, in [HealthEntry.num1]. */
    const val STEPS = "STEPS"

    /** Types that hold a single value per day (a new entry replaces the old one). */
    val ONE_PER_DAY = setOf(MUCUS, OVULATION_TEST, PREGNANCY_TEST, PILL, BREAST_EXAM, SLEEP, STEPS)
}
