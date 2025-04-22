package com.example.phms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AppointmentAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    fun scheduleAppointmentReminders(appointment: Appointment) {
        if (!appointment.reminders) return

        cancelAppointmentReminders(appointment)

        val appointmentId = appointment.id ?: return

        try {
            val dateStr = appointment.date
            val timeStr = appointment.time

            val dateParts = dateStr.split("-")
            if (dateParts.size != 3) return

            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) return

            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            val appointmentDateTime = LocalDateTime.of(
                year, month, day, hour, minute
            )

            val oneDayBefore = appointmentDateTime.minus(1, ChronoUnit.DAYS)
            val oneHourBefore = appointmentDateTime.minus(1, ChronoUnit.HOURS)

            val currentDateTime = LocalDateTime.now()

            if (oneDayBefore.isAfter(currentDateTime)) {
                scheduleNotification(appointment, oneDayBefore, NotificationType.ONE_DAY_BEFORE)
                Log.d("AppointmentAlarm", "Scheduled 1-day reminder for appointment $appointmentId")
            }

            if (oneHourBefore.isAfter(currentDateTime)) {
                scheduleNotification(appointment, oneHourBefore, NotificationType.ONE_HOUR_BEFORE)
                Log.d("AppointmentAlarm", "Scheduled 1-hour reminder for appointment $appointmentId")
            }

        } catch (e: Exception) {
            Log.e("AppointmentAlarm", "Error scheduling reminders for appointment $appointmentId", e)
        }
    }

    fun cancelAppointmentReminders(appointment: Appointment) {
        val appointmentId = appointment.id ?: return

        val oneDayIntent = createReminderIntent(appointment, NotificationType.ONE_DAY_BEFORE)
        val oneDayPendingIntent = PendingIntent.getBroadcast(
            context,
            getDayBeforeRequestCode(appointmentId),
            oneDayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(oneDayPendingIntent)

        val oneHourIntent = createReminderIntent(appointment, NotificationType.ONE_HOUR_BEFORE)
        val oneHourPendingIntent = PendingIntent.getBroadcast(
            context,
            getHourBeforeRequestCode(appointmentId),
            oneHourIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(oneHourPendingIntent)

        Log.d("AppointmentAlarm", "Cancelled reminders for appointment $appointmentId")
    }

    suspend fun scheduleAllAppointmentReminders(userId: String) {
        try {
            val appointments = AppointmentRepository.getUpcomingAppointments(userId)

            for (appointment in appointments) {
                if (appointment.reminders) {
                    scheduleAppointmentReminders(appointment)
                }
            }

            Log.d("AppointmentAlarm", "Scheduled reminders for ${appointments.size} appointments")
        } catch (e: Exception) {
            Log.e("AppointmentAlarm", "Error scheduling all reminders", e)
        }
    }

    private fun scheduleNotification(
        appointment: Appointment,
        notificationTime: LocalDateTime,
        notificationType: NotificationType
    ) {
        val appointmentId = appointment.id ?: return

        val notificationTimeMillis = notificationTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = createReminderIntent(appointment, notificationType)

        val requestCode = when (notificationType) {
            NotificationType.ONE_DAY_BEFORE -> getDayBeforeRequestCode(appointmentId)
            NotificationType.ONE_HOUR_BEFORE -> getHourBeforeRequestCode(appointmentId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTimeMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTimeMillis,
                pendingIntent
            )
        }
    }

    private fun createReminderIntent(
        appointment: Appointment,
        notificationType: NotificationType
    ): Intent {
        return Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra(EXTRA_APPOINTMENT_ID, appointment.id)
            putExtra(EXTRA_DOCTOR_NAME, appointment.doctorName)
            putExtra(EXTRA_DATE, appointment.date)
            putExtra(EXTRA_TIME, appointment.time)
            putExtra(EXTRA_REASON, appointment.reason)
            putExtra(EXTRA_USER_ID, appointment.userId)
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationType.name)
        }
    }

    private fun getDayBeforeRequestCode(appointmentId: Int): Int {
        return appointmentId * 10 + 1
    }

    private fun getHourBeforeRequestCode(appointmentId: Int): Int {
        return appointmentId * 10 + 2
    }

    fun cancelAllReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag("com.example.phms.AppointmentReminderWorker")

        Log.d("AppointmentAlarm", "Cancelled background worker")
    }

    companion object {
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
        const val EXTRA_REASON = "reason"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }

    enum class NotificationType {
        ONE_DAY_BEFORE,
        ONE_HOUR_BEFORE
    }
}