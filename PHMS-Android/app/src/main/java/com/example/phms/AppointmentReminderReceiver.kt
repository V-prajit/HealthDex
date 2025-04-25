package com.example.phms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AppointmentReceiver", ">>> ReminderReceiver triggered! <<< Intent Action: ${intent.action}") // Log trigger

        val appointmentId = intent.getIntExtra(AppointmentAlarmManager.EXTRA_APPOINTMENT_ID, -1)
        if (appointmentId == -1) {
            Log.e("AppointmentReceiver", "Invalid appointment ID")
            return
        }

        val doctorName = intent.getStringExtra(AppointmentAlarmManager.EXTRA_DOCTOR_NAME) ?: "Doctor"
        val dateStr = intent.getStringExtra(AppointmentAlarmManager.EXTRA_DATE) ?: ""
        val timeStr = intent.getStringExtra(AppointmentAlarmManager.EXTRA_TIME) ?: ""
        val reason = intent.getStringExtra(AppointmentAlarmManager.EXTRA_REASON) ?: ""
        val userId = intent.getStringExtra(AppointmentAlarmManager.EXTRA_USER_ID) ?: ""
        val notificationTypeStr = intent.getStringExtra(AppointmentAlarmManager.EXTRA_NOTIFICATION_TYPE)

        val notificationType = try {
            AppointmentAlarmManager.NotificationType.valueOf(notificationTypeStr ?: "ONE_DAY_BEFORE")
        } catch (e: Exception) {
            AppointmentAlarmManager.NotificationType.ONE_DAY_BEFORE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appointment = AppointmentRepository.getAppointment(appointmentId)

                if (appointment != null && appointment.reminders) {
                    val notificationAppointment = Appointment(
                        id = appointmentId,
                        userId = userId,
                        doctorId = appointment.doctorId,
                        doctorName = doctorName,
                        date = dateStr,
                        time = timeStr,
                        duration = appointment.duration,
                        reason = reason,
                        status = appointment.status,
                        reminders = true
                    )

                    val message = when (notificationType) {
                        AppointmentAlarmManager.NotificationType.ONE_DAY_BEFORE ->
                            "You have an appointment tomorrow"
                        AppointmentAlarmManager.NotificationType.ONE_HOUR_BEFORE ->
                            "You have an appointment in 1 hour"
                    }

                    val notificationId = when (notificationType) {
                        AppointmentAlarmManager.NotificationType.ONE_DAY_BEFORE ->
                            1000 + appointmentId
                        AppointmentAlarmManager.NotificationType.ONE_HOUR_BEFORE ->
                            2000 + appointmentId
                    }

                    // Show the notification
                    val notificationManager = AppointmentNotificationManager(context)
                    notificationManager.showAppointmentReminderWithMessage(
                        notificationAppointment,
                        message,
                        notificationId
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentReceiver", "Error processing appointment reminder", e)
            }
        }
    }
}