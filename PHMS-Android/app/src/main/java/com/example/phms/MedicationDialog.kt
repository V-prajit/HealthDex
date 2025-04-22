package com.example.phms

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import java.time.Instant

@Composable
fun MedicationDialog(
    initial: Medication?,
    userId: String,
    onSave: (Medication) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var dosage by remember { mutableStateOf(initial?.dosage ?: "") }
    var frequency by remember { mutableStateOf(initial?.frequency ?: "") }
    var instructions by remember { mutableStateOf(initial?.instructions ?: "") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (initial == null) "Add Medication" else "Edit Medication") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage") })
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Frequency") })
                OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Instructions") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Medication(
                        id = initial?.id,
                        userId = userId,
                        name = name,
                        category = category,
                        dosage = dosage,
                        frequency = frequency,
                        instructions = instructions,
                        time = Instant.now().toString()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
