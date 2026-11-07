package com.example.utils

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.CyclePredictor
import java.time.LocalDate

/**
 * One place that (re)schedules every cycle-related reminder from the real logged data.
 * Used by the ViewModel (when data or settings change) and by [RescheduleWorker]
 * (after boot, time changes, a fired reminder and once a day).
 */
object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    data class Settings(
        val onboarded: Boolean,
        val periodEnabled: Boolean,
        val ovulationEnabled: Boolean,
        val daysBeforePeriod: Int,
        val daysBeforeOvulation: Int,
        val hour: Int,
        val minute: Int
    )

    fun settingsFrom(prefs: UserPreferencesManager) = Settings(
        onboarded = prefs.isLoggedIn.value,
        periodEnabled = prefs.isNotificationEnabled.value,
        ovulationEnabled = prefs.isOvulationRemindersEnabled.value,
        daysBeforePeriod = prefs.daysBefore.value,
        daysBeforeOvulation = prefs.ovulationDaysBefore.value,
        hour = prefs.notificationHour.value,
        minute = prefs.notificationMinute.value
    )

    fun schedule(context: Context, prediction: CyclePredictor.Prediction, settings: Settings) {
        val lastStart = prediction.lastPeriodStart
        if (!settings.onboarded || lastStart == null) {
            CycleNotificationHelper.cancelAll(context)
            PhaseReminderHelper.cancelAllPhaseReminders(context)
            return
        }
        val length = prediction.averageCycleLength
        CycleNotificationHelper.scheduleAllReminders(
            context = context,
            lastPeriodStart = lastStart,
            cycleLength = length,
            isPeriodEnabled = settings.periodEnabled,
            daysBeforePeriod = settings.daysBeforePeriod,
            isOvulationEnabled = settings.ovulationEnabled,
            daysBeforeOvulation = settings.daysBeforeOvulation,
            hour = settings.hour,
            minute = settings.minute
        )

        if (!settings.periodEnabled) {
            PhaseReminderHelper.cancelAllPhaseReminders(context)
            return
        }
        val ovulation = CyclePredictor.ovulationDay(length)
        val phases = listOf(
            Triple(prediction.averagePeriodLength + 1, "فاز فولیکولی", "انرژی در حال افزایش است. زمان خوبی برای برنامه‌های جدید و فعالیت بدنی است."),
            Triple(ovulation - 1, "فاز تخمک‌گذاری", "اوج انرژی و تمرکز! زمان خوبی برای خلاقیت و ارتباطات است."),
            Triple(ovulation + 2, "فاز لوتئال", "به بدن خود استراحت دهید و به خواب کافی و آرامش ذهنی توجه کنید."),
            Triple(1, "فاز قاعدگی", "زمان استراحت و بازتوانی است. مایعات گرم بنوشید و فعالیت سبک داشته باشید.")
        )
        val today = LocalDate.now()
        phases.forEachIndexed { index, (cycleDay, name, message) ->
            val date = CyclePredictor.nextOccurrenceOfCycleDay(lastStart, length, cycleDay, today)
            PhaseReminderHelper.schedulePhaseReminder(context, date, name, message, index, length, settings.hour, settings.minute)
        }
    }

    /** Reads everything from storage; safe to call from a background worker. */
    suspend fun rescheduleFromStorage(context: Context) {
        try {
            val prefs = UserPreferencesManager(context)
            val db = AppDatabase.getDatabase(context)
            val bleedingDays = db.dailyLogDao().getPeriodDaysList().mapNotNull {
                runCatching { LocalDate.parse(it.dateStr) }.getOrNull()
            }
            val prediction = CyclePredictor.predict(bleedingDays, prefs.averageCycleLength.value)
            schedule(context, prediction, settingsFrom(prefs))
            MedicineReminderHelper.scheduleReminders(context, prefs.medicationReminders.value)
            com.example.tools.WellnessReminders.scheduleAll(context, prediction.lastPeriodStart, prediction.averageCycleLength)
            com.example.tools.PartnerReminders.schedule(context, prediction.lastPeriodStart, prediction.averageCycleLength)
            com.example.tools.CycleWidgetProvider.requestUpdate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reminders", e)
        }
    }
}
