package com.example.phms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.phms.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsScreen(
    userId: String?,
    onBackClick: () -> Unit
) {
    // 1) Strings
    val vitalsLabel      = stringResource(R.string.vitals)
    val backLabel        = stringResource(R.string.back)
    val addVitalDesc     = stringResource(R.string.add_vital)
    val allLabel         = stringResource(R.string.all)
    val bpLabel          = stringResource(R.string.blood_pressure)
    val glucoseLabel     = stringResource(R.string.glucose)
    val cholLabel        = stringResource(R.string.cholesterol)
    val hrLabel          = stringResource(R.string.heart_rate)
    val filterLabel      = stringResource(R.string.filter_by_type_label)
    val mostRecentLabel  = stringResource(R.string.most_recent)
    val noEntriesLabel   = stringResource(R.string.no_entries_yet)
    val noRecentLabel    = stringResource(R.string.no_most_recent)
    val editDesc         = stringResource(R.string.edit_vital)
    val deleteDesc       = stringResource(R.string.delete)

    // 2) State
    val scope = rememberCoroutineScope()
    var vitals       by remember { mutableStateOf<List<VitalSign>>(emptyList()) }
    var showDlg      by remember { mutableStateOf(false) }
    var editing      by remember { mutableStateOf<VitalSign?>(null) }
    var selectedType by remember { mutableStateOf(allLabel) }
    var expanded     by remember { mutableStateOf(false) }
    var latestByType by remember { mutableStateOf<VitalSign?>(null) }

    // 3) Load
    LaunchedEffect(userId) {
        if (userId != null) vitals = VitalRepository.getVitals(userId)
    }
    LaunchedEffect(userId, selectedType) {
        latestByType = if (userId != null && selectedType != allLabel)
            VitalRepository.getLatestVital(userId, selectedType)
        else null
    }

    // 4) Filtered list
    val filteredVitals by remember(vitals, selectedType) {
        derivedStateOf {
            if (selectedType == allLabel) vitals
            else vitals.filter { it.type == selectedType }
        }
    }

    // 5) UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vitalsLabel) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backLabel)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDlg = true
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = addVitalDesc)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Filter row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(filterLabel, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(selectedType)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf(allLabel, bpLabel, glucoseLabel, cholLabel, hrLabel).forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Most recent (only for specific type)
            if (selectedType != allLabel) {
                if (latestByType != null) {
                    val v = latestByType!!
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.elevatedCardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(mostRecentLabel, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("${v.type}: ${v.value} ${v.unit}",
                                style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(2.dp))
                            Text(v.timestamp, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text(noRecentLabel, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Full filtered list
            if (filteredVitals.isEmpty()) {
                Text(noEntriesLabel, style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredVitals) { v ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.elevatedCardElevation(2.dp)
                        ) {
                            ListItem(
                                headlineContent   = { Text(v.type) },
                                supportingContent = { Text("${v.value} ${v.unit}") },
                                trailingContent   = {
                                    Row {
                                        IconButton(onClick = {
                                            editing = v
                                            showDlg = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = editDesc)
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                v.id?.let { id ->
                                                    if (VitalRepository.deleteVital(id))
                                                        vitals = VitalRepository.getVitals(userId!!)
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = deleteDesc)
                                        }
                                    }
                                }
                            )
                            Divider()
                            Text(
                                text = v.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showDlg) {
        VitalDialog(
            initial  = editing,
            userId   = userId,
            onSave   = { newV ->
                scope.launch {
                    if (newV.id == null) VitalRepository.addVital(newV)
                    else                  VitalRepository.updateVital(newV)
                    vitals = VitalRepository.getVitals(userId!!)
                    showDlg = false
                }
            },
            onCancel = { showDlg = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalDialog(
    initial: VitalSign?,
    userId: String?,
    onSave: (VitalSign) -> Unit,
    onCancel: () -> Unit
) {
    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    val bpLabel       = stringResource(R.string.blood_pressure)
    val glucoseLabel  = stringResource(R.string.glucose)
    val cholLabel     = stringResource(R.string.cholesterol)
    val hrLabel       = stringResource(R.string.heart_rate)
    val options       = listOf(bpLabel, glucoseLabel, cholLabel, hrLabel)

    var type         by remember { mutableStateOf(initial?.type ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }
    var valueText    by remember { mutableStateOf(initial?.value?.toString() ?: "") }
    var unit         by remember { mutableStateOf(initial?.unit ?: "") }
    var error        by remember { mutableStateOf("") }

    val saveLabel          = stringResource(R.string.save)
    val cancelLabel        = stringResource(R.string.cancel)
    val vitalInputErrorTxt = stringResource(R.string.vital_input_error)
    val dialogTitle        = stringResource(
        if (initial == null) R.string.add_vital else R.string.edit_vital
    )

    AlertDialog(
        onDismissRequest = onCancel,
        title            = { Text(dialogTitle) },
        text             = {
            Column {
                // Type selector
                Box {
                    OutlinedButton(
                        onClick = { typeExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (type.isBlank()) stringResource(R.string.select_type) else type)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    type = opt
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(stringResource(R.string.value_label)) },
                    isError = error.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.unit_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton   = {
            TextButton(onClick = {
                val dbl = valueText.toDoubleOrNull()
                if (type.isBlank() || dbl == null || unit.isBlank()) {
                    error = vitalInputErrorTxt
                } else {
                    val now = df.format(Date())
                    onSave(VitalSign(initial?.id, userId ?: "", type, dbl, unit, now))
                }
            }) {
                Text(saveLabel)
            }
        },
        dismissButton   = {
            TextButton(onClick = onCancel) {
                Text(cancelLabel)
            }
        }
    )
}
