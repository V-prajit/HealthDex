package com.example.phms

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsScreen(
    userId: String?,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var showDoctorDialog by remember { mutableStateOf(false) }
    var currentDoctor by remember { mutableStateOf<Doctor?>(null) }
    var confirmDeleteDialog by remember { mutableStateOf<Doctor?>(null) }

    // Load doctors when screen is shown
    LaunchedEffect(userId) {
        if (userId != null) {
            doctors = DoctorRepository.getDoctors(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.doctors)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentDoctor = null // Ensure we're creating a new doctor
                    showDoctorDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_doctor))
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
            if (doctors.isEmpty()) {
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
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_doctors_yet),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tap_to_add_doctor),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // List of doctors
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(doctors) { doctor ->
                        DoctorCard(
                            doctor = doctor,
                            onEdit = {
                                currentDoctor = doctor
                                showDoctorDialog = true
                            },
                            onDelete = {
                                confirmDeleteDialog = doctor
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit/Add Doctor Dialog
    if (showDoctorDialog) {
        DoctorDialog(
            doctor = currentDoctor,
            userId = userId ?: "",
            onSave = { doctor ->
                scope.launch {
                    if (doctor.id == null) {
                        // Add new doctor
                        DoctorRepository.addDoctor(doctor)
                    } else {
                        // Update existing doctor
                        DoctorRepository.updateDoctor(doctor)
                    }
                    // Refresh doctor list
                    if (userId != null) {
                        doctors = DoctorRepository.getDoctors(userId)
                    }
                    showDoctorDialog = false
                }
            },
            onCancel = {
                showDoctorDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    if (confirmDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.delete_doctor_confirmation, confirmDeleteDialog!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val doctorToDelete = confirmDeleteDialog
                            if (doctorToDelete?.id != null) {
                                DoctorRepository.deleteDoctor(doctorToDelete.id)
                                // Refresh doctor list
                                if (userId != null) {
                                    doctors = DoctorRepository.getDoctors(userId)
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
fun DoctorCard(
    doctor: Doctor,
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
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }

            Text(
                text = doctor.specialization,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = doctor.phone,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = doctor.email,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = doctor.address,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!doctor.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = doctor.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDialog(
    doctor: Doctor?,
    userId: String,
    onSave: (Doctor) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(doctor?.name ?: "") }
    var specialization by remember { mutableStateOf(doctor?.specialization ?: "") }
    var phone by remember { mutableStateOf(doctor?.phone ?: "") }
    var email by remember { mutableStateOf(doctor?.email ?: "") }
    var address by remember { mutableStateOf(doctor?.address ?: "") }
    var notes by remember { mutableStateOf(doctor?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var specializationError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = if (doctor == null)
                    stringResource(R.string.add_doctor)
                else
                    stringResource(R.string.edit_doctor)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.doctor_name)) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text(stringResource(R.string.required_field))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = specialization,
                    onValueChange = {
                        specialization = it
                        specializationError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.specialization)) },
                    isError = specializationError,
                    supportingText = {
                        if (specializationError) {
                            Text(stringResource(R.string.required_field))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.phone)) },
                    isError = phoneError,
                    supportingText = {
                        if (phoneError) {
                            Text(stringResource(R.string.required_field))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.email)) },
                    isError = emailError,
                    supportingText = {
                        if (emailError) {
                            Text(stringResource(R.string.required_field))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.address)) },
                    isError = addressError,
                    supportingText = {
                        if (addressError) {
                            Text(stringResource(R.string.required_field))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()
                    specializationError = specialization.isBlank()
                    phoneError = phone.isBlank()
                    emailError = email.isBlank()
                    addressError = address.isBlank()

                    if (!nameError && !specializationError && !phoneError && !emailError && !addressError) {
                        val updatedDoctor = Doctor(
                            id = doctor?.id,
                            userId = userId,
                            name = name,
                            specialization = specialization,
                            phone = phone,
                            email = email,
                            address = address,
                            notes = notes
                        )
                        onSave(updatedDoctor)
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
}