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
import androidx.compose.ui.res.stringResource
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

    val categories = listOf(
        stringResource(R.string.med_category_cold_flu),
        stringResource(R.string.med_category_pain),
        stringResource(R.string.med_category_allergy),
        stringResource(R.string.med_category_digestive),
        stringResource(R.string.med_category_misc)
    )
    var categoryExpanded by remember { mutableStateOf(false) }

    val dosageOptions = listOf("tsp", "tbsp", "ml", "pill", "capsule")
    var dosageExpanded by remember { mutableStateOf(false) }

    val frequencyOptions = listOf("1", "2", "3", "4")
    var frequencyExpanded by remember { mutableStateOf(false) }

    val weekdays = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )
    val selectedDays = remember { mutableStateListOf<String>() }

    val initialTimes = initial?.time?.split(",") ?: listOf("09:00")
    val freqInt = frequency.toIntOrNull() ?: 1

    val timeList = remember {
        mutableStateListOf<String>().apply {
            if (initial != null && initial.time.isNotEmpty()) {

                if (initial.time.contains(",")) {

                    addAll(initial.time.split(","))
                } else {

                    add(initial.time)
                }


                while (size < freqInt) add("09:00")
                while (size > freqInt) removeAt(lastIndex)
            } else {

                repeat(freqInt) { add("09:00") }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (initial == null) R.string.add_medication_title else R.string.edit_medication_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.med_name_label)) },
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
                        label = { Text(stringResource(R.string.med_category_label)) },
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
                        label = { Text(stringResource(R.string.med_dosage_label)) },
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
                        label = { Text(stringResource(R.string.med_frequency_label)) },
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
                            setTitle(context.getString(R.string.select_time_for_dose_title, i + 1))
                        }.show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.time_dose_display, i + 1, time))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.med_days_label))
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
                    label = { Text(stringResource(R.string.med_instructions_label)) },
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