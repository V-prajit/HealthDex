package com.example.phms.ui.features.emergency_contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.phms.data.model.EmergencyContact
import com.example.phms.domain.repository.EmergencyContactRepository
import com.example.phms.R
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
                        Text("No emergency contacts added")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to add an emergency contact")
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

    // Edit/Add Dialog
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

    // Delete Confirmation Dialog
    if (confirmDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text("Are you sure you want to delete ${confirmDeleteDialog!!.name}?") },
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
                    Text("Receives emergency notifications")
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
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var email by remember { mutableStateOf(contact?.email ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var notifyOnEmergency by remember { mutableStateOf(contact?.notifyOnEmergency ?: true) }

    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var relationshipError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = if (contact == null)
                    stringResource(R.string.add_contact)
                else
                    "Edit Contact"
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
                    label = { Text("Name") },
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
                    onValueChange = {
                        phone = it
                        phoneError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.phone)) },
                    isError = phoneError,
                    supportingText = {
                        if (phoneError) Text(stringResource(R.string.required_field))
                    },
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
                    phoneError = phone.isBlank()
                    relationshipError = relationship.isBlank()

                    if (!nameError && !emailError && !phoneError && !relationshipError) {
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