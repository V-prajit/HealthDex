@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.phms.Screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.phms.Doctor
import com.example.phms.R
import com.example.phms.repository.DoctorRepository
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
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Row {
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
                    Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.call_doctor_desc))
                }
                IconButton(onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${doctor.email}")))
                }) {
                    Icon(Icons.Default.Email, contentDescription = stringResource(R.string.email_doctor_desc))
                }
                IconButton(onClick = {
                    val q = Uri.encode(doctor.address)
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")))
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.map_doctor_desc))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DoctorDialog(
    doctor: Doctor?,
    userId: String,
    onSave: (Doctor) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(doctor?.name.orEmpty()) }
    var phone by remember { mutableStateOf(doctor?.phone.orEmpty()) }
    var email by remember { mutableStateOf(doctor?.email.orEmpty()) }
    var address by remember { mutableStateOf(doctor?.address.orEmpty()) }
    var notes by remember { mutableStateOf(doctor?.notes.orEmpty()) }

    var nameError by remember { mutableStateOf(false) }
    var specializationError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var notifyOnEmergency by remember { mutableStateOf(doctor?.notifyOnEmergency ?: false) }

    val context = LocalContext.current
    val requiredFieldText = stringResource(R.string.required_field)
    val errorPhoneRequiredText = stringResource(R.string.error_phone_required)
    val errorPhoneInvalidText = stringResource(R.string.error_phone_invalid)
    val specOtherText = stringResource(R.string.other)
    val specifySpecializationText = stringResource(R.string.specify_specialization_label)
    val errorSpecifySpecializationText = stringResource(R.string.error_specify_specialization)
    val otherSpecFormatText = stringResource(R.string.other_spec_format)

    val specificSpecializations = listOf(
        stringResource(R.string.spec_allergist),
        stringResource(R.string.spec_cardiologist),
        stringResource(R.string.spec_dermatologist),
        stringResource(R.string.spec_endocrinologist),
        stringResource(R.string.spec_ent),
        stringResource(R.string.spec_gastroenterologist),
        stringResource(R.string.spec_gynecologist),
        stringResource(R.string.spec_neurologist),
        stringResource(R.string.spec_oncologist),
        stringResource(R.string.spec_ophthalmologist),
        stringResource(R.string.spec_orthopedist),
        stringResource(R.string.spec_pediatrician),
        stringResource(R.string.spec_primary_care),
        stringResource(R.string.spec_psychiatrist),
        specOtherText
    )

    var specializationExpanded by remember { mutableStateOf(false) }
    var specialization by remember { mutableStateOf(doctor?.specialization?.takeIf { it in specificSpecializations } ?: "") }
    var customSpecialization by remember {
        mutableStateOf(doctor?.specialization?.takeIf { it !in specificSpecializations.dropLast(1) } ?: "")
    }

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
                            Text(requiredFieldText)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = specializationExpanded,
                    onExpandedChange = { specializationExpanded = !specializationExpanded }
                ) {
                    OutlinedTextField(
                        value = when {
                            specialization == specOtherText && customSpecialization.isNotBlank() -> String.format(otherSpecFormatText, customSpecialization)
                            specialization == specOtherText -> specOtherText
                            else -> specialization
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.specialization)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationExpanded) },
                        isError = specializationError && (specialization.isBlank() || (specialization == specOtherText && customSpecialization.isBlank())),
                        supportingText = {
                            if (specializationError && (specialization.isBlank() || (specialization == specOtherText && customSpecialization.isBlank()))) {
                                Text(requiredFieldText)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = specializationExpanded,
                        onDismissRequest = { specializationExpanded = false }
                    ) {
                        specificSpecializations.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    specialization = selectionOption
                                    specializationExpanded = false
                                    if (selectionOption != specOtherText) {
                                        customSpecialization = ""
                                    }
                                    specializationError = selectionOption == specOtherText && customSpecialization.isBlank()
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (specialization == specOtherText) {
                    OutlinedTextField(
                        value = customSpecialization,
                        onValueChange = {
                            customSpecialization = it
                            specializationError = it.isBlank()
                        },
                        label = { Text(specifySpecializationText) },
                        isError = specializationError,
                        supportingText = {
                            if (specializationError) {
                                Text(errorSpecifySpecializationText)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }


                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        val filteredValue = newValue.filterIndexed { index, char ->
                            char.isDigit() || (index == 0 && char == '+')
                        }
                        phone = filteredValue
                        when {
                            filteredValue.isBlank() -> {
                                phoneError = errorPhoneRequiredText
                            }
                            !filteredValue.matches(Regex("^\\+?\\d{10,15}$")) -> {
                                phoneError = errorPhoneInvalidText
                            }
                            else -> {
                                phoneError = null
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.phone)) },
                    isError = phoneError != null,
                    supportingText = {
                        phoneError?.let { Text(it) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

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
                            Text(requiredFieldText)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

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
                            Text(requiredFieldText)
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = notifyOnEmergency,
                        onCheckedChange = { notifyOnEmergency = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.notify_on_vital_emergencies))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()

                    val finalSpecialization = if (specialization == specOtherText) customSpecialization.trim() else specialization
                    specializationError = finalSpecialization.isBlank()

                    val isPhoneValid = phone.isNotBlank() && phone.matches(Regex("^\\+?\\d{10,15}$"))
                    if (!isPhoneValid && phoneError == null) {
                        phoneError = if (phone.isBlank()) errorPhoneRequiredText else errorPhoneInvalidText
                    } else if (isPhoneValid) {
                        phoneError = null
                    }

                    emailError = email.isBlank()
                    addressError = address.isBlank()

                    if (!nameError && !specializationError && phoneError == null && !emailError && !addressError) {
                        val updatedDoctor = Doctor(
                            id = doctor?.id,
                            userId = userId,
                            name = name,
                            specialization = finalSpecialization,
                            phone = phone,
                            email = email,
                            address = address,
                            notes = notes,
                            notifyOnEmergency = notifyOnEmergency
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