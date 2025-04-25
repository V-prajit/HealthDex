package com.example.phms

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onViewDoctors: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var showAppointmentDialog by remember { mutableStateOf(false) }
    var currentAppointment by remember { mutableStateOf<Appointment?>(null) }
    var confirmDeleteDialog by remember { mutableStateOf<Appointment?>(null) }
    var showAllAppointments by remember { mutableStateOf(false) }
    val settingsLabel = stringResource(R.string.settings)

    // Load appointments and doctors when screen is shown
    LaunchedEffect(userId, showAllAppointments) {
        if (userId != null) {
            doctors = DoctorRepository.getDoctors(userId)
            appointments = if (showAllAppointments) {
                AppointmentRepository.getAppointments(userId)
            } else {
                AppointmentRepository.getUpcomingAppointments(userId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appointments)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onViewDoctors) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = stringResource(R.string.doctors)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = settingsLabel
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (doctors.isEmpty()) {
                        // If no doctors, redirect to doctor screen
                        onViewDoctors()
                    } else {
                        currentAppointment = null // Ensure we're creating a new appointment
                        showAppointmentDialog = true
                    }
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_appointment))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Toggle switch for showing all vs upcoming appointments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (showAllAppointments)
                        stringResource(R.string.all_appointments)
                    else
                        stringResource(R.string.upcoming_appointments),
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = showAllAppointments,
                    onCheckedChange = { showAllAppointments = it }
                )
            }

            if (appointments.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_appointments),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tap_to_add_appointment),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Group appointments by date
                val groupedAppointments = appointments.groupBy { it.date }
                val sortedDates = groupedAppointments.keys.sortedBy { LocalDate.parse(it) }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (date in sortedDates) {
                        val formattedDate = formatDate(date)
                        item {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(groupedAppointments[date]!!.sortedBy { it.time }) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                doctorName = appointment.doctorName ?: doctors.find { it.id == appointment.doctorId }?.name ?: "",
                                onEdit = {
                                    currentAppointment = appointment
                                    showAppointmentDialog = true
                                },
                                onDelete = {
                                    confirmDeleteDialog = appointment
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Edit/Add Appointment Dialog
    if (showAppointmentDialog) {
        AppointmentDialog(
            appointment = currentAppointment,
            userId = userId ?: "",
            doctors = doctors,
            onSave = { appointment ->
                scope.launch {
                    if (appointment.id == null) {
                        Log.d("ApptScreenSave", "Attempting to add new appointment: $appointment") // Log data being sent
                        val saved = AppointmentRepository.addAppointment(appointment)
                        Log.d("ApptScreenSave", "AppointmentRepository returned: $saved") // Log the result

                        if (saved != null && saved.reminders) {
                            Log.d("ApptScreenSave", "Scheduling reminders for ID: ${saved.id}") // Log before scheduling
                            AppointmentAlarmManager(context).scheduleAppointmentReminders(saved)
                        } else{
                            Log.w("ApptScreenSave", "NOT scheduling reminders. Saved is null? ${saved == null}. Reminders enabled? ${saved?.reminders}") // Log why not scheduling
                        }
                    } else {
                        AppointmentRepository.updateAppointment(appointment)
                        val mgr = AppointmentAlarmManager(context)

                        if (appointment.reminders) {
                            mgr.scheduleAppointmentReminders(appointment)
                        } else {
                            mgr.cancelAppointmentReminders(appointment)
                        }
                    }

                    // Refresh appointment list
                    if (userId != null) {
                        appointments = if (showAllAppointments) {
                            AppointmentRepository.getAppointments(userId)
                        } else {
                            AppointmentRepository.getUpcomingAppointments(userId)
                        }
                    }
                    showAppointmentDialog = false
                }
            },
            onCancel = {
                showAppointmentDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    if (confirmDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = {
                val doctorName = confirmDeleteDialog!!.doctorName ?:
                doctors.find { it.id == confirmDeleteDialog!!.doctorId }?.name ?: ""
                Text(
                    stringResource(
                        R.string.delete_appointment_confirmation,
                        formatDate(confirmDeleteDialog!!.date),
                        confirmDeleteDialog!!.time,
                        doctorName
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val appointmentToDelete = confirmDeleteDialog
                            if (appointmentToDelete?.id != null) {
                                AppointmentRepository.deleteAppointment(appointmentToDelete.id)
                                // Refresh appointment list
                                if (userId != null) {
                                    appointments = if (showAllAppointments) {
                                        AppointmentRepository.getAppointments(userId)
                                    } else {
                                        AppointmentRepository.getUpcomingAppointments(userId)
                                    }
                                }
                            }
                            confirmDeleteDialog = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    doctorName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${appointment.time} (${appointment.duration} min)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Status badge
                AppointmentStatusBadge(status = appointment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = doctorName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appointment.reason,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!appointment.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appointment.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Reminders toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (appointment.reminders) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (appointment.reminders) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (appointment.reminders) stringResource(R.string.reminders_on) else stringResource(R.string.reminders_off),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentStatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "scheduled" -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        "completed" -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
        "cancelled" -> Pair(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDialog(
    appointment: Appointment?,
    userId: String,
    doctors: List<Doctor>,
    onSave: (Appointment) -> Unit,
    onCancel: () -> Unit
) {
    // Dialog state
    var selectedDoctorId by remember { mutableStateOf(appointment?.doctorId ?: (doctors.firstOrNull()?.id ?: 0)) }
    var date by remember { mutableStateOf(appointment?.date ?: LocalDate.now().toString()) }
    var time by remember { mutableStateOf(appointment?.time ?: "09:00") }
    var duration by remember { mutableStateOf((appointment?.duration ?: 30).toString()) }
    var reason by remember { mutableStateOf(appointment?.reason ?: "") }
    var notes by remember { mutableStateOf(appointment?.notes ?: "") }
    var status by remember { mutableStateOf(appointment?.status ?: "scheduled") }
    var reminders by remember { mutableStateOf(appointment?.reminders ?: true) }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.parse(date).toEpochDay() * 24 * 60 * 60 * 1000
    )

    // Error states
    var reasonError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }

    // Status options
    val statusOptions = listOf("scheduled", "completed", "cancelled")
    var statusExpanded by remember { mutableStateOf(false) }

    // Doctor dropdown
    var doctorExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = if (appointment == null)
                    stringResource(R.string.add_appointment)
                else
                    stringResource(R.string.edit_appointment)
            )
        },
        text = {
            Column {
                // Doctor selection
                Box {
                    OutlinedTextField(
                        value = doctors.find { it.id == selectedDoctorId }?.name ?: "",
                        onValueChange = { },
                        label = { Text(stringResource(R.string.doctor)) },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { doctorExpanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        doctors.forEach { doctor ->
                            DropdownMenuItem(
                                text = { Text(doctor.name) },
                                onClick = {
                                    selectedDoctorId = doctor.id ?: 0
                                    doctorExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date picker
                OutlinedTextField(
                    value = formatDate(date),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.date)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                var showTimePicker by remember { mutableStateOf(false) }
                var selectedHour by remember { mutableStateOf(time.split(":").getOrNull(0)?.toIntOrNull() ?: 9) }
                var selectedMinute by remember { mutableStateOf(time.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", selectedHour, selectedMinute),
                        onValueChange = { /* Read only, managed by picker */ },
                        label = { Text(stringResource(R.string.time)) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Select time")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (showTimePicker) {
                        TimePickerDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    time = String.format("%02d:%02d", selectedHour, selectedMinute)
                                    showTimePicker = false
                                }) {
                                    Text(stringResource(R.string.ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTimePicker = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        ) {
                            TimePicker(
                                initialHour = selectedHour,
                                initialMinute = selectedMinute,
                                onTimeChange = { hour, minute ->
                                    selectedHour = hour
                                    selectedMinute = minute
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = duration,
                        onValueChange = {
                            duration = it
                            durationError = it.toIntOrNull() == null || it.toIntOrNull() ?: 0 <= 0
                        },
                        label = { Text(stringResource(R.string.duration_minutes)) },
                        isError = durationError,
                        supportingText = {
                            if (durationError) {
                                Text(stringResource(R.string.must_be_number))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reason
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        reasonError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.reason)) },
                    isError = reasonError,
                    supportingText = {
                        if (reasonError) {
                            Text(stringResource(R.string.required_field))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status
                Box {
                    OutlinedTextField(
                        value = status.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        onValueChange = { },
                        label = { Text(stringResource(R.string.status)) },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { statusExpanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    })
                                },
                                onClick = {
                                    status = option
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminders
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = reminders,
                        onCheckedChange = { reminders = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.enable_reminders))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    reasonError = reason.isBlank()
                    durationError = duration.toIntOrNull() == null || duration.toIntOrNull() ?: 0 <= 0

                    if (!reasonError && !durationError) {
                        val updatedAppointment = Appointment(
                            id = appointment?.id,
                            userId = userId,
                            doctorId = selectedDoctorId,
                            doctorName = doctors.find { it.id == selectedDoctorId }?.name,
                            date = date,
                            time = time,
                            duration = duration.toIntOrNull() ?: 30,
                            reason = reason,
                            notes = notes,
                            status = status,
                            reminders = reminders
                        )
                        onSave(updatedAppointment)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val selectedDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                            date = selectedDate.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Helper function to format date
fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun TimePicker(
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onTimeChange: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        // Hour picker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    hour = if (hour <= 0) 23 else hour - 1
                    onTimeChange(hour, minute)
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase hour")
            }

            Text(
                text = String.format("%02d", hour),
                style = MaterialTheme.typography.headlineLarge
            )

            IconButton(
                onClick = {
                    hour = if (hour >= 23) 0 else hour + 1
                    onTimeChange(hour, minute)
                }
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease hour")
            }

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = {
                    minute = if (minute <= 0) 55 else minute - 5
                    onTimeChange(hour, minute)
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase minute")
            }

            Text(
                text = String.format("%02d", minute),
                style = MaterialTheme.typography.headlineLarge
            )

            IconButton(
                onClick = {
                    minute = if (minute >= 55) 0 else minute + 5
                    onTimeChange(hour, minute)
                }
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease minute")
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = { content() },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}