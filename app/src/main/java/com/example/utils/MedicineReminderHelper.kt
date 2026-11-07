package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.model.MedicineReminder
import java.util.Calendar

object MedicineReminderHelper {
    private const val STORE = "hanita_medicine_alarms"
    private const val KEY_IDS = "scheduled_ids"

    fun scheduleReminders(context: Context, reminders: List<MedicineReminder>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel alarms of reminders that were deleted since the last run, otherwise a removed
        // medicine would keep notifying every day forever.
        val store = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val currentIds = reminders.map { it.id }.toSet()
        val previousIds = store.getStringSet(KEY_IDS, emptySet()).orEmpty()
        for (removedId in previousIds - currentIds) {
            val stale = PendingIntent.getBroadcast(
                context,
                removedId.hashCode(),
                Intent(context, MedicineReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (stale != null) {
                alarmManager.cancel(stale)
                stale.cancel()
            }
        }
        store.edit().putStringSet(KEY_IDS, currentIds).apply()

        for (reminder in reminders) {
            val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
                putExtra("MED_NAME", reminder.name)
                putExtra("MED_DOSAGE", reminder.dosage)
                putExtra("MED_ID", reminder.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!reminder.isEnabled) {
                alarmManager.cancel(pendingIntent)
                continue
            }

            try {
                // Same reading as the add/edit form, so "۱۴:۳۰" or "14.30" rings at the right time.
                val time = com.example.domain.TimeText.parse(reminder.time)
                if (time == null) {
                    Log.w("MedicineReminderHelper", "Unreadable time for a medicine reminder; not scheduled")
                    alarmManager.cancel(pendingIntent)
                } else {
                    val (hour, minute) = time

                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // If time is already past for today, schedule for tomorrow
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    // Inexact repeating alarms can be delayed by hours under Doze; a one-shot alarm
                    // that is re-armed when it fires (MedicineReminderReceiver) stays on time.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("MedicineReminderHelper", "Scheduled ${reminder.name} at ${reminder.time}")
                }
            } catch (e: Exception) {
                Log.e("MedicineReminderHelper", "Error parsing time or scheduling: ${e.message}")
            }
        }
    }
}
