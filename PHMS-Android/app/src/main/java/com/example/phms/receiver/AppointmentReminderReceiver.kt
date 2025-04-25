package com.example.phms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.phms.data.model.Appointment
import com.example.phms.domain.repository.AppointmentRepository
import com.example.phms.service.alarm.AppointmentAlarmManager
import com.example.phms.service.notification.AppointmentNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppointmentReminderReceiver : BroadcastReceiver() {
    private val TAG = "AppointmentReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: action=${intent.action}")

        // Handle device boot to reschedule alarms
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling all appointment reminders")
            handleBootCompleted(context)
            return
        }

        // Check if this is our specific appointment reminder action
        if (intent.action != AppointmentAlarmManager.ACTION_APPOINTMENT_REMINDER) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }

        val appointmentId = intent.getIntExtra(AppointmentAlarmManager.EXTRA_APPOINTMENT_ID, -1)
        if (appointmentId == -1) {
            Log.e(TAG, "Invalid appointment ID")
            return
        }

        Log.d(TAG, "Processing reminder for appointment ID: $appointmentId")

        val doctorName = intent.getStringExtra(AppointmentAlarmManager.EXTRA_DOCTOR_NAME) ?: "Doctor"
        val dateStr = intent.getStringExtra(AppointmentAlarmManager.EXTRA_DATE) ?: ""
        val timeStr = intent.getStringExtra(AppointmentAlarmManager.EXTRA_TIME) ?: ""
        val reason = intent.getStringExtra(AppointmentAlarmManager.EXTRA_REASON) ?: ""
        val userId = intent.getStringExtra(AppointmentAlarmManager.EXTRA_USER_ID) ?: ""
        val notificationTypeStr = intent.getStringExtra(AppointmentAlarmManager.EXTRA_NOTIFICATION_TYPE)

        val notificationType = try {
            AppointmentAlarmManager.NotificationType.valueOf(notificationTypeStr ?: "ONE_DAY_BEFORE")
        } catch (e: Exception) {
            Log.e(TAG, "Invalid notification type: $notificationTypeStr", e)
            AppointmentAlarmManager.NotificationType.ONE_DAY_BEFORE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the latest appointment data from repository
                val appointment = AppointmentRepository.getAppointment(appointmentId)

                if (appointment != null && appointment.reminders) {
                    Log.d(TAG, "Showing notification for appointment with doctor: ${appointment.doctorName}")

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

                    Log.d(TAG, "Successfully showed notification for appointment $appointmentId")
                } else {
                    Log.d(TAG, "Appointment $appointmentId not found or reminders disabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing appointment reminder", e)
            }
        }
    }

    /**
     * Handle device boot completed by rescheduling all alarms
     */
    private fun handleBootCompleted(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("LAST_USER_UID", null)

                if (userId != null) {
                    Log.d(TAG, "Rescheduling appointments for user: $userId")
                    val alarmManager = AppointmentAlarmManager(context)
                    alarmManager.scheduleAllAppointmentReminders(userId)
                } else {
                    Log.w(TAG, "No user ID found, cannot reschedule appointments")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling appointment alarms", e)
            }
        }
    }
}