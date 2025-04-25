package com.example.phms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.phms.repository.MedicationRepository

class MedicationAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "MedicationAlarmManager"

    /**
     * Schedules notifications for a medication based on its frequency and time settings
     */
    fun scheduleMedicationReminders(medication: Medication) {
        Log.d(TAG, "Scheduling reminders for medication: ${medication.name}, time: ${medication.time}")

        // First cancel any existing reminders for this medication
        cancelMedicationReminders(medication)

        val medicationId = medication.id ?: return

        try {
            // Parse frequency (times per day)
            val frequency = medication.frequency.toIntOrNull() ?: 1
            Log.d(TAG, "Medication frequency: $frequency")

            // We need to parse the times list from the medication
            val timeList = parseMedicationTimes(medication)
            Log.d(TAG, "Parsed times: $timeList")

            if (timeList.isEmpty()) {
                Log.w(TAG, "No valid times found for medication ${medication.name}")
                return
            }

            Log.d(TAG, "Found ${timeList.size} reminder times for ${medication.name}")

            // Schedule an alarm for each time
            for ((index, timeString) in timeList.withIndex()) {
                val timeParts = timeString.split(":")
                if (timeParts.size != 2) {
                    Log.e(TAG, "Invalid time format: $timeString")
                    continue
                }

                val hour = timeParts[0].toIntOrNull() ?: continue
                val minute = timeParts[1].toIntOrNull() ?: continue
                Log.d(TAG, "Scheduling for time: $hour:$minute")

                // Schedule daily repeating notification at this time
                scheduleRepeatingNotification(medication, hour, minute, index)
            }

            Log.d(TAG, "Successfully scheduled all reminders for medication ${medication.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling medication reminders", e)
        }
    }

    /**
     * Parse medication times from the medication object
     * Medication times might be stored in different ways depending on your implementation
     */
    private fun parseMedicationTimes(medication: Medication): List<String> {
        // For example, if times are stored in a "time" field
        // In your MedicationDialog.kt, you use a timeList to store times

        // Check if time field has a list of times (implementation dependent)
        val defaultTime = "09:00"
        val freq = medication.frequency.toIntOrNull() ?: 1

        // If time field contains multiple times separated by comma
        return if (medication.time.contains(",")) {
            medication.time.split(",")
        } else {
            // Generate a list based on frequency if not available
            List(freq) { medication.time.takeIf { it.isNotEmpty() } ?: defaultTime }
        }
    }

    /**
     * Schedule a repeating notification for a specific time
     */
    private fun scheduleRepeatingNotification(
        medication: Medication,
        hour: Int,
        minute: Int,
        index: Int
    ) {
        if (!hasExactAlarmPermission()) {
            Log.w(TAG, "No permission to schedule exact alarms. Requesting permission...")
            requestExactAlarmPermission()
            return
        }

        // Calculate the next occurrence of this time
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)

        // If the time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= now) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val intent = createMedicationReminderIntent(medication, index)
        val requestCode = getMedicationReminderRequestCode(medication.id ?: 0, index)

        Log.d(TAG, "Scheduling medication reminder: ${medication.name}, Time: ${hour}:${minute}, RequestCode: $requestCode")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Schedule repeating alarm
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY, // Repeat daily
                pendingIntent
            )

            Log.d(TAG, "Successfully scheduled repeating alarm for medication ${medication.name} at ${hour}:${minute}")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException setting alarm: ${se.message}")
            requestExactAlarmPermission()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm: ${e.message}", e)
        }
    }

    /**
     * Cancel all reminders for a medication
     */
    fun cancelMedicationReminders(medication: Medication) {
        val medicationId = medication.id ?: return
        Log.d(TAG, "Cancelling reminders for medication $medicationId")

        // Cancel all possible time slots based on max frequency
        val maxFrequency = 4
        for (index in 0 until maxFrequency) {
            val intent = createMedicationReminderIntent(medication, index)
            val requestCode = getMedicationReminderRequestCode(medicationId, index)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling medication reminder", e)
            }
        }

        Log.d(TAG, "Cancelled all reminders for medication ${medication.name}")
    }

    /**
     * Create intent for the medication reminder broadcast
     */
    private fun createMedicationReminderIntent(medication: Medication, timeIndex: Int): Intent {
        return Intent(context, MedicationReminderReceiver::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medication.id)
            putExtra(EXTRA_MEDICATION_NAME, medication.name)
            putExtra(EXTRA_MEDICATION_DOSAGE, medication.dosage)
            putExtra(EXTRA_MEDICATION_INSTRUCTIONS, medication.instructions)
            putExtra(EXTRA_USER_ID, medication.userId)
            putExtra(EXTRA_TIME_INDEX, timeIndex)
            action = ACTION_MEDICATION_REMINDER
        }
    }

    /**
     * Schedule reminders for all medications for a user
     */
    fun scheduleAllMedicationReminders(userId: String) {
        try {
            MedicationRepository.fetchAll(userId) { medications ->
                if (medications != null) {
                    Log.d(TAG, "Scheduling reminders for ${medications.size} medications")

                    for (medication in medications) {
                        scheduleMedicationReminders(medication)
                    }

                    Log.d(TAG, "Successfully scheduled all medication reminders")
                } else {
                    Log.e(TAG, "No medications found for user $userId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling all medication reminders", e)
        }
    }

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

    private fun getMedicationReminderRequestCode(medicationId: Int, timeIndex: Int): Int {
        // Generate a unique request code for each medication and time index
        return medicationId * 100 + timeIndex
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_MEDICATION_DOSAGE = "medication_dosage"
        const val EXTRA_MEDICATION_INSTRUCTIONS = "medication_instructions"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_TIME_INDEX = "time_index"
        const val ACTION_MEDICATION_REMINDER = "com.example.phms.ACTION_MEDICATION_REMINDER"
    }
}