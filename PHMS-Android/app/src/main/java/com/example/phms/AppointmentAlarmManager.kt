package com.example.phms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.provider.Settings
import com.example.phms.repository.AppointmentRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AppointmentAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "AppointmentAlarmManager"

    /**
     * Checks if the app has permission to schedule exact alarms
     */
    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Request permission for exact alarms on Android 12+
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting exact alarm permission", e)
            }
        }
    }

    /**
     * Schedule reminders for a specific appointment
     */
    fun scheduleAppointmentReminders(appointment: Appointment) {
        Log.d(TAG, "Scheduling reminders for appointment: ${appointment.id}, reminders enabled: ${appointment.reminders}")

        if (!appointment.reminders) {
            Log.d(TAG, "Reminders are disabled for appointment ${appointment.id}, cancelling any existing reminders.")
            cancelAppointmentReminders(appointment)
            return
        }

        val appointmentId = appointment.id ?: return

        // First, cancel any existing reminders for this appointment
        cancelAppointmentReminders(appointment)

        try {
            // Parse the appointment date and time
            val dateStr = appointment.date
            val timeStr = appointment.time

            Log.d(TAG, "Parsing date: '$dateStr', time: '$timeStr' for appointment $appointmentId")

            val dateParts = dateStr.split("-")
            if (dateParts.size != 3) {
                Log.e(TAG, "Invalid date format: '$dateStr' for appointment $appointmentId")
                return
            }

            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) {
                Log.e(TAG, "Invalid time format: '$timeStr' for appointment $appointmentId")
                return
            }

            val year = dateParts[0].toIntOrNull()
            val month = dateParts[1].toIntOrNull()
            val day = dateParts[2].toIntOrNull()
            val hour = timeParts[0].toIntOrNull()
            val minute = timeParts[1].toIntOrNull()

            if (year == null || month == null || day == null || hour == null || minute == null) {
                Log.e(TAG, "Failed to parse date/time components for appointment $appointmentId")
                return
            }

            val appointmentDateTime = LocalDateTime.of(year, month, day, hour, minute)
            val oneDayBefore = appointmentDateTime.minus(1, ChronoUnit.DAYS)
            val oneHourBefore = appointmentDateTime.minus(1, ChronoUnit.HOURS)
            val currentDateTime = LocalDateTime.now()

            Log.d(TAG, "Appointment: $appointmentId, Appt Time: $appointmentDateTime, Current Time: $currentDateTime")

            if (oneDayBefore.isAfter(currentDateTime)) {
                scheduleNotification(appointment, oneDayBefore, NotificationType.ONE_DAY_BEFORE)
                Log.d(TAG, "Scheduled 1-day reminder for appointment $appointmentId")
            } else {
                Log.d(TAG, "1-day reminder time is in the past, skipping")
            }

            if (oneHourBefore.isAfter(currentDateTime)) {
                scheduleNotification(appointment, oneHourBefore, NotificationType.ONE_HOUR_BEFORE)
                Log.d(TAG, "Scheduled 1-hour reminder for appointment $appointmentId")
            } else {
                Log.d(TAG, "1-hour reminder time is in the past, skipping")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminders", e)
        }
    }

    /**
     * Schedule a notification at a specific time
     */
    private fun scheduleNotification(
        appointment: Appointment,
        notificationTime: LocalDateTime,
        notificationType: NotificationType
    ) {
        if (!hasExactAlarmPermission()) {
            Log.w(TAG, "No permission to schedule exact alarms. Requesting permission...")
            requestExactAlarmPermission()
            return
        }

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

        Log.d(TAG, "Scheduling notification: AppointmentID=$appointmentId, Type=$notificationType, Time=$notificationTimeMillis")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Schedule with appropriate method based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "Using setExactAndAllowWhileIdle for Android M+")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTimeMillis,
                    pendingIntent
                )
            } else {
                Log.d(TAG, "Using setExact for older Android versions")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled alarm for appointment $appointmentId")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException setting alarm: ${se.message}")
            requestExactAlarmPermission()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm: ${e.message}", e)
        }
    }

    /**
     * Cancel existing reminders for an appointment
     */
    fun cancelAppointmentReminders(appointment: Appointment) {
        val appointmentId = appointment.id ?: return
        Log.d(TAG, "Cancelling reminders for appointment $appointmentId")

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

        Log.d(TAG, "Successfully cancelled reminders for appointment $appointmentId")
    }

    /**
     * Create intent for the reminder broadcast receiver
     */
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
            // Add explicit action to make intent more specific
            action = ACTION_APPOINTMENT_REMINDER
        }
    }

    /**
     * Schedule all appointments for a user
     */
    suspend fun scheduleAllAppointmentReminders(userId: String) {
        try {
            val appointments = AppointmentRepository.getUpcomingAppointments(userId)

            Log.d(TAG, "Scheduling reminders for ${appointments.size} upcoming appointments")

            for (appointment in appointments) {
                if (appointment.reminders) {
                    scheduleAppointmentReminders(appointment)
                }
            }

            Log.d(TAG, "Successfully scheduled all appointment reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling all appointment reminders", e)
        }
    }

    private fun getDayBeforeRequestCode(appointmentId: Int): Int {
        return appointmentId * 10 + 1
    }

    private fun getHourBeforeRequestCode(appointmentId: Int): Int {
        return appointmentId * 10 + 2
    }

    companion object {
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
        const val EXTRA_REASON = "reason"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val ACTION_APPOINTMENT_REMINDER = "com.example.phms.ACTION_APPOINTMENT_REMINDER"
    }

    enum class NotificationType {
        ONE_DAY_BEFORE,
        ONE_HOUR_BEFORE
    }
}