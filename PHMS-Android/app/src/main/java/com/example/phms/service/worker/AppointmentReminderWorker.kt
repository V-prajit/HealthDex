package com.example.phms.service.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class AppointmentReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("LAST_USER_UID", null) ?: return Result.failure()

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "appointment_reminder_checker"

        fun scheduleNextCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val checkRequest = PeriodicWorkRequestBuilder<AppointmentReminderWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                checkRequest
            )
        }

        fun initialize(context: Context) {
            scheduleNextCheck(context)

            val oneTimeRequest = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
            Log.d("AppointmentReminderWorker", "Initialization called.")
        }
    }
}