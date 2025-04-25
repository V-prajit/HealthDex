package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    userToken: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var meds by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogInitial by remember { mutableStateOf<Medication?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // load meds
    LaunchedEffect(userToken) {
        userToken?.let { uid ->
            MedicationRepository.fetchAll(uid) { fetched ->
                meds = fetched.orEmpty()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Medications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                dialogInitial = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text("Loaded meds: ${meds.size}", modifier = Modifier.padding(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(meds) { med ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Name: ${med.name}", style = MaterialTheme.typography.titleMedium)
                                Text("Category: ${med.category}")
                                Text("Dosage: ${med.dosage}")
                                Text("Frequency: ${med.frequency}")
                                Text("Instructions: ${med.instructions}")
                            }
                            Row {
                                IconButton(onClick = {
                                    dialogInitial = med
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    med.id?.let { id ->
                                        // Save reference to medication before deletion
                                        val medicationToDelete = med

                                        MedicationRepository.delete(id) { success ->
                                            if (success) {
                                                meds = meds.filterNot { it.id == id }

                                                // Add this code for notification cancellation
                                                val alarmManager = MedicationAlarmManager(context)
                                                alarmManager.cancelMedicationReminders(medicationToDelete)
                                            }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            MedicationDialog(
                initial = dialogInitial,
                userId = userToken ?: return@Scaffold,
                onSave = { m ->
                    if (dialogInitial == null) {
                        Log.d("MedicationScreen", "Adding medication: ${m.name}, time: ${m.time}")
                        MedicationRepository.add(m) { saved ->
                            saved?.let {
                                Log.d("MedicationScreen", "Saved medication: ${it.name}, time: ${it.time}")
                                meds = listOf(it) + meds

                                // Add this code for notification scheduling
                                val alarmManager = MedicationAlarmManager(context)
                                alarmManager.scheduleMedicationReminders(it)
                            }
                            showDialog = false
                        }
                    } else {
                        Log.d("MedicationScreen", "Updating medication: ${m.name}, time: ${m.time}")
                        MedicationRepository.update(m) { success ->
                            if (success) {
                                Log.d("MedicationScreen", "Updated medication successfully")
                                meds = meds.map { if (it.id == m.id) m else it }

                                // Add this code for notification updating
                                val alarmManager = MedicationAlarmManager(context)
                                alarmManager.scheduleMedicationReminders(m)
                            }
                            showDialog = false
                        }
                    }
                },
                onCancel = {
                    showDialog = false
                }
            )
        }
    }
}
