package com.example.phms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


data class MedicationNotificationData(
    val id: Int,
    val name: String,
    val dosage: String,
    val instructions: String,
    val timeIndex: Int
)

class MedicationNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "medication_reminders"
        private const val NOTIFICATION_ID_PREFIX = 3000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.med_notification_channel_name)
            val descriptionText = context.getString(R.string.med_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicationReminder(data: MedicationNotificationData) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("MedicationNotificationManager", "No permission to post notifications")
                return
            }
        }


        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_MEDICATIONS", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val contentText = if (data.dosage.isNotEmpty()) {
            context.getString(R.string.med_notification_content_with_dosage, data.dosage, data.name)
        } else {
            context.getString(R.string.med_notification_content_no_dosage, data.name)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.med_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$contentText\n${data.instructions}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        val notificationId = NOTIFICATION_ID_PREFIX + data.id * 10 + data.timeIndex

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d("MedicationNotificationManager", "Showed notification for medication ${data.name}")
        } catch (e: SecurityException) {
            Log.e("MedicationNotificationManager", "Security exception showing notification", e)
        } catch (e: Exception) {
            Log.e("MedicationNotificationManager", "Error showing notification", e)
        }
    }
}