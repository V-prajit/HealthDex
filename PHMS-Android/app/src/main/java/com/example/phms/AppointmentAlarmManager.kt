package com.example.phms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeParseException
import android.provider.Settings

class AppointmentAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(AlarmManager::class.java)
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun requestExactAlarmPermissionOnce() {
        val prefs = context.getSharedPreferences("alarms_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("asked_exact_alarm", false)) {
            requestExactAlarmPermission()
            prefs.edit().putBoolean("asked_exact_alarm", true).apply()
        }
    }


    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun scheduleAppointmentReminders(appointment: Appointment) {
        Log.d("AlarmManager", "scheduleAppointmentReminders called for appointment: ${appointment.id}, reminders enabled: ${appointment.reminders}")
        if (!appointment.reminders) {
            Log.d("AlarmManager", "Reminders are disabled for appointment ${appointment.id}, returning.")
            return
        }

        cancelAppointmentReminders(appointment)

        val appointmentId = appointment.id ?: return
        if (appointmentId == null) {
            Log.e("AlarmManager", "Appointment ID is null! Cannot schedule reminders.")
            return
        }

        try {
            val dateStr = appointment.date
            val timeStr = appointment.time

            Log.d("AlarmManager", "Parsing date: '$dateStr', time: '$timeStr' for appointment $appointmentId")

            val dateParts = dateStr.split("-")
            if (dateParts.size != 3) {
                Log.e("AlarmManager", "Invalid date format: '$dateStr' for appointment $appointmentId")
                return
            }

            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) {
                Log.e("AlarmManager", "Invalid time format: '$timeStr' for appointment $appointmentId")
                return
            }

            val year = dateParts[0].toIntOrNull()
            val month = dateParts[1].toIntOrNull()
            val day = dateParts[2].toIntOrNull()
            val hour = timeParts[0].toIntOrNull()
            val minute = timeParts[1].toIntOrNull()

            if (year == null || month == null || day == null || hour == null || minute == null) {
                Log.e("AlarmManager", "Failed to parse date/time components for appointment $appointmentId")
                return
            }

            val appointmentDateTime = LocalDateTime.of(
                year, month, day, hour, minute
            )

            val oneDayBefore = appointmentDateTime.minus(1, ChronoUnit.DAYS)
            val oneHourBefore = appointmentDateTime.minus(1, ChronoUnit.HOURS)

            val currentDateTime = LocalDateTime.now()

            Log.d("AlarmManager", "Appointment: $appointmentId, Appt Time: $appointmentDateTime, Current Time: $currentDateTime")
            Log.d("AlarmManager", "Checking 1 Day Before: $oneDayBefore (is future? ${oneDayBefore.isAfter(currentDateTime)})")
            Log.d("AlarmManager", "Checking 1 Hour Before: $oneHourBefore (is future? ${oneHourBefore.isAfter(currentDateTime)})")

            if (oneDayBefore.isAfter(currentDateTime)) {
                scheduleNotification(appointment, oneDayBefore, NotificationType.ONE_DAY_BEFORE)
                Log.d("AlarmManager", "Scheduled 1-day reminder for appointment $appointmentId") // Keep original log
            } else {
                Log.w("AlarmManager", "1-day reminder time ($oneDayBefore) is in the past for appointment $appointmentId, not scheduling.")
            }

            if (oneHourBefore.isAfter(currentDateTime)) {
                scheduleNotification(appointment, oneHourBefore, NotificationType.ONE_HOUR_BEFORE)
                Log.d("AlarmManager", "Scheduled 1-hour reminder for appointment $appointmentId") // Keep original log
            } else {
                Log.w("AlarmManager", "1-hour reminder time ($oneHourBefore) is in the past for appointment $appointmentId, not scheduling.")
            }

        } catch (e: DateTimeParseException) {
            Log.e("AlarmManager", "DateTimeParseException for appointment $appointmentId: ${e.message}")
        } catch (e: NumberFormatException) {
            Log.e("AlarmManager", "NumberFormatException parsing date/time for appointment $appointmentId: ${e.message}")
        } catch (e: Exception) {
            Log.e("AlarmManager", "Error scheduling reminders for appointment $appointmentId", e)
        }
    }

    fun cancelAppointmentReminders(appointment: Appointment) {
        val appointmentId = appointment.id ?: return
        Log.d("AlarmManager", "Cancelling reminders for appointment $appointmentId")

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
        if (!hasExactAlarmPermission()){
            requestExactAlarmPermissionOnce()
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

        Log.d("AlarmManager", "scheduleNotification called for Appt ID: $appointmentId, Type: $notificationType, Time (ms): $notificationTimeMillis, RequestCode: $requestCode")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm
        try { // Add try-catch around alarm setting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Check for exact alarm permission if needed on Android 12+
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                //     if (!alarmManager.canScheduleExactAlarms()) {
                //         Log.w("AlarmManager", "Cannot schedule exact alarms. Scheduling approximate for $appointmentId.")
                //         // Fallback to non-exact alarm or prompt user for permission
                //         alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTimeMillis, pendingIntent)
                //         return
                //     }
                // }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTimeMillis,
                    pendingIntent
                )
                Log.d("AlarmManager", "Scheduled using setExactAndAllowWhileIdle for $appointmentId")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTimeMillis,
                    pendingIntent
                )
                Log.d("AlarmManager", "Scheduled using setExact for $appointmentId")
            }
        } catch (se: SecurityException) {
            Log.e("AlarmManager", "SecurityException setting alarm for $appointmentId. Check SCHEDULE_EXACT_ALARM permission?", se)
        } catch (e: Exception) {
            Log.e("AlarmManager", "Exception setting alarm for $appointmentId", e)
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