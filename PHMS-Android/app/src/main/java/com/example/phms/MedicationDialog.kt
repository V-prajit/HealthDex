@file:OptIn(ExperimentalLayoutApi::class)

package com.example.phms

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDialog(
    initial: Medication?,
    userId: String,
    onSave: (Medication) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var dosage by remember { mutableStateOf(initial?.dosage ?: "") }
    var frequency by remember { mutableStateOf(initial?.frequency ?: "1") }
    var instructions by remember { mutableStateOf(initial?.instructions ?: "") }

    val categories = listOf("Cold & Flu", "Pain Relief", "Allergy", "Digestive", "Miscellaneous")
    var categoryExpanded by remember { mutableStateOf(false) }

    val dosageOptions = listOf("tsp", "tbsp", "ml", "pill", "capsule")
    var dosageExpanded by remember { mutableStateOf(false) }

    val frequencyOptions = listOf("1", "2", "3", "4")
    var frequencyExpanded by remember { mutableStateOf(false) }

    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember { mutableStateListOf<String>() }

    val initialTimes = initial?.time?.split(",") ?: listOf("09:00")
    val freqInt = frequency.toIntOrNull() ?: 1

    val timeList = remember {
        mutableStateListOf<String>().apply {
            if (initial != null && initial.time.isNotEmpty()) {
                // Handle both single time and comma-separated times
                if (initial.time.contains(",")) {
                    // Multiple times
                    addAll(initial.time.split(","))
                } else {
                    // Single time
                    add(initial.time)
                }

                // Adjust list size if needed based on frequency
                while (size < freqInt) add("09:00")
                while (size > freqInt) removeAt(lastIndex)
            } else {
                // No saved times, use defaults
                repeat(freqInt) { add("09:00") }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add Medication") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                category = it
                                categoryExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = dosageExpanded,
                    onExpandedChange = { dosageExpanded = !dosageExpanded }
                ) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dosage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dosageExpanded,
                        onDismissRequest = { dosageExpanded = false }
                    ) {
                        dosageOptions.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                dosage = it
                                dosageExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = !frequencyExpanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Times per day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false }
                    ) {
                        frequencyOptions.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = {
                                frequency = it
                                frequencyExpanded = false
                                val n = it.toIntOrNull() ?: 1
                                while (timeList.size < n) timeList.add("09:00")
                                while (timeList.size > n) timeList.removeAt(timeList.lastIndex)
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                timeList.forEachIndexed { i, time ->
                    val cal = Calendar.getInstance()
                    val hour = time.substringBefore(":").toIntOrNull() ?: 9
                    val minute = time.substringAfter(":").toIntOrNull() ?: 0

                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _: TimePicker, h: Int, m: Int ->
                                timeList[i] = String.format("%02d:%02d", h, m)
                            },
                            hour,
                            minute,
                            true
                        ).apply {
                            setTitle("Select Time for Dose ${i + 1}")
                        }.show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Time ${i + 1}: $time")
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Days to take")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekdays.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                if (day in selectedDays) selectedDays.remove(day)
                                else selectedDays.add(day)
                            },
                            label = { Text(day) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val timeString = timeList.joinToString(",")
                val med = Medication(
                    id = initial?.id,
                    userId = userId,
                    name = name,
                    category = category,
                    dosage = dosage,
                    frequency = frequency,
                    instructions = instructions,
                    time = timeString
                )
                onSave(med)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}