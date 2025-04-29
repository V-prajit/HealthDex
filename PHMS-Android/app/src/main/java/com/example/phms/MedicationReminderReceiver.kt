package com.example.phms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationReminderReceiver : BroadcastReceiver() {
    private val TAG = "MedicationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: action=${intent.action}")


        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, rescheduling all medication reminders")
            handleBootCompleted(context)
            return
        }


        if (intent.action != MedicationAlarmManager.ACTION_MEDICATION_REMINDER) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }

        val medicationId = intent.getIntExtra(MedicationAlarmManager.EXTRA_MEDICATION_ID, -1)
        if (medicationId == -1) {
            Log.e(TAG, "Invalid medication ID")
            return
        }

        Log.d(TAG, "Processing reminder for medication ID: $medicationId")

        val medicationName = intent.getStringExtra(MedicationAlarmManager.EXTRA_MEDICATION_NAME) ?: context.getString(R.string.default_medication_name)
        val dosage = intent.getStringExtra(MedicationAlarmManager.EXTRA_MEDICATION_DOSAGE) ?: ""
        val instructions = intent.getStringExtra(MedicationAlarmManager.EXTRA_MEDICATION_INSTRUCTIONS) ?: ""
        val userId = intent.getStringExtra(MedicationAlarmManager.EXTRA_USER_ID) ?: ""
        val timeIndex = intent.getIntExtra(MedicationAlarmManager.EXTRA_TIME_INDEX, 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {

                showMedicationNotification(context, medicationId, medicationName, dosage, instructions, timeIndex)
                Log.d(TAG, "Successfully showed notification for medication $medicationName")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing medication reminder", e)
            }
        }
    }


    private fun showMedicationNotification(
        context: Context,
        medicationId: Int,
        medicationName: String,
        dosage: String,
        instructions: String,
        timeIndex: Int
    ) {
        val notificationManager = MedicationNotificationManager(context)
        notificationManager.showMedicationReminder(
            MedicationNotificationData(
                id = medicationId,
                name = medicationName,
                dosage = dosage,
                instructions = instructions,
                timeIndex = timeIndex
            )
        )
    }


    private fun handleBootCompleted(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("LAST_USER_UID", null)

                if (userId != null) {
                    Log.d(TAG, "Rescheduling medications for user: $userId")
                    val alarmManager = MedicationAlarmManager(context)
                    alarmManager.scheduleAllMedicationReminders(userId)
                } else {
                    Log.w(TAG, "No user ID found, cannot reschedule medications")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling medication alarms", e)
            }
        }
    }
}