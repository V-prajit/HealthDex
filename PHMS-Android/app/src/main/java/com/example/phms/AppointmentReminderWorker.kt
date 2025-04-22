package com.example.phms

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class AppointmentReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("LAST_USER_UID", null) ?: return Result.failure()

        val appointments = withContext(Dispatchers.IO) {
            try {
                AppointmentRepository.getUpcomingAppointments(userId)
            } catch (e: Exception) {
                emptyList()
            }
        }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val notificationManager = AppointmentNotificationManager(context)

        appointments.forEach { appointment ->
            if (!appointment.reminders) return@forEach

            val appointmentDate = LocalDate.parse(appointment.date)

            when {
                appointmentDate == today -> {
                    notificationManager.showAppointmentReminder(appointment)
                }
                appointmentDate == tomorrow -> {
                    notificationManager.showAppointmentReminder(appointment)
                }
            }
        }

        scheduleNextCheck(context)

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
        }
    }
}