package com.example.tools

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.PartnerAlerts
import com.example.utils.JalaliCalendar
import com.example.utils.RescheduleWorker
import com.example.utils.privateOnLockScreen
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Partner name/number and alert choices. Stored only on this phone (included in encrypted backups). */
class PartnerPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var partnerName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value.trim().take(40)).apply()

    var partnerPhone: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE, value.trim().take(20)).apply()

    var alertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var events: Set<PartnerAlerts.Event>
        get() {
            val stored = prefs.getStringSet(KEY_EVENTS, null)
                ?: return PartnerAlerts.Event.values().filter { it.defaultOn }.toSet()
            return stored.mapNotNull { PartnerAlerts.Event.fromCode(it) }.toSet()
        }
        set(value) = prefs.edit().putStringSet(KEY_EVENTS, value.map { it.code }.toSet()).apply()

    var tone: PartnerAlerts.Tone
        get() = PartnerAlerts.Tone.fromCode(prefs.getString(KEY_TONE, null))
        set(value) = prefs.edit().putString(KEY_TONE, value.code).apply()

    var daysBeforePeriod: Int
        get() = prefs.getInt(KEY_DAYS_BEFORE, 2).coerceIn(PartnerAlerts.MIN_DAYS_BEFORE, PartnerAlerts.MAX_DAYS_BEFORE)
        set(value) = prefs.edit().putInt(KEY_DAYS_BEFORE, value.coerceIn(PartnerAlerts.MIN_DAYS_BEFORE, PartnerAlerts.MAX_DAYS_BEFORE)).apply()

    val hasPartner: Boolean get() = PartnerAlerts.normalizePhone(partnerPhone) != null

    companion object {
        const val NAME = "partner_prefs"
        private const val KEY_NAME = "partner_name"
        private const val KEY_PHONE = "partner_phone"
        private const val KEY_ENABLED = "alerts_enabled"
        private const val KEY_EVENTS = "alert_events"
        private const val KEY_TONE = "message_tone"
        private const val KEY_DAYS_BEFORE = "days_before_period"
    }
}

/**
 * Schedules one alarm for the next sensitive day. When it fires the user gets a notification with
 * the prepared message and "send SMS" / "other messenger" buttons. The message is only sent after
 * the user presses send in their own SMS or messenger app (no SEND_SMS permission is used).
 */
object PartnerReminders {
    private const val TAG = "PartnerReminders"
    const val CHANNEL_ID = "partner_channel"
    const val ACTION_ALERT = "com.example.tools.ACTION_PARTNER_ALERT"
    const val EXTRA_EVENT = "partner_event"
    private const val REQ_ALERT = 7301
    private const val REQ_SMS = 7302
    private const val REQ_SHARE = 7303
    private const val REQ_OPEN = 7304
    const val NOTIF_ID = 7401

    fun schedule(context: Context, lastPeriodStart: LocalDate?, cycleLength: Int) {
        val prefs = PartnerPrefs(context)
        val user = UserPreferencesManager(context)
        if (!prefs.alertsEnabled || !prefs.hasPartner || !user.isLoggedIn.value) {
            cancel(context)
            return
        }
        val time = LocalTime.of(user.notificationHour.value.coerceIn(0, 23), user.notificationMinute.value.coerceIn(0, 59))
        val now = LocalDateTime.now()
        val next = PartnerAlerts.upcoming(lastPeriodStart, cycleLength, now.toLocalDate(), prefs.events, prefs.daysBeforePeriod)
            .firstOrNull { LocalDateTime.of(it.date, time).isAfter(now) }
        if (next == null) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = alarmIntent(context, next.event, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val millis = LocalDateTime.of(next.date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } catch (e: Exception) {
            Log.w(TAG, "Could not schedule partner alert", e)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = alarmIntent(context, null, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun alarmIntent(context: Context, event: PartnerAlerts.Event?, flags: Int): PendingIntent? {
        val intent = Intent(context, PartnerAlertReceiver::class.java).setAction(ACTION_ALERT)
        if (event != null) intent.putExtra(EXTRA_EVENT, event.code)
        return PendingIntent.getBroadcast(context, REQ_ALERT, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "اطلاع‌رسانی به همسر در روزهای حساس", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "پیام آماده برای ارسال به همسر در روزهای حساس چرخه (ارسال فقط با تأیید شما)"
                }
            )
        }
    }

    /** Opens the default SMS app with the number and message filled in. */
    fun smsIntent(phone: String, message: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone))).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** Lets the user pick WhatsApp, Telegram, Bale, Eitaa, ... */
    fun shareIntent(message: String): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "ارسال پیام به همسر"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun dialIntent(phone: String): Intent =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "برنامه‌ای برای این کار روی گوشی پیدا نشد.", Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not open $intent", e)
        }
    }

    internal fun smsPendingIntent(context: Context, phone: String, message: String): PendingIntent =
        PendingIntent.getActivity(context, REQ_SMS, smsIntent(phone, message), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    internal fun sharePendingIntent(context: Context, message: String): PendingIntent =
        PendingIntent.getActivity(context, REQ_SHARE, shareIntent(message), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    internal fun openPartnerTab(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, "partner")
        }
        return PendingIntent.getActivity(context, REQ_OPEN, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class PartnerAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PartnerReminders.ACTION_ALERT) return
        val event = PartnerAlerts.Event.fromCode(intent.getStringExtra(PartnerReminders.EXTRA_EVENT) ?: "")
        if (event != null) show(context, event)
        // Arm the next sensitive day.
        RescheduleWorker.runNow(context)
    }

    @android.annotation.SuppressLint("MissingPermission") // checked with areNotificationsEnabled()
    private fun show(context: Context, event: PartnerAlerts.Event) {
        val prefs = PartnerPrefs(context)
        val phone = PartnerAlerts.normalizePhone(prefs.partnerPhone) ?: return
        if (!prefs.alertsEnabled || event !in prefs.events) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        PartnerReminders.ensureChannel(context)
        val name = prefs.partnerName.ifBlank { "همسرتان" }
        val message = PartnerAlerts.message(event, prefs.partnerName, prefs.tone, prefs.daysBeforePeriod)
        val today = JalaliCalendar.gregorianToJalali(LocalDate.now()).formatShort()
        val title = "${event.emoji} روز حساس: ${event.title}"
        val body = "امروز ($today) پیام آماده برای $name:\n«$message»\nبا یک لمس بفرستید؛ بدون تأیید شما چیزی ارسال نمی‌شود."
        val notification = NotificationCompat.Builder(context, PartnerReminders.CHANNEL_ID)
            .privateOnLockScreen(context, PartnerReminders.CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_stat_hanita)
            .setContentTitle(title)
            .setContentText("پیام آماده برای $name")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(PartnerReminders.openPartnerTab(context))
            .addAction(0, "پیامک به $name", PartnerReminders.smsPendingIntent(context, phone, message))
            .addAction(0, "سایر پیام‌رسان‌ها", PartnerReminders.sharePendingIntent(context, message))
            .build()
        NotificationManagerCompat.from(context).notify(PartnerReminders.NOTIF_ID, notification)
    }
}
