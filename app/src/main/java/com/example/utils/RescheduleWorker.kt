package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Restores all reminders from the local database (after boot, time change, a fired alarm, daily)
 * and, once a day, writes the encrypted automatic safety copy of the user's data.
 */
class RescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ReminderScheduler.rescheduleFromStorage(applicationContext)
        com.example.tools.LocalSnapshots.takeIfDue(applicationContext)
        return Result.success()
    }

    companion object {
        private const val ONE_TIME = "hanita_reschedule_now"
        private const val PERIODIC = "hanita_reschedule_daily"

        fun runNow(context: Context) {
            try {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<RescheduleWorker>().build()
                )
            } catch (e: Exception) {
                Log.w("RescheduleWorker", "WorkManager unavailable", e)
            }
        }

        fun schedulePeriodic(context: Context) {
            try {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<RescheduleWorker>(1, TimeUnit.DAYS).build()
                )
            } catch (e: Exception) {
                Log.w("RescheduleWorker", "WorkManager unavailable", e)
            }
        }
    }
}
