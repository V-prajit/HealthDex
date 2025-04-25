package com.example.phms.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.phms.util.MainActivity
import com.example.phms.R
import com.example.phms.data.model.Appointment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AppointmentNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "appointment_reminders"
        private const val NOTIFICATION_ID_PREFIX = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Appointment Reminders"
            val descriptionText = "Notifications for upcoming doctor appointments"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAppointmentReminder(appointment: Appointment) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Create an intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_APPOINTMENTS", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format date and time for notification
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
        val date = LocalDate.parse(appointment.date)
        val formattedDate = date.format(dateFormatter)

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming Appointment Reminder")
            .setContentText("You have an appointment with ${appointment.doctorName} at ${appointment.time} on $formattedDate")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You have an appointment with ${appointment.doctorName} at ${appointment.time} on $formattedDate.\nReason: ${appointment.reason}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show notification with permission check
        try {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = NOTIFICATION_ID_PREFIX + (appointment.id ?: 0)
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationManager", "Permission denied for notification", e)
        }
    }

    fun showAppointmentReminderWithMessage(
        appointment: Appointment,
        message: String,
        notificationId: Int = NOTIFICATION_ID_PREFIX + (appointment.id ?: 0)
    ) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Create an intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_APPOINTMENTS", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message)
            .setContentText("Appointment with ${appointment.doctorName} at ${appointment.time}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\nReason: ${appointment.reason}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show notification
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationManager", "Permission denied for notification", e)
        }
    }
}