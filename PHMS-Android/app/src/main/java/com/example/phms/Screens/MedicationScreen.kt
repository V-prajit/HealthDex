package com.example.phms.Screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phms.Medication
import com.example.phms.MedicationAlarmManager
import com.example.phms.R
import com.example.phms.repository.MedicationRepository
import com.example.phms.ui.theme.PokemonClassicFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonMedicationsScreen(
    userToken: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var meds by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogInitial by remember { mutableStateOf<Medication?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Medication?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(userToken) {
        userToken?.let { uid ->
            MedicationRepository.fetchAll(uid) { fetched ->
                meds = fetched.orEmpty()
            }
        }
    }

    showDeleteConfirmation?.let { medication ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(stringResource(R.string.delete_medication_title)) },
            text = { Text(stringResource(R.string.delete_medication_confirmation, medication.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        medication.id?.let { id ->
                            MedicationRepository.delete(id) { success ->
                                if (success) {
                                    meds = meds.filterNot { it.id == id }
                                    val alarmManager = MedicationAlarmManager(context)
                                    alarmManager.cancelMedicationReminders(medication)
                                }
                            }
                        }
                        showDeleteConfirmation = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.medication_pouch_title),
                            fontFamily = PokemonClassicFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    dialogInitial = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_medication_desc))
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background
    ) {  innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (meds.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.medication_pouch_empty),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.tap_to_add_medication),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(meds) { med ->
                        val medicationThemeColors = when (med.category.lowercase()) {
                            stringResource(R.string.med_cat_cold_flu).lowercase() -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                            stringResource(R.string.med_cat_pain_relief).lowercase() -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            stringResource(R.string.med_cat_allergy).lowercase() -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                            stringResource(R.string.med_cat_digestive).lowercase() -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        PokemonMedicationCard(
                            medication = med,
                            cardColor = medicationThemeColors.first,
                            contentColor = medicationThemeColors.second,
                            onEdit = {
                                dialogInitial = med
                                showDialog = true
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showDialog) {
            ExtendedMedicationDialog(
                initial = dialogInitial,
                userId = userToken ?: return@Scaffold,
                onSave = { m ->
                    if (dialogInitial == null) {
                        MedicationRepository.add(m) { saved ->
                            saved?.let {
                                meds = listOf(it) + meds
                                val alarmManager = MedicationAlarmManager(context)
                                alarmManager.scheduleMedicationReminders(it)
                            }
                            showDialog = false
                        }
                    } else {
                        MedicationRepository.update(m) { success ->
                            if (success) {
                                meds = meds.map { if (it.id == m.id) m else it }
                                val alarmManager = MedicationAlarmManager(context)
                                alarmManager.scheduleMedicationReminders(m)
                            }
                            showDialog = false
                        }
                    }
                },
                onDelete = { medication ->
                    showDialog = false
                    showDeleteConfirmation = medication
                },
                onCancel = {
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun PokemonMedicationCard(
    medication: Medication,
    cardColor: Color,
    contentColor: Color,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(0.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
            .background(cardColor)
            .clickable { onEdit() }
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(0.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalPharmacy,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = medication.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = medication.category,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = medication.dosage.replace(Regex("[0-9]"), "").trim(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = stringResource(R.string.medication_frequency_display, medication.frequency),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ExtendedMedicationDialog(
    initial: Medication?,
    userId: String,
    onSave: (Medication) -> Unit,
    onDelete: (Medication) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var dosage by remember { mutableStateOf(initial?.dosage ?: "") }
    var frequency by remember { mutableStateOf(initial?.frequency ?: "1") }
    var instructions by remember { mutableStateOf(initial?.instructions ?: "") }

    val categories = listOf(stringResource(R.string.med_cat_cold_flu), stringResource(R.string.med_cat_pain_relief), stringResource(R.string.med_cat_allergy), stringResource(R.string.med_cat_digestive), stringResource(R.string.med_cat_misc))
    var categoryExpanded by remember { mutableStateOf(false) }

    val dosageOptions = listOf(stringResource(R.string.dosage_tsp), stringResource(R.string.dosage_tbsp), stringResource(R.string.dosage_ml), stringResource(R.string.dosage_pill), stringResource(R.string.dosage_capsule))
    var dosageExpanded by remember { mutableStateOf(false) }

    val frequencyOptions = listOf("1", "2", "3", "4")
    var frequencyExpanded by remember { mutableStateOf(false) }

    val weekdays = listOf(stringResource(R.string.day_mon), stringResource(R.string.day_tue), stringResource(R.string.day_wed), stringResource(R.string.day_thu), stringResource(R.string.day_fri), stringResource(R.string.day_sat), stringResource(R.string.day_sun))
    val selectedDays = remember { mutableStateListOf<String>() }

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

    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }
    var tempHour by remember { mutableStateOf(9) }
    var tempMinute by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (initial == null) stringResource(R.string.add_medication_title) else stringResource(R.string.edit_medication_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_label)) },
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
                        label = { Text(stringResource(R.string.category_label)) },
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
                        label = { Text(stringResource(R.string.dosage_label)) },
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
                        label = { Text(stringResource(R.string.times_per_day_label)) },
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
                    val currentHour = remember(time) { time.substringBefore(":").toIntOrNull() ?: 9 }
                    val currentMinute = remember(time) { time.substringAfter(":").toIntOrNull() ?: 0 }

                    OutlinedButton(
                        onClick = {
                            tempHour = currentHour
                            tempMinute = currentMinute
                            showTimePickerForIndex = i
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            stringResource(R.string.medication_time_button, i + 1, time),
                            fontFamily = PokemonClassicFontFamily
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.days_to_take_label))
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
                    label = { Text(stringResource(R.string.instructions_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                if (initial != null) {
                    Button(
                        onClick = { onDelete(initial) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    showTimePickerForIndex?.let { index ->
        ThemedTimePickerDialog(
            onDismissRequest = { showTimePickerForIndex = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeList[index] = String.format("%02d:%02d", tempHour, tempMinute)
                        showTimePickerForIndex = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.ok), fontFamily = PokemonClassicFontFamily)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePickerForIndex = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.cancel), fontFamily = PokemonClassicFontFamily)
                }
            }
        ) {
            ThemedTimePicker(
                initialHour = tempHour,
                initialMinute = tempMinute,
                onTimeChange = { hour, minute ->
                    tempHour = hour
                    tempMinute = minute
                }
            )
        }
    }
}