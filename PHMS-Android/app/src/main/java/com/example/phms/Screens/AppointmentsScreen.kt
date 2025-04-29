@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.phms.Screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.phms.Appointment
import com.example.phms.AppointmentAlarmManager
import com.example.phms.Doctor
import com.example.phms.R
import com.example.phms.repository.AppointmentRepository
import com.example.phms.repository.DoctorRepository
import com.example.phms.ui.theme.PokemonClassicFontFamily
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    LaunchedEffect(userId, showAllAppointments) {
        if (userId != null) {
            doctors = DoctorRepository.getDoctors(userId)
            appointments = AppointmentRepository.getAppointments(userId)
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
                        onViewDoctors()
                    } else {
                        currentAppointment = null
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
            AppointmentCalendarWithScroll(
                appointments = appointments,
                doctors = doctors,
                onEdit = {
                    currentAppointment = it
                    showAppointmentDialog = true
                },
                onDelete = {
                    confirmDeleteDialog = it
                }
            )
        }
    }

    if (showAppointmentDialog) {
        AppointmentDialog(
            appointment = currentAppointment,
            userId = userId ?: "",
            doctors = doctors,
            onSave = { appointment ->
                scope.launch {
                    if (appointment.id == null) {
                        Log.d("ApptScreenSave", "Attempting to add new appointment: $appointment")
                        val saved = AppointmentRepository.addAppointment(appointment)
                        Log.d("ApptScreenSave", "AppointmentRepository returned: $saved")

                        if (saved != null && saved.reminders) {
                            Log.d("ApptScreenSave", "Scheduling reminders for ID: ${saved.id}")
                            AppointmentAlarmManager(context).scheduleAppointmentReminders(saved)
                        } else{
                            Log.w("ApptScreenSave", "NOT scheduling reminders. Saved is null? ${saved == null}. Reminders enabled? ${saved?.reminders}")
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

                    if (userId != null) {
                        appointments = AppointmentRepository.getAppointments(userId)
                    }
                    showAppointmentDialog = false
                }
            },
            onCancel = {
                showAppointmentDialog = false
            }
        )
    }

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
                                if (userId != null) {
                                    appointments = AppointmentRepository.getAppointments(userId)
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
        shape = RoundedCornerShape(0.dp),
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
                        text = stringResource(R.string.appointment_time_duration, appointment.time, appointment.duration),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (appointment.reminders) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (appointment.reminders) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (appointment.reminders) stringResource(R.string.reminders_on) else stringResource(
                            R.string.reminders_off
                        ),
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
        shape = RoundedCornerShape(0.dp),
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
    var selectedDoctorId by remember { mutableStateOf(appointment?.doctorId ?: (doctors.firstOrNull()?.id ?: 0)) }
    var date by remember { mutableStateOf(appointment?.date ?: LocalDate.now().toString()) }
    var time by remember { mutableStateOf(appointment?.time ?: "09:00") }
    var duration by remember { mutableStateOf((appointment?.duration ?: 30).toString()) }
    var reason by remember { mutableStateOf(appointment?.reason ?: "") }
    var notes by remember { mutableStateOf(appointment?.notes ?: "") }
    var status by remember { mutableStateOf(appointment?.status ?: "scheduled") }
    var reminders by remember { mutableStateOf(appointment?.reminders ?: true) }

    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.parse(date).toEpochDay() * 24 * 60 * 60 * 1000
    )

    var reasonError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }

    val statusOptions = listOf("scheduled", "completed", "cancelled")
    var statusExpanded by remember { mutableStateOf(false) }

    var doctorExpanded by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(time.split(":").getOrNull(0)?.toIntOrNull() ?: 9) }
    var selectedMinute by remember { mutableStateOf(time.split(":").getOrNull(1)?.toIntOrNull() ?: 0) }

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

                OutlinedTextField(
                    value = formatDate(date),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.date)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date_desc))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", selectedHour, selectedMinute),
                        onValueChange = { },
                        label = { Text(stringResource(R.string.time)) },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.select_time_desc))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

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

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            time = String.format("%02d:%02d", selectedHour, selectedMinute),
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

    if (showTimePicker) {
        ThemedTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        time = String.format("%02d:%02d", selectedHour, selectedMinute)
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.ok), fontFamily = PokemonClassicFontFamily)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.cancel), fontFamily = PokemonClassicFontFamily)
                }
            }
        ) {
            ThemedTimePicker(
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                onTimeChange = { hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                }
            )
        }
    }
}

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
fun ThemedTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    hour = (hour - 1 + 24) % 24
                    onTimeChange(hour, minute)
                }) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.increase_hour_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    Text(
                        text = String.format("%02d", hour).substring(0, 1),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = PokemonClassicFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = String.format("%02d", hour).substring(1, 2),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = PokemonClassicFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                IconButton(onClick = {
                    hour = (hour + 1) % 24
                    onTimeChange(hour, minute)
                }) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.decrease_hour_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = ":",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = PokemonClassicFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    minute = (minute - 5 + 60) % 60
                    onTimeChange(hour, minute)
                }) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.increase_minute_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    Text(
                        text = String.format("%02d", minute).substring(0, 1),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = PokemonClassicFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = String.format("%02d", minute).substring(1, 2),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = PokemonClassicFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                IconButton(onClick = {
                    minute = (minute + 5) % 60
                    onTimeChange(hour, minute)
                }) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.decrease_minute_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ThemedTimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(
            stringResource(R.string.select_time_title),
            fontFamily = PokemonClassicFontFamily,
            color = MaterialTheme.colorScheme.onSurface
        ) },
        text = { content() },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun AppointmentCalendarWithScroll(
    appointments: List<Appointment>,
    doctors: List<Doctor>,
    onEdit: (Appointment) -> Unit,
    onDelete: (Appointment) -> Unit
) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return

    var calendarViewEnabled by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showAllAppointments by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val markedDates = appointments.mapNotNull {
        try { LocalDate.parse(it.date, formatter) } catch (e: Exception) { null }
    }.toSet()

    val today = remember { LocalDate.now() }
    val calendarState = rememberDatePickerState(
        initialDisplayedMonthMillis = today.toEpochDay() * 86_400_000,
        initialSelectedDateMillis = today.toEpochDay() * 86_400_000
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { calendarViewEnabled = !calendarViewEnabled }) {
                Icon(
                    imageVector = if (calendarViewEnabled) Icons.Default.ViewList else Icons.Default.DateRange,
                    contentDescription = stringResource(if (calendarViewEnabled) R.string.list_view_desc else R.string.calendar_view_desc)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(if (calendarViewEnabled) R.string.list_view else R.string.calendar_view))
            }
        }

        if (calendarViewEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
            ) {
                DatePicker(
                    state = calendarState,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(),
                    title = null,
                    headline = null
                )
            }

            LaunchedEffect(calendarState.selectedDateMillis) {
                calendarState.selectedDateMillis?.let {
                    selectedDate = LocalDate.ofEpochDay(it / 86_400_000)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val selectedDayAppointments = appointments.filter {
                try { LocalDate.parse(it.date) == selectedDate } catch (e: Exception) { false }
            }.sortedBy { it.time }

            if (selectedDate != null && selectedDayAppointments.isNotEmpty()) {
                selectedDayAppointments.forEach { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        doctorName = appointment.doctorName ?: doctors.find { it.id == appointment.doctorId }?.name ?: "",
                        onEdit = { onEdit(appointment) },
                        onDelete = { onDelete(appointment) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.all_appointments))
                Switch(
                    checked = showAllAppointments,
                    onCheckedChange = { showAllAppointments = it }
                )
            }

            val baseFilteredAppointments = appointments.filter {
                try {
                    val date = LocalDate.parse(it.date)
                    showAllAppointments || date >= today
                } catch (e: Exception) { false }
            }

            val groupedAppointments = baseFilteredAppointments.groupBy { it.date }
            val sortedDates = groupedAppointments.keys.sortedBy { it }

            sortedDates.forEach { date ->
                val readableDate = try {
                    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                } catch (e: Exception) {
                    date
                }

                Text(text = readableDate, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
                groupedAppointments[date]?.forEach { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        doctorName = appointment.doctorName ?: doctors.find { it.id == appointment.doctorId }?.name ?: "",
                        onEdit = { onEdit(appointment) },
                        onDelete = { onDelete(appointment) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}