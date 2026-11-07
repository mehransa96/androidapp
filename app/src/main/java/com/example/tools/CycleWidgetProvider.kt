package com.example.tools

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.CyclePredictor
import com.example.security.AppLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Home-screen widget: days until the next period and today's phase.
 * When the app lock is on, the widget shows only a neutral text (no health data on the home screen).
 */
class CycleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildViews(context)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
                scheduleMidnightRefresh(context, appWidgetIds)
            } catch (e: Exception) {
                Log.w(TAG, "Widget update failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    /** Last widget removed: stop the midnight refresh. */
    override fun onDisabled(context: Context) {
        try {
            val pending = PendingIntent.getBroadcast(
                context, 7301,
                Intent(context, CycleWidgetProvider::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                (context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager)?.cancel(pending)
                pending.cancel()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not cancel the widget refresh", e)
        }
    }

    companion object {
        private const val TAG = "CycleWidget"

        /** Refreshes every placed widget (cheap no-op when none is placed). */
        fun requestUpdate(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, CycleWidgetProvider::class.java))
                if (ids.isEmpty()) return
                val intent = Intent(context, CycleWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Widget refresh failed", e)
            }
        }

        /**
         * The system refreshes widgets only every few hours, so after midnight the widget kept
         * showing yesterday's cycle day. One (non-waking) alarm just after midnight refreshes it.
         */
        private fun scheduleMidnightRefresh(context: Context, ids: IntArray) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
                val intent = Intent(context, CycleWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                val pending = PendingIntent.getBroadcast(
                    context, 7301, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val nextMidnight = LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())
                    .plusMinutes(1).toInstant().toEpochMilli()
                alarmManager.set(android.app.AlarmManager.RTC, nextMidnight, pending)
            } catch (e: Exception) {
                Log.w(TAG, "Midnight widget refresh not scheduled", e)
            }
        }

        private suspend fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_cycle)
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            val prefs = UserPreferencesManager(context)
            if (AppLockManager(context).isEnabled) {
                views.setTextViewText(R.id.widget_main, "🌸")
                views.setTextViewText(R.id.widget_sub, "برای مشاهده، برنامه را باز کنید")
                return views
            }
            if (!prefs.isLoggedIn.value) {
                views.setTextViewText(R.id.widget_main, "خوش آمدید")
                views.setTextViewText(R.id.widget_sub, "برای شروع، برنامه را باز کنید")
                return views
            }
            val today = LocalDate.now()
            val bleeding = AppDatabase.getDatabase(context).dailyLogDao().getPeriodDaysList()
                .mapNotNull { runCatching { LocalDate.parse(it.dateStr) }.getOrNull() }
            val prediction = CyclePredictor.predict(bleeding, prefs.averageCycleLength.value, today)
            if (prediction.lastPeriodStart == null) {
                views.setTextViewText(R.id.widget_main, "ثبت پریود")
                views.setTextViewText(R.id.widget_sub, "برای پیش‌بینی، شروع پریود را ثبت کنید")
                return views
            }
            val loggedToday = bleeding.contains(today)
            val status = CyclePredictor.dayStatus(today, prediction, loggedToday, today)
            val (main, sub) = widgetText(status, loggedToday)
            views.setTextViewText(R.id.widget_main, main)
            views.setTextViewText(R.id.widget_sub, sub)
            return views
        }

        internal fun widgetText(status: CyclePredictor.DayStatus, loggedToday: Boolean): Pair<String, String> {
            val phase = when (status.phase) {
                CyclePredictor.Phase.MENSTRUAL -> "فاز قاعدگی"
                CyclePredictor.Phase.FOLLICULAR -> "فاز فولیکولی"
                CyclePredictor.Phase.OVULATION -> "فاز تخمک‌گذاری"
                CyclePredictor.Phase.LUTEAL -> "فاز لوتئال"
            }
            return when {
                status.isLate -> "${status.daysLate} روز تأخیر" to "پریود ثبت نشده است"
                status.isPeriod && loggedToday -> "روز ${status.cycleDay} پریود" to phase
                status.isPeriod -> "روزهای احتمالی پریود" to "روز ${status.cycleDay} چرخه (پیش‌بینی)"
                status.daysUntilNextPeriod <= 0 -> "پریود امروز" to "طبق پیش‌بینی"
                status.daysUntilNextPeriod == 1 -> "پریود از فردا" to "روز ${status.cycleDay} چرخه"
                else -> "${status.daysUntilNextPeriod} روز تا پریود" to "روز ${status.cycleDay} چرخه · $phase"
            }
        }
    }
}
