@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.phms

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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


@OptIn(ExperimentalFoundationApi::class)
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

    LaunchedEffect(userId) {
        userId?.let { doctors = DoctorRepository.getDoctors(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.doctors)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentDoctor = null
                    showDoctorDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_doctor)
                )
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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

        if (showDoctorDialog) {
            DoctorDialog(
                doctor = currentDoctor,
                userId = userId ?: "",
                onSave = { doctor ->
                    scope.launch {
                        if (doctor.id == null) {
                            DoctorRepository.addDoctor(doctor)
                        } else {
                            DoctorRepository.updateDoctor(doctor)
                        }
                        userId?.let { doctors = DoctorRepository.getDoctors(it) }
                        showDoctorDialog = false
                    }
                },
                onCancel = { showDoctorDialog = false }
            )
        }

        if (confirmDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { confirmDeleteDialog = null },
                title = { Text(stringResource(R.string.confirm_delete)) },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_doctor_confirmation,
                            confirmDeleteDialog!!.name
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                confirmDeleteDialog?.id?.let {
                                    DoctorRepository.deleteDoctor(it)
                                    userId?.let { uid ->
                                        doctors = DoctorRepository.getDoctors(uid)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = doctor.name, style = MaterialTheme.typography.titleMedium)
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

            val ctx = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${doctor.phone}")))
                }) {
                    Icon(Icons.Default.Phone, contentDescription = "Call")
                }
                IconButton(onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${doctor.email}")))
                }) {
                    Icon(Icons.Default.Email, contentDescription = "Email")
                }
                IconButton(onClick = {
                    val q = Uri.encode(doctor.address)
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")))
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Map")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // unchanged: details rows
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = doctor.phone, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = doctor.email, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = doctor.address, style = MaterialTheme.typography.bodySmall)
            }

            if (!doctor.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text(text = doctor.notes, style = MaterialTheme.typography.bodySmall)
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
    var name by remember { mutableStateOf(doctor?.name.orEmpty()) }
    var specialization by remember { mutableStateOf(doctor?.specialization.orEmpty()) }
    var phone by remember { mutableStateOf(doctor?.phone.orEmpty()) }
    var email by remember { mutableStateOf(doctor?.email.orEmpty()) }
    var address by remember { mutableStateOf(doctor?.address.orEmpty()) }
    var notes by remember { mutableStateOf(doctor?.notes.orEmpty()) }

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
                // -- name --
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.doctor_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // -- specialization --
                OutlinedTextField(
                    value = specialization,
                    onValueChange = { specialization = it },
                    label = { Text(stringResource(R.string.specialization)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // -- phone --
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // -- email --
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // -- address --
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))

                // -- notes --
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
                    onSave(
                        Doctor(
                            id = doctor?.id,
                            userId = userId,
                            name = name,
                            specialization = specialization,
                            phone = phone,
                            email = email,
                            address = address,
                            notes = notes,
                            notifyOnEmergency = doctor?.notifyOnEmergency ?: false
                        )
                    )
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
