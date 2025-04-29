package com.example.phms.Screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.widget.Toast
import androidx.compose.material.icons.filled.Alarm
import com.example.phms.Appointment
import com.example.phms.AppointmentAlarmManager
import com.example.phms.AppointmentNotificationManager
import com.example.phms.LocaleHelper
import com.example.phms.MainActivity
import com.example.phms.R
import com.example.phms.RetrofitClient
import com.example.phms.ThresholdValues
import com.example.phms.VitalAlertRequest
import com.example.phms.VitalSignsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    vitalSignsViewModel: VitalSignsViewModel = viewModel()
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showEmergencyContacts by remember { mutableStateOf(false) }
    val currentLanguageCode = remember { LocaleHelper.getCurrentLanguageCode(context) }
    val currentLanguage = remember(currentLanguageCode) {
        LocaleHelper.supportedLanguages.find { it.code == currentLanguageCode } ?: LocaleHelper.supportedLanguages[0]
    }

    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    var biometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("LAST_USER_BIOMETRIC", false))
    }
    var darkModeEnabled by remember {
        mutableStateOf(prefs.getBoolean("DARK_MODE", false))
    }

    // Fetch string resources outside lambdas where needed
    val enabledStr = stringResource(R.string.enabled)
    val disabledStr = stringResource(R.string.disabled)
    val testAlertSuccessToastStr = stringResource(R.string.test_alert_success_toast)
    val testAlertFailToastStr = stringResource(R.string.test_alert_failed_toast)
    val unknownErrorStr = stringResource(R.string.login_error_unknown)
    val userNotFoundStr = stringResource(R.string.user_not_found)
    val errorTemplateStr = stringResource(R.string.error)
    val testDrNameStr = stringResource(R.string.test_dr_name)
    val testAppointmentReasonStr = stringResource(R.string.test_appointment_reason)
    val testAppointmentNotesStr = stringResource(R.string.test_appointment_notes)
    val testNotificationSentToastStr = stringResource(R.string.test_notification_sent_toast)
    val remindersScheduledToastStr = stringResource(R.string.reminders_scheduled_toast)
    val userIdNotFoundToastStr = stringResource(R.string.user_id_not_found_toast)

    if (showEmergencyContacts) {
        EmergencyContactsScreen(
            userId = prefs.getString("LAST_USER_UID", "") ?: "",
            onBackClick = { showEmergencyContacts = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language)) },
                    supportingContent = { Text(currentLanguage.displayName) },
                    trailingContent = {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showLanguageDialog = true }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.enable_biometric_login_title)) },
                    supportingContent = { Text(if (biometricEnabled) enabledStr else disabledStr) }, // Use variable
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { checked ->
                                biometricEnabled = checked
                                prefs.edit().putBoolean("LAST_USER_BIOMETRIC", checked).apply()
                            }
                        )
                    }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.enable_dark_mode_title)) },
                    supportingContent = { Text(if (darkModeEnabled) enabledStr else disabledStr) }, // Use variable
                    trailingContent = {
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = { checked ->
                                darkModeEnabled = checked
                                prefs.edit().putBoolean("DARK_MODE", checked).apply()
                                (context as? MainActivity)?.updateTheme(checked)
                            }
                        )
                    }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.vital_threshold_title)) },
                    supportingContent = { Text(stringResource(R.string.vital_threshold_desc)) },
                    trailingContent = {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showThresholdDialog = true }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.emergency_contacts)) },
                    supportingContent = { Text(stringResource(R.string.emergency_contacts_desc)) },
                    trailingContent = {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showEmergencyContacts = true }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.logout)) },
                    supportingContent = { Text(stringResource(R.string.sign_out_description)) },
                    trailingContent = {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onLogout() }
                )

                Divider()


                ListItem(
                    headlineContent = { Text(stringResource(R.string.test_emergency_alert_title)) },
                    supportingContent = { Text(stringResource(R.string.test_emergency_alert_desc)) },
                    trailingContent = {
                        Icon(Icons.Default.Warning, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        scope.launch {
                            val userId = prefs.getString("LAST_USER_UID", null)
                            val testAlertVitalNameStr = context.getString(R.string.test_alert_vital_name) // Fetch here
                            if (userId != null) {
                                val alertRequest = VitalAlertRequest(
                                    userId = userId,
                                    vitalName = testAlertVitalNameStr, // Use variable
                                    value = 150f,
                                    threshold = 100f,
                                    isHigh = true
                                )

                                try {
                                    val response: retrofit2.Response<Map<String, Int>> = RetrofitClient.apiService.sendVitalAlert(alertRequest)

                                    if (response.isSuccessful) {
                                        val result = response.body()
                                        Toast.makeText(context, String.format(testAlertSuccessToastStr, result?.get("emailsSent") ?: 0), Toast.LENGTH_LONG).show() // Use variable
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: unknownErrorStr // Use variable
                                        Log.e("TestAlertError", "Test alert failed: ${response.code()} - $errorBody")
                                        Toast.makeText(context, String.format(testAlertFailToastStr, response.code()), Toast.LENGTH_SHORT).show() // Use variable
                                    }
                                } catch (e: Exception) {
                                    Log.e("TestAlertError", "Failed to send test alert", e)
                                    val errorMsg = e.message ?: e.toString()
                                    Toast.makeText(context, String.format(errorTemplateStr, errorMsg), Toast.LENGTH_SHORT).show() // Use variable
                                }
                            } else {
                                Toast.makeText(context, userNotFoundStr, Toast.LENGTH_SHORT).show() // Use variable
                            }
                        }
                    }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.test_appointment_reminder_title)) },
                    supportingContent = { Text(stringResource(R.string.test_appointment_reminder_desc)) },
                    trailingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    modifier = Modifier.clickable {

                        val testAppointment = Appointment(
                            id = 999,
                            userId = prefs.getString("LAST_USER_UID", "") ?: "",
                            doctorId = 1,
                            doctorName = testDrNameStr, // Use variable
                            date = LocalDate.now().toString(),
                            time = "12:00",
                            duration = 30,
                            reason = testAppointmentReasonStr, // Use variable
                            notes = testAppointmentNotesStr, // Use variable
                            status = "scheduled",
                            reminders = true
                        )


                        val notificationManager = AppointmentNotificationManager(context)
                        notificationManager.showAppointmentReminder(testAppointment)

                        Toast.makeText(context, testNotificationSentToastStr, Toast.LENGTH_SHORT).show() // Use variable
                    }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.schedule_reminders_title)) },
                    supportingContent = { Text(stringResource(R.string.schedule_reminders_desc)) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable {
                        val userId = prefs.getString("LAST_USER_UID", null)
                        if (userId != null) {
                            scope.launch {
                                val alarmManager = AppointmentAlarmManager(context)
                                alarmManager.scheduleAllAppointmentReminders(userId)
                                Toast.makeText(context, remindersScheduledToastStr, Toast.LENGTH_SHORT).show() // Use variable
                            }
                        } else {
                            Toast.makeText(context, userIdNotFoundToastStr, Toast.LENGTH_SHORT).show() // Use variable
                        }
                    }
                )
            }

            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.select_language)) },
                    text = {
                        LazyColumn {
                            items(LocaleHelper.supportedLanguages) { language ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LocaleHelper.applyLanguageWithoutRecreation(
                                                context,
                                                language.code
                                            )
                                            showLanguageDialog = false
                                            // Don't call onBackClick here, let MainActivity handle recomposition if needed
                                            // onBackClick()
                                        }
                                        .padding(vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(language.displayName)

                                    if (language.code == currentLanguageCode) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (language != LocaleHelper.supportedLanguages.last()) {
                                    Divider()
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showThresholdDialog) {
                ThresholdSettingsDialog(
                    viewModel = vitalSignsViewModel,
                    onDismiss = { showThresholdDialog = false }
                )
            }
        }
    }
}


@Composable
fun ThresholdSettingsDialog(
    viewModel: VitalSignsViewModel,
    onDismiss: () -> Unit
) {
    val currentThresholds by viewModel.thresholds.collectAsState()
    val context = LocalContext.current
    val errorThresholdLowHighStr = stringResource(R.string.error_threshold_low_high) // Fetch string

    var hrHigh by remember { mutableStateOf(currentThresholds.hrHigh.toString()) }
    var hrLow by remember { mutableStateOf(currentThresholds.hrLow.toString()) }
    var bpSysHigh by remember { mutableStateOf(currentThresholds.bpSysHigh.toString()) }
    var bpSysLow by remember { mutableStateOf(currentThresholds.bpSysLow.toString()) }
    var bpDiaHigh by remember { mutableStateOf(currentThresholds.bpDiaHigh.toString()) }
    var bpDiaLow by remember { mutableStateOf(currentThresholds.bpDiaLow.toString()) }
    var glucoseHigh by remember { mutableStateOf(currentThresholds.glucoseHigh.toString()) }
    var glucoseLow by remember { mutableStateOf(currentThresholds.glucoseLow.toString()) }
    var cholesterolHigh by remember { mutableStateOf(currentThresholds.cholesterolHigh.toString()) }
    var cholesterolLow by remember { mutableStateOf(currentThresholds.cholesterolLow.toString()) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vital_threshold_title)) },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ThresholdTextField(label = stringResource(R.string.hr_label), value = hrHigh, onValueChange = { hrHigh = it })
                ThresholdTextField(label = stringResource(R.string.hr_low_label), value = hrLow, onValueChange = { hrLow = it })
                Spacer(Modifier.height(8.dp))
                ThresholdTextField(label = stringResource(R.string.systolic_label), value = bpSysHigh, onValueChange = { bpSysHigh = it })
                ThresholdTextField(label = stringResource(R.string.systolic_low_label), value = bpSysLow, onValueChange = { bpSysLow = it })
                Spacer(Modifier.height(8.dp))
                ThresholdTextField(label = stringResource(R.string.diastolic_label), value = bpDiaHigh, onValueChange = { bpDiaHigh = it })
                ThresholdTextField(label = stringResource(R.string.diastolic_low_label), value = bpDiaLow, onValueChange = { bpDiaLow = it })
                Spacer(Modifier.height(8.dp))
                ThresholdTextField(label = stringResource(R.string.glucose_label), value = glucoseHigh, onValueChange = { glucoseHigh = it })
                ThresholdTextField(label = stringResource(R.string.glucose_low_label), value = glucoseLow, onValueChange = { glucoseLow = it })
                Spacer(Modifier.height(8.dp))
                ThresholdTextField(label = stringResource(R.string.cholesterol_label), value = cholesterolHigh, onValueChange = { cholesterolHigh = it })
                ThresholdTextField(label = stringResource(R.string.cholesterol_low_label), value = cholesterolLow, onValueChange = { cholesterolLow = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {

                val newThresholds = ThresholdValues(
                    hrHigh = hrHigh.toFloatOrNull() ?: currentThresholds.hrHigh,
                    hrLow = hrLow.toFloatOrNull() ?: currentThresholds.hrLow,
                    bpSysHigh = bpSysHigh.toFloatOrNull() ?: currentThresholds.bpSysHigh,
                    bpSysLow = bpSysLow.toFloatOrNull() ?: currentThresholds.bpSysLow,
                    bpDiaHigh = bpDiaHigh.toFloatOrNull() ?: currentThresholds.bpDiaHigh,
                    bpDiaLow = bpDiaLow.toFloatOrNull() ?: currentThresholds.bpDiaLow,
                    glucoseHigh = glucoseHigh.toFloatOrNull() ?: currentThresholds.glucoseHigh,
                    glucoseLow = glucoseLow.toFloatOrNull() ?: currentThresholds.glucoseLow,
                    cholesterolHigh = cholesterolHigh.toFloatOrNull() ?: currentThresholds.cholesterolHigh,
                    cholesterolLow = cholesterolLow.toFloatOrNull() ?: currentThresholds.cholesterolLow
                )

                if (newThresholds.hrLow >= newThresholds.hrHigh ||
                    newThresholds.bpSysLow >= newThresholds.bpSysHigh ||
                    newThresholds.bpDiaLow >= newThresholds.bpDiaHigh ||
                    newThresholds.glucoseLow >= newThresholds.glucoseHigh ||
                    newThresholds.cholesterolLow >= newThresholds.cholesterolHigh) {
                    Toast.makeText(context, errorThresholdLowHighStr, Toast.LENGTH_LONG).show() // Use variable
                } else {
                    viewModel.saveThresholds(newThresholds)
                    onDismiss()
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ThresholdTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },

        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}