package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MedicineReminder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The local account (profile created at first launch) and every user setting.
 *
 * One shared instance per process: every screen, ViewModel, widget and worker sees the same values,
 * so a change made in one place (for example the cycle length) is visible everywhere at once.
 * Values live in the app's private storage on the phone and are never sent to a server.
 */
class UserPreferencesManager private constructor(context: Context, @Suppress("UNUSED_PARAMETER") shared: Boolean) {
    private val prefs: SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    companion object {
        const val NAME = "user_prefs"
        const val MAX_NAME_LENGTH = 30

        @Volatile
        private var instance: UserPreferencesManager? = null

        operator fun invoke(context: Context): UserPreferencesManager =
            instance ?: synchronized(this) {
                instance ?: UserPreferencesManager(context.applicationContext, true).also { instance = it }
            }

        /** Cleans a display name typed at sign-up: trimmed, single spaces, limited length. */
        fun sanitizeName(raw: String): String =
            raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH)
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val remindersType = Types.newParameterizedType(List::class.java, MedicineReminder::class.java)
    private val remindersAdapter = moshi.adapter<List<MedicineReminder>>(remindersType)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userWeight = MutableStateFlow(prefs.getString("user_weight", "65") ?: "65")
    val userWeight: StateFlow<String> = _userWeight.asStateFlow()

    private val _lastPeriodDate = MutableStateFlow(prefs.getString("last_period_date", "") ?: "")
    val lastPeriodDate: StateFlow<String> = _lastPeriodDate.asStateFlow()

    private val _userAvatar = MutableStateFlow(prefs.getString("user_avatar", "avatar_default") ?: "avatar_default")
    val userAvatar: StateFlow<String> = _userAvatar.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(prefs.getBoolean("is_notification_enabled", true))
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _isAiRemindersEnabled = MutableStateFlow(prefs.getBoolean("is_ai_reminders_enabled", true))
    val isAiRemindersEnabled: StateFlow<Boolean> = _isAiRemindersEnabled.asStateFlow()

    private val _isOvulationRemindersEnabled = MutableStateFlow(prefs.getBoolean("is_ovulation_reminders_enabled", true))
    val isOvulationRemindersEnabled: StateFlow<Boolean> = _isOvulationRemindersEnabled.asStateFlow()

    private val _daysBefore = MutableStateFlow(prefs.getInt("days_before_notification", 1))
    val daysBefore: StateFlow<Int> = _daysBefore.asStateFlow()

    private val _ovulationDaysBefore = MutableStateFlow(prefs.getInt("ovulation_days_before_notification", 1))
    val ovulationDaysBefore: StateFlow<Int> = _ovulationDaysBefore.asStateFlow()

    private val _notificationHour = MutableStateFlow(prefs.getInt("notification_hour", 9))
    val notificationHour: StateFlow<Int> = _notificationHour.asStateFlow()

    private val _notificationMinute = MutableStateFlow(prefs.getInt("notification_minute", 0))
    val notificationMinute: StateFlow<Int> = _notificationMinute.asStateFlow()

    private val _averageCycleLength = MutableStateFlow(prefs.getInt("average_cycle_length", 28))
    val averageCycleLength: StateFlow<Int> = _averageCycleLength.asStateFlow()

    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "COSMIC_BLOSSOM") ?: "COSMIC_BLOSSOM")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _appFont = MutableStateFlow(prefs.getString("app_font", "CLASSIC") ?: "CLASSIC")
    val appFont: StateFlow<String> = _appFont.asStateFlow()

    private val _medicationReminders = MutableStateFlow(getMedicationRemindersFromPrefs())
    val medicationReminders: StateFlow<List<MedicineReminder>> = _medicationReminders.asStateFlow()

    private val _aiConsentGiven = MutableStateFlow(prefs.getBoolean("ai_data_consent", false))
    /** Whether the user agreed to send cycle data to the AI service (asked once, revocable). */
    val aiConsentGiven: StateFlow<Boolean> = _aiConsentGiven.asStateFlow()

    fun setAiConsentGiven(given: Boolean) {
        prefs.edit().putBoolean("ai_data_consent", given).apply()
        _aiConsentGiven.value = given
    }

    private val _customPhysicalSymptoms = MutableStateFlow(
        prefs.getStringSet("custom_physical_symptoms", emptySet())?.toList() ?: emptyList()
    )
    val customPhysicalSymptoms: StateFlow<List<String>> = _customPhysicalSymptoms.asStateFlow()

    private val _customMoodSymptoms = MutableStateFlow(
        prefs.getStringSet("custom_mood_symptoms", emptySet())?.toList() ?: emptyList()
    )
    val customMoodSymptoms: StateFlow<List<String>> = _customMoodSymptoms.asStateFlow()


    /** When the local account was created (epoch millis), 0 for accounts made before this field existed. */
    val registeredAt: Long get() = prefs.getLong("registered_at", 0L)

    /**
     * Creates the local account on this phone (first launch). No server or Google account is involved.
     * Written synchronously so the account survives even if the app is killed right after sign-up.
     */
    @android.annotation.SuppressLint("ApplySharedPref")
    fun register(name: String, cycleLength: Int = 28) {
        val clean = sanitizeName(name)
        val length = cycleLength.coerceIn(21, 45)
        prefs.edit()
            .putString("user_name", clean)
            .putBoolean("is_logged_in", true)
            .putInt("average_cycle_length", length)
            .putLong("registered_at", System.currentTimeMillis())
            .commit()
        _userName.value = clean
        _isLoggedIn.value = true
        _averageCycleLength.value = length
    }

    fun setUserName(name: String) {
        val clean = sanitizeName(name)
        if (clean.isBlank()) return
        prefs.edit().putString("user_name", clean).apply()
        _userName.value = clean
    }

    /**
     * Re-reads every value from disk. Needed after a backup restore or a full wipe, which write the
     * preference file directly while this shared instance stays alive in the same process.
     */
    fun reload() {
        _userName.value = prefs.getString("user_name", "") ?: ""
        _userWeight.value = prefs.getString("user_weight", "65") ?: "65"
        _lastPeriodDate.value = prefs.getString("last_period_date", "") ?: ""
        _userAvatar.value = prefs.getString("user_avatar", "avatar_default") ?: "avatar_default"
        _isLoggedIn.value = prefs.getBoolean("is_logged_in", false)
        _isNotificationEnabled.value = prefs.getBoolean("is_notification_enabled", true)
        _isAiRemindersEnabled.value = prefs.getBoolean("is_ai_reminders_enabled", true)
        _isOvulationRemindersEnabled.value = prefs.getBoolean("is_ovulation_reminders_enabled", true)
        _daysBefore.value = prefs.getInt("days_before_notification", 1)
        _ovulationDaysBefore.value = prefs.getInt("ovulation_days_before_notification", 1)
        _notificationHour.value = prefs.getInt("notification_hour", 9)
        _notificationMinute.value = prefs.getInt("notification_minute", 0)
        _averageCycleLength.value = prefs.getInt("average_cycle_length", 28)
        _appTheme.value = prefs.getString("app_theme", "COSMIC_BLOSSOM") ?: "COSMIC_BLOSSOM"
        _themeMode.value = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        _appFont.value = prefs.getString("app_font", "CLASSIC") ?: "CLASSIC"
        _medicationReminders.value = getMedicationRemindersFromPrefs()
        _aiConsentGiven.value = prefs.getBoolean("ai_data_consent", false)
        _customPhysicalSymptoms.value = prefs.getStringSet("custom_physical_symptoms", emptySet())?.toList() ?: emptyList()
        _customMoodSymptoms.value = prefs.getStringSet("custom_mood_symptoms", emptySet())?.toList() ?: emptyList()
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_notification_enabled", enabled).apply()
        _isNotificationEnabled.value = enabled
    }

    fun setAiRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_ai_reminders_enabled", enabled).apply()
        _isAiRemindersEnabled.value = enabled
    }

    fun setOvulationRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_ovulation_reminders_enabled", enabled).apply()
        _isOvulationRemindersEnabled.value = enabled
    }

    fun setDaysBefore(days: Int) {
        prefs.edit().putInt("days_before_notification", days).apply()
        _daysBefore.value = days
    }

    fun setOvulationDaysBefore(days: Int) {
        prefs.edit().putInt("ovulation_days_before_notification", days).apply()
        _ovulationDaysBefore.value = days
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit().apply {
            putInt("notification_hour", hour)
            putInt("notification_minute", minute)
            apply()
        }
        _notificationHour.value = hour
        _notificationMinute.value = minute
    }

    fun setAverageCycleLength(length: Int) {
        prefs.edit().putInt("average_cycle_length", length).apply()
        _averageCycleLength.value = length
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _appTheme.value = theme
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setAppFont(font: String) {
        prefs.edit().putString("app_font", font).apply()
        _appFont.value = font
    }

    fun setUserAvatar(avatar: String) {
        prefs.edit().putString("user_avatar", avatar).apply()
        _userAvatar.value = avatar
    }

    fun setUserWeight(weight: String) {
        prefs.edit().putString("user_weight", weight).apply()
        _userWeight.value = weight
    }

    fun setLastPeriodDate(dateStr: String) {
        prefs.edit().putString("last_period_date", dateStr).apply()
        _lastPeriodDate.value = dateStr
    }

    private fun getMedicationRemindersFromPrefs(): List<MedicineReminder> {
        val json = prefs.getString("medication_reminders_json", null)
        if (json.isNullOrBlank()) {
            // Templates only: all disabled until the user turns one on.
            return listOf(
                MedicineReminder("ref1", "قرص آهن (سولفات آهن)", "21:00", "طبق دستور پزشک", false, "pill"),
                MedicineReminder("ref2", "مسکن (طبق تجویز پزشک)", "08:00", "طبق دستور پزشک", false, "capsule"),
                MedicineReminder("ref3", "منیزیم و ویتامین ب۶", "14:30", "طبق دستور پزشک", false, "powder")
            )
        }
        return try {
            remindersAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMedicationReminders(list: List<MedicineReminder>) {
        try {
            val json = remindersAdapter.toJson(list)
            prefs.edit().putString("medication_reminders_json", json).apply()
            _medicationReminders.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCycleAnalysisReport(): String {
        return prefs.getString("cycle_analysis_report", "") ?: ""
    }

    fun saveCycleAnalysisReport(report: String) {
        prefs.edit().putString("cycle_analysis_report", report).apply()
    }


    fun addCustomPhysicalSymptom(symptom: String) {
        val currentSet = prefs.getStringSet("custom_physical_symptoms", emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet().apply { add(symptom) }
        prefs.edit().putStringSet("custom_physical_symptoms", newSet).apply()
        _customPhysicalSymptoms.value = newSet.toList()
    }

    fun removeCustomPhysicalSymptom(symptom: String) {
        val currentSet = prefs.getStringSet("custom_physical_symptoms", emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet().apply { remove(symptom) }
        prefs.edit().putStringSet("custom_physical_symptoms", newSet).apply()
        _customPhysicalSymptoms.value = newSet.toList()
    }

    fun addCustomMoodSymptom(symptom: String) {
        val currentSet = prefs.getStringSet("custom_mood_symptoms", emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet().apply { add(symptom) }
        prefs.edit().putStringSet("custom_mood_symptoms", newSet).apply()
        _customMoodSymptoms.value = newSet.toList()
    }

    fun removeCustomMoodSymptom(symptom: String) {
        val currentSet = prefs.getStringSet("custom_mood_symptoms", emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet().apply { remove(symptom) }
        prefs.edit().putStringSet("custom_mood_symptoms", newSet).apply()
        _customMoodSymptoms.value = newSet.toList()
    }

}
