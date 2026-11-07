package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.PillPack
import java.time.LocalDate

/** Settings of the health tools (pill pack and breast self-exam reminders). */
class WellnessPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var pillEnabled: Boolean
        get() = prefs.getBoolean(KEY_PILL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PILL_ENABLED, value).apply()

    var pillStart: LocalDate?
        get() = prefs.getString(KEY_PILL_START, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        set(value) = prefs.edit().putString(KEY_PILL_START, value?.toString()).apply()

    var pillType: PillPack.Type
        get() = PillPack.Type.fromCode(prefs.getString(KEY_PILL_TYPE, null))
        set(value) = prefs.edit().putString(KEY_PILL_TYPE, value.code).apply()

    var pillHour: Int
        get() = prefs.getInt(KEY_PILL_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_PILL_HOUR, value.coerceIn(0, 23)).apply()

    var pillMinute: Int
        get() = prefs.getInt(KEY_PILL_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_PILL_MINUTE, value.coerceIn(0, 59)).apply()

    var breastExamReminder: Boolean
        get() = prefs.getBoolean(KEY_BREAST_REMINDER, false)
        set(value) = prefs.edit().putBoolean(KEY_BREAST_REMINDER, value).apply()

    companion object {
        const val NAME = "hanita_wellness"
        private const val KEY_PILL_ENABLED = "pill_enabled"
        private const val KEY_PILL_START = "pill_start"
        private const val KEY_PILL_TYPE = "pill_type"
        private const val KEY_PILL_HOUR = "pill_hour"
        private const val KEY_PILL_MINUTE = "pill_minute"
        private const val KEY_BREAST_REMINDER = "breast_exam_reminder"
    }
}
