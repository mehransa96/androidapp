package com.example.tools

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.model.HealthEntry
import com.example.data.model.HealthEntryType
import com.example.data.preferences.WellnessPrefs
import com.example.domain.BreastSelfExam
import com.example.domain.PillPack
import com.example.utils.JalaliCalendar
import com.example.utils.RescheduleWorker
import com.example.utils.privateOnLockScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Daily contraceptive-pill reminder and monthly breast self-exam reminder.
 * Alarms are one-shot ([AlarmManager.setAndAllowWhileIdle]) and re-armed every time they fire,
 * after boot (through [com.example.utils.ReminderScheduler]) and whenever settings change.
 */
object WellnessReminders {
    private const val TAG = "WellnessReminders"
    const val CHANNEL_ID = "wellness_channel"
    private const val REQ_PILL = 7101
    private const val REQ_BREAST = 7102
    private const val REQ_PILL_TAKEN = 7103
    const val NOTIF_PILL = 7201
    const val NOTIF_BREAST = 7202

    /**
     * [lastPeriodStart] and [cycleLength] come from the real logged data (see
     * [com.example.utils.ReminderScheduler.rescheduleFromStorage]).
     */
    fun scheduleAll(context: Context, lastPeriodStart: LocalDate?, cycleLength: Int) {
        val prefs = WellnessPrefs(context)
        schedulePill(context, prefs)
        scheduleBreastExam(context, prefs, lastPeriodStart, cycleLength)
    }

    private fun schedulePill(context: Context, prefs: WellnessPrefs) {
        val start = prefs.pillStart
        if (!prefs.pillEnabled || start == null) {
            cancel(context, ACTION_PILL, REQ_PILL)
            return
        }
        val now = LocalDateTime.now()
        var date = if (start.isAfter(now.toLocalDate())) start else now.toLocalDate()
        val time = LocalTime.of(prefs.pillHour, prefs.pillMinute)
        if (!LocalDateTime.of(date, time).isAfter(now)) date = date.plusDays(1)
        set(context, ACTION_PILL, REQ_PILL, LocalDateTime.of(date, time))
    }

    private fun scheduleBreastExam(context: Context, prefs: WellnessPrefs, lastPeriodStart: LocalDate?, cycleLength: Int) {
        if (!prefs.breastExamReminder) {
            cancel(context, ACTION_BREAST, REQ_BREAST)
            return
        }
        try {
            val date = BreastSelfExam.nextDate(lastPeriodStart, cycleLength, LocalDate.now())
            set(context, ACTION_BREAST, REQ_BREAST, LocalDateTime.of(date, LocalTime.of(10, 0)))
        } catch (e: Exception) {
            Log.w(TAG, "Breast exam reminder not scheduled", e)
        }
    }

    private fun pendingIntent(context: Context, action: String, requestCode: Int, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, WellnessReminderReceiver::class.java).setAction(action),
            flags or PendingIntent.FLAG_IMMUTABLE
        )

    private fun set(context: Context, action: String, requestCode: Int, at: LocalDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context, action, requestCode, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule $action", e)
        }
    }

    private fun cancel(context: Context, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context, action, requestCode, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآورهای سلامت (قرص و خودآزمایی)", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    internal fun openToolsIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, "tools")
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    internal fun pillTakenIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQ_PILL_TAKEN,
            Intent(context, WellnessReminderReceiver::class.java).setAction(ACTION_PILL_TAKEN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    const val ACTION_PILL = "com.example.tools.ACTION_PILL_REMINDER"
    const val ACTION_BREAST = "com.example.tools.ACTION_BREAST_EXAM_REMINDER"
    const val ACTION_PILL_TAKEN = "com.example.tools.ACTION_PILL_TAKEN"
}

class WellnessReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WellnessReminders.ACTION_PILL -> {
                showPill(context)
                RescheduleWorker.runNow(context)
            }
            WellnessReminders.ACTION_BREAST -> {
                showBreast(context)
                RescheduleWorker.runNow(context)
            }
            WellnessReminders.ACTION_PILL_TAKEN -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val today = LocalDate.now().toString()
                        val dao = AppDatabase.getDatabase(context).healthEntryDao()
                        dao.deleteForDay(today, HealthEntryType.PILL)
                        dao.insert(HealthEntry(dateStr = today, type = HealthEntryType.PILL, value = "TAKEN"))
                        NotificationManagerCompat.from(context).cancel(WellnessReminders.NOTIF_PILL)
                    } catch (e: Exception) {
                        Log.w("WellnessReminderReceiver", "Could not mark pill as taken", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun canNotify(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    @android.annotation.SuppressLint("MissingPermission") // checked by canNotify()
    private fun showPill(context: Context) {
        val prefs = WellnessPrefs(context)
        val start = prefs.pillStart ?: return
        val day = PillPack.dayFor(start, prefs.pillType, LocalDate.now()) ?: return
        if (!canNotify(context)) return
        WellnessReminders.ensureChannel(context)
        val body = if (day.isActivePill) {
            "روز ${day.dayInPack} از بسته: زمان مصرف قرص امروز است."
        } else {
            "روز ${day.dayInPack} از بسته (هفته استراحت/قرص بی‌اثر). بسته بعدی ${day.daysUntilNextPack} روز دیگر شروع می‌شود."
        }
        val builder = NotificationCompat.Builder(context, WellnessReminders.CHANNEL_ID)
            .privateOnLockScreen(context, WellnessReminders.CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_stat_hanita)
            .setContentTitle("یادآور قرص روزانه 💊")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(WellnessReminders.openToolsIntent(context, WellnessReminders.NOTIF_PILL))
        if (day.isActivePill) {
            builder.addAction(0, "مصرف کردم ✓", WellnessReminders.pillTakenIntent(context))
        }
        NotificationManagerCompat.from(context).notify(WellnessReminders.NOTIF_PILL, builder.build())
    }

    @android.annotation.SuppressLint("MissingPermission") // checked by canNotify()
    private fun showBreast(context: Context) {
        if (!canNotify(context)) return
        WellnessReminders.ensureChannel(context)
        val date = JalaliCalendar.gregorianToJalali(LocalDate.now()).formatShort()
        val body = "امروز ($date) زمان مناسبی برای خودآزمایی ماهانه پستان است. راهنمای گام‌به‌گام در هانیتا آماده است."
        val builder = NotificationCompat.Builder(context, WellnessReminders.CHANNEL_ID)
            .privateOnLockScreen(context, WellnessReminders.CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_stat_hanita)
            .setContentTitle("یادآور خودآزمایی ماهانه 🎀")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(WellnessReminders.openToolsIntent(context, WellnessReminders.NOTIF_BREAST))
        NotificationManagerCompat.from(context).notify(WellnessReminders.NOTIF_BREAST, builder.build())
    }
}
