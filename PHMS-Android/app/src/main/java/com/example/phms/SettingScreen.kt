package com.example.phms

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    headlineContent = { Text("Enable Biometric Login") },
                    supportingContent = { Text(if (biometricEnabled) "Enabled" else "Disabled") },
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
                    headlineContent = { Text("Enable Dark Mode") },
                    supportingContent = { Text(if (darkModeEnabled) "Enabled" else "Disabled") },
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
                    headlineContent = { Text("Vital Sign Thresholds") },
                    supportingContent = { Text("Set alert limits for vital signs") },
                    trailingContent = {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showThresholdDialog = true }
                )

                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.emergency_contacts)) },
                    supportingContent = { Text("Manage emergency contact notifications") },
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

                // Add test alert button for development
                ListItem(
                    headlineContent = { Text("Test Emergency Alert") },
                    supportingContent = { Text("Send a test alert (for development)") },
                    trailingContent = {
                        Icon(Icons.Default.Warning, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        scope.launch {
                            val userId = prefs.getString("LAST_USER_UID", null)
                            if (userId != null) {
                                val alertRequest = VitalAlertRequest(
                                    userId = userId,
                                    vitalName = "Heart Rate",
                                    value = 150f,
                                    threshold = 100f,
                                    isHigh = true
                                )

                                try {
                                    val response: retrofit2.Response<Map<String, Int>> = RetrofitClient.apiService.sendVitalAlert(alertRequest) // <-- CHANGE THIS

                                    if (response.isSuccessful) {
                                        val result = response.body()
                                        Toast.makeText(context, "Test alert sent successfully. Emails: ${result?.get("emailsSent") ?: 0}", Toast.LENGTH_LONG).show() // <-- Adjusted Toast
                                    } else {
                                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                        Log.e("TestAlertError", "Test alert failed: ${response.code()} - $errorBody")
                                        Toast.makeText(context, "Test alert failed: ${response.code()}", Toast.LENGTH_SHORT).show() // <-- Adjusted Toast
                                    }
                                } catch (e: Exception) {
                                    Log.e("TestAlertError", "Failed to send test alert", e)
                                    val errorMsg = e.message ?: e.toString()
                                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "No user ID found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Divider()
                ListItem(
                    headlineContent = { Text("Test Appointment Reminder") },
                    supportingContent = { Text("Send a test notification") },
                    trailingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        // Create and show a direct notification without using WorkManager
                        val testAppointment = Appointment(
                            id = 999,
                            userId = prefs.getString("LAST_USER_UID", "") ?: "",
                            doctorId = 1,
                            doctorName = "Dr. Test Doctor",
                            date = LocalDate.now().toString(),
                            time = "12:00",
                            duration = 30,
                            reason = "Test Appointment",
                            notes = "This is a test notification",
                            status = "scheduled",
                            reminders = true
                        )

                        val notificationManager = AppointmentNotificationManager(context)
                        notificationManager.showAppointmentReminder(testAppointment)

                        Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show()
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
                                            LocaleHelper.applyLanguageWithoutRecreation(context, language.code)
                                            showLanguageDialog = false
                                            onBackClick()
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
        title = { Text("Set Vital Sign Thresholds") },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState()) // Make dialog content scrollable
            ) {
                ThresholdTextField(label = "Heart Rate High (bpm)", value = hrHigh, onValueChange = { hrHigh = it })
                ThresholdTextField(label = "Heart Rate Low (bpm)", value = hrLow, onValueChange = { hrLow = it })
                Spacer(Modifier.height(8.dp))
                ThresholdTextField(label = "Systolic BP High (mmHg)", value = bpSysHigh, onValueChange = { bpSysHigh = it })
                ThresholdTextField(label = "Systolic BP Low (mmHg)", value = bpSysLow, onValueChange = { bpSysLow = it })
                 Spacer(Modifier.height(8.dp))
                 ThresholdTextField(label = "Diastolic BP High (mmHg)", value = bpDiaHigh, onValueChange = { bpDiaHigh = it })
                 ThresholdTextField(label = "Diastolic BP Low (mmHg)", value = bpDiaLow, onValueChange = { bpDiaLow = it })
                 Spacer(Modifier.height(8.dp))
                 ThresholdTextField(label = "Glucose High (mg/dL)", value = glucoseHigh, onValueChange = { glucoseHigh = it })
                 ThresholdTextField(label = "Glucose Low (mg/dL)", value = glucoseLow, onValueChange = { glucoseLow = it })
                 Spacer(Modifier.height(8.dp))
                 ThresholdTextField(label = "Cholesterol High (mg/dL)", value = cholesterolHigh, onValueChange = { cholesterolHigh = it })
                 ThresholdTextField(label = "Cholesterol Low (mg/dL)", value = cholesterolLow, onValueChange = { cholesterolLow = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Validate and save
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
                 // Basic validation example: ensure low < high
                if (newThresholds.hrLow >= newThresholds.hrHigh ||
                     newThresholds.bpSysLow >= newThresholds.bpSysHigh ||
                     newThresholds.bpDiaLow >= newThresholds.bpDiaHigh ||
                     newThresholds.glucoseLow >= newThresholds.glucoseHigh ||
                     newThresholds.cholesterolLow >= newThresholds.cholesterolHigh) {
                     Toast.makeText(context, "Low threshold cannot be higher than high threshold", Toast.LENGTH_LONG).show()
                 } else {
                     viewModel.saveThresholds(newThresholds)
                     onDismiss()
                 }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        // *** CORRECTED LINE BELOW ***
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}