package com.example.phms.Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.phms.EmergencyContact
import com.example.phms.R
import com.example.phms.repository.EmergencyContactRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    userId: String,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var currentContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var confirmDeleteDialog by remember { mutableStateOf<EmergencyContact?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        isLoading = true
        contacts = EmergencyContactRepository.getContacts(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.emergency_contacts)) },
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
                    currentContact = null
                    showDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_contact))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            contacts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_emergency_contacts))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.tap_to_add_contact))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts) { contact ->
                        EmergencyContactCard(
                            contact = contact,
                            onEdit = {
                                currentContact = contact
                                showDialog = true
                            },
                            onDelete = {
                                confirmDeleteDialog = contact
                            }
                        )
                    }
                }
            }
        }
    }


    if (showDialog) {
        EmergencyContactDialog(
            contact = currentContact,
            userId = userId,
            onSave = { contact ->
                scope.launch {
                    if (contact.id == null) {
                        EmergencyContactRepository.addContact(contact)
                    } else {
                        EmergencyContactRepository.updateContact(contact)
                    }
                    contacts = EmergencyContactRepository.getContacts(userId)
                    showDialog = false
                }
            },
            onCancel = { showDialog = false }
        )
    }


    if (confirmDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.delete_contact_confirmation, confirmDeleteDialog!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            confirmDeleteDialog?.id?.let { id ->
                                if (EmergencyContactRepository.deleteContact(id)) {
                                    contacts = EmergencyContactRepository.getContacts(userId)
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
fun EmergencyContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = contact.name,
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
                text = contact.relationship,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = contact.email)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = contact.phone)
            }

            if (contact.notifyOnEmergency) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.receives_emergency_notifications))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactDialog(
    contact: EmergencyContact?,
    userId: String,
    onSave: (EmergencyContact) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var email by remember { mutableStateOf(contact?.email ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var notifyOnEmergency by remember { mutableStateOf(contact?.notifyOnEmergency ?: true) }

    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var relationshipError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = if (contact == null)
                    stringResource(R.string.add_contact)
                else
                    stringResource(R.string.edit_contact_title)
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
                    label = { Text(stringResource(R.string.name_label)) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(stringResource(R.string.required_field))
                    },
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
                        if (emailError) Text(stringResource(R.string.required_field))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

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
                                phoneError = context.getString(R.string.error_phone_required)
                            }
                            !filteredValue.matches(Regex("^\\+?\\d{10,15}$")) -> {
                                phoneError = context.getString(R.string.error_phone_invalid)
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

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = relationship,
                    onValueChange = {
                        relationship = it
                        relationshipError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.relationship)) },
                    isError = relationshipError,
                    supportingText = {
                        if (relationshipError) Text(stringResource(R.string.required_field))
                    },
                    modifier = Modifier.fillMaxWidth()
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
                    Text(stringResource(R.string.notify_on_emergency))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()
                    emailError = email.isBlank()

                    val isPhoneValid = phone.isNotBlank() && phone.matches(Regex("^\\+?\\d{10,15}$"))
                    if (!isPhoneValid && phoneError == null) {
                        phoneError = if (phone.isBlank()) context.getString(R.string.error_phone_required) else context.getString(R.string.error_phone_invalid)
                    } else if (isPhoneValid) {
                        phoneError = null
                    }

                    relationshipError = relationship.isBlank()

                    if (!nameError && !emailError && phoneError == null && !relationshipError) {
                        val updatedContact = EmergencyContact(
                            id = contact?.id,
                            userId = userId,
                            name = name,
                            email = email,
                            phone = phone,
                            relationship = relationship,
                            notifyOnEmergency = notifyOnEmergency
                        )
                        onSave(updatedContact)
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