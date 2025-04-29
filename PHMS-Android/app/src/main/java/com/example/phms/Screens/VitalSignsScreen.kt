package com.example.phms.Screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.phms.BPChartDataPoint
import com.example.phms.BloodPressureChart
import com.example.phms.ChartDataPoint
import com.example.phms.ChartOrange
import com.example.phms.ChartPurple
import com.example.phms.ChartRed
import com.example.phms.R
import com.example.phms.SimpleLineChart
import com.example.phms.VitalRepository
import com.example.phms.VitalSign
import com.example.phms.VitalSignsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    vitalSignsViewModel: VitalSignsViewModel = viewModel()
) {

    val vitalsLabel     = stringResource(R.string.vitals)
    val backLabel       = stringResource(R.string.back)
    val addVitalDesc    = stringResource(R.string.add_vital)
    val allLabel        = stringResource(R.string.all)
    val bpLabel         = stringResource(R.string.blood_pressure)
    val glucoseLabel    = stringResource(R.string.glucose)
    val cholLabel       = stringResource(R.string.cholesterol)
    val hrLabel         = stringResource(R.string.heart_rate)
    val otherLabel      = stringResource(R.string.other)
    val filterLabel     = stringResource(R.string.filter_by_type_label)
    val mostRecentLabel = stringResource(R.string.most_recent)
    val noEntriesLabel  = stringResource(R.string.no_entries_yet)
    val noRecentLabel   = stringResource(R.string.no_most_recent)
    val editDesc        = stringResource(R.string.edit_vital)
    val deleteDesc      = stringResource(R.string.delete)
    val settingsLabel   = stringResource(R.string.settings)

    val realTimeDataLabel = stringResource(R.string.real_time_vitals_header)
    val manualEntriesLabel = stringResource(R.string.manual_entries_title)


    val scope = rememberCoroutineScope()
    var vitals       by remember { mutableStateOf<List<VitalSign>>(emptyList()) }
    var showDlg      by remember { mutableStateOf(false) }
    var editing      by remember { mutableStateOf<VitalSign?>(null) }
    var selectedType by remember { mutableStateOf(allLabel) }
    var expanded     by remember { mutableStateOf(false) }
    var latestByType by remember { mutableStateOf<VitalSign?>(null) }


    val vitalHistory by vitalSignsViewModel.vitalHistory.collectAsState()
    val thresholds by vitalSignsViewModel.thresholds.collectAsState()



    LaunchedEffect(userId) {
        if (userId != null) vitals = VitalRepository.getVitals(userId)
    }

    LaunchedEffect(userId, selectedType) {
        latestByType = if (userId != null && selectedType != allLabel && selectedType != otherLabel)
            VitalRepository.getLatestVital(userId, selectedType)
        else null
    }


    val heartRateData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.heartRate?.let { hr -> ChartDataPoint(it.timestampMs, hr) } }
    }
    val glucoseData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.glucose?.let { g -> ChartDataPoint(it.timestampMs, g) } }
    }
    val cholesterolData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.cholesterol?.let { c -> ChartDataPoint(it.timestampMs, c) } }
    }
    val bpData = remember(vitalHistory) {
        vitalHistory.mapNotNull {
            if(it.bpSystolic != null && it.bpDiastolic != null)
                BPChartDataPoint(it.timestampMs, it.bpSystolic, it.bpDiastolic)
            else null
        }
    }



    val filteredVitals by remember(vitals, selectedType) {
        derivedStateOf {
            when (selectedType) {
                allLabel -> vitals

                otherLabel -> vitals.filter { vs ->
                    listOf(bpLabel, glucoseLabel, cholLabel, hrLabel)
                        .none { t -> t == vs.type }
                }

                else -> vitals.filter { vs -> vs.type == selectedType }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vitalsLabel) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backLabel)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = settingsLabel
                        )
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

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            item {
                Column {

                    Text(realTimeDataLabel, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))


                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.chart_hr_title),
                        data = heartRateData,
                        highThreshold = thresholds.hrHigh,
                        lowThreshold = thresholds.hrLow,
                        lineColor = ChartRed
                    )


                    BloodPressureChart(
                        modifier = Modifier.fillMaxWidth(),
                        data = bpData,
                        sysHighThreshold = thresholds.bpSysHigh,
                        sysLowThreshold = thresholds.bpSysLow,
                        diaHighThreshold = thresholds.bpDiaHigh,
                        diaLowThreshold = thresholds.bpDiaLow
                    )



                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.chart_glucose_title),
                        data = glucoseData,
                        highThreshold = thresholds.glucoseHigh,
                        lowThreshold = thresholds.glucoseLow,
                        lineColor = ChartOrange
                    )


                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.chart_cholesterol_title),
                        data = cholesterolData,
                        highThreshold = thresholds.cholesterolHigh,
                        lowThreshold = thresholds.cholesterolLow,
                        lineColor = ChartPurple
                    )
                }
            }


            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(manualEntriesLabel, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
            }


            item {
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
                            listOf(allLabel, bpLabel, glucoseLabel, cholLabel, hrLabel, otherLabel)
                                .forEach { type ->
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
            }



            if (selectedType != allLabel && selectedType != otherLabel) {
                item {
                    latestByType?.let { v ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            elevation = CardDefaults.elevatedCardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(mostRecentLabel, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))

                                val valueDisplay =
                                    if (v.type == bpLabel)
                                        stringResource(R.string.bp_value_display, v.manualSystolic?.toInt() ?: 0, v.manualDiastolic?.toInt() ?: 0, v.unit)
                                    else
                                        stringResource(R.string.vital_value_display, v.value ?: "", v.unit)
                                Text(stringResource(R.string.vital_display, v.type, valueDisplay), style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(2.dp))
                                Text(v.timestamp, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } ?: Text(noRecentLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }


            if (filteredVitals.isEmpty()) {
                item {
                    if(vitalHistory.isEmpty()) {
                        Text(stringResource(R.string.no_vitals_data), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(noEntriesLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(filteredVitals, key = { it.id ?: UUID.randomUUID() }) { v ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        elevation = CardDefaults.elevatedCardElevation(2.dp)
                    ) {
                        ListItem(
                            headlineContent   = { Text(v.type) },
                            supportingContent = {

                                val valueDisplay = if (v.type == bpLabel) {
                                    stringResource(R.string.bp_value_display, v.manualSystolic?.toInt() ?: 0, v.manualDiastolic?.toInt() ?: 0, v.unit)
                                } else {
                                    stringResource(R.string.vital_value_display, v.value ?: "", v.unit)
                                }
                                Text(valueDisplay)
                            },
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
                                                if (VitalRepository.deleteVital(id)) {
                                                    vitals = VitalRepository.getVitals(userId!!)
                                                }
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
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
                        )
                    }
                }
            }


            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }


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


    val saveLabel        = stringResource(R.string.save)
    val cancelLabel      = stringResource(R.string.cancel)
    val inputErrorTxt    = stringResource(R.string.vital_input_error)
    val bpInputErrorTxt  = stringResource(id = R.string.bp_input_error)
    val dialogTitle      = stringResource(
        if (initial == null) R.string.add_vital else R.string.edit_vital
    )
    val selectTypeLabel  = stringResource(R.string.select_type)
    val otherLabel       = stringResource(R.string.other)
    val specifyTypeLabel = stringResource(R.string.specify_type)
    val selectUnitLabel  = stringResource(R.string.select_unit)
    val specifyUnitLabel = stringResource(R.string.specify_unit)
    val systolicLabel    = stringResource(id = R.string.systolic_value)
    val diastolicLabel   = stringResource(id = R.string.diastolic_value)
    val valueLabel       = stringResource(R.string.value_label)



    val bpLabel       = stringResource(R.string.blood_pressure)
    val glucoseLabel  = stringResource(R.string.glucose)
    val cholLabel     = stringResource(R.string.cholesterol)
    val hrLabel       = stringResource(R.string.heart_rate)
    val typeOptions   = listOf(bpLabel, glucoseLabel, cholLabel, hrLabel, otherLabel)

    var typeExpanded  by remember { mutableStateOf(false) }
    var type          by remember { mutableStateOf(initial?.type ?: "") }
    var customType    by remember { mutableStateOf(initial?.type?.takeIf { it !in typeOptions } ?: "") }


    val unitMap = mapOf(
        bpLabel      to listOf("mmHg","kPa","cmH₂O","inHg"),
        glucoseLabel to listOf("mg/dL","mmol/L","mg%","g/L"),
        cholLabel    to listOf("mg/dL","mmol/L","mg%","g/L"),
        hrLabel      to listOf("bpm","bps","Hz","cpm")
    )
    var unitExpanded by remember { mutableStateOf(false) }
    var unit         by remember { mutableStateOf(initial?.unit ?: "") }
    var customUnit   by remember { mutableStateOf(initial?.unit?.takeIf { it !in unitMap[type].orEmpty() } ?: "") }


    var valueText by remember(initial) {
        mutableStateOf(
            if (initial != null && initial.type != bpLabel)
                initial.value?.toString() ?: ""
            else ""
        )
    }

    var systolicValueText by remember(initial) {
        mutableStateOf(
            if (initial?.type == bpLabel)
                initial.manualSystolic?.toString() ?: ""
            else ""
        )
    }

    var diastolicValueText by remember(initial) {
        mutableStateOf(
            if (initial?.type == bpLabel)
                initial.manualDiastolic?.toString() ?: ""
            else ""
        )
    }

    var error        by remember { mutableStateOf("") }


    LaunchedEffect(initial, type) {
        if (initial != null) {

            type = initial.type
            unit = initial.unit
            if (initial.type == bpLabel) {
                systolicValueText  = initial.manualSystolic?.toString() ?: ""
                diastolicValueText = initial.manualDiastolic?.toString() ?: ""
                valueText = ""
            } else {
                valueText = initial.value?.toString() ?: ""
                systolicValueText = ""
                diastolicValueText = ""
            }
            customType = initial.type.takeIf { it !in typeOptions } ?: ""
            customUnit = initial.unit.takeIf { it !in unitMap[type].orEmpty() } ?: ""
        } else {

            if (type == bpLabel) {
                valueText = ""

                if(unit.isBlank()) unit = unitMap[bpLabel]?.firstOrNull() ?: ""
            } else {
                systolicValueText = ""
                diastolicValueText = ""
            }
        }
        error = ""
    }


    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title            = { Text(dialogTitle) },
        text             = {
            Column {

                Box {
                    OutlinedButton(
                        onClick = { typeExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val display = when {
                            type.isBlank()                  -> selectTypeLabel
                            type == otherLabel && customType.isNotBlank() -> customType
                            else                            -> type
                        }
                        Text(display)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                typeExpanded = false
                                type = opt
                                if (opt == otherLabel) customType = "" else customType = ""

                                if(opt != bpLabel && opt != otherLabel) unit = "" else if (opt == bpLabel && unit.isBlank()) unit = unitMap[bpLabel]?.firstOrNull() ?: ""
                                customUnit = ""
                            })
                        }
                    }
                }

                if (type == otherLabel) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it },
                        label = { Text(specifyTypeLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))


                if (type.isBlank()) {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(selectUnitLabel)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                } else if (type != otherLabel) {
                    Box {
                        OutlinedButton(onClick = { unitExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            val displayUnit = when {
                                unit.isNotBlank() && unit != otherLabel -> unit
                                unit == otherLabel && customUnit.isNotBlank() -> customUnit
                                unit == otherLabel -> specifyUnitLabel
                                else -> selectUnitLabel
                            }
                            Text(displayUnit)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            (unitMap[type] ?: emptyList()).plus(otherLabel).forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = {
                                    unitExpanded = false
                                    unit = opt
                                    if (opt == otherLabel) customUnit = "" else customUnit = ""
                                })
                            }
                        }
                    }
                    if (unit == otherLabel) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customUnit,
                            onValueChange = { customUnit = it },
                            label = { Text(specifyUnitLabel) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUnit,
                        onValueChange = { customUnit = it; unit = otherLabel },
                        label = { Text(specifyUnitLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))


                if (type == bpLabel) {
                    OutlinedTextField(
                        value = systolicValueText,
                        onValueChange = { systolicValueText = it ; error = "" },
                        label = { Text(systolicLabel) },
                        isError = error.isNotEmpty(),

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = diastolicValueText,
                        onValueChange = { diastolicValueText = it ; error = "" },
                        label = { Text(diastolicLabel) },
                        isError = error.isNotEmpty(),

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (type.isNotBlank()) {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it ; error = "" },
                        label = { Text(valueLabel) },
                        isError = error.isNotEmpty(),

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val finalType = if (type == otherLabel) customType.trim() else type
                val finalUnit = when {
                    type == otherLabel            -> customUnit.trim()
                    unit == otherLabel            -> customUnit.trim()
                    type == bpLabel && unit.isBlank() -> unitMap[bpLabel]?.firstOrNull() ?: ""
                    unit.isBlank() -> ""
                    else                          -> unit
                }
                val now = df.format(Date())
                var vitalToSave: VitalSign? = null

                if (finalType.isBlank() || finalUnit.isBlank()) {
                    error = inputErrorTxt
                } else if (type == bpLabel) {
                    val systolicDbl = systolicValueText.toDoubleOrNull()
                    val diastolicDbl = diastolicValueText.toDoubleOrNull()

                    if (systolicDbl == null || diastolicDbl == null) {
                        error = bpInputErrorTxt
                    } else {
                        vitalToSave = VitalSign(
                            id              = initial?.id,
                            userId          = userId ?: "",
                            type            = finalType,
                            value           = systolicDbl,
                            unit            = finalUnit,
                            timestamp       = now,
                            manualSystolic  = systolicDbl,
                            manualDiastolic = diastolicDbl
                        )
                    }
                } else {
                    val dbl = valueText.toDoubleOrNull()
                    if (dbl == null) {
                        error = inputErrorTxt
                    } else {
                        vitalToSave = VitalSign(
                            id              = initial?.id,
                            userId          = userId ?: "",
                            type            = finalType,
                            value           = dbl,
                            unit            = finalUnit,
                            timestamp       = now
                        )
                    }
                }

                vitalToSave?.let { onSave(it) }

            }) {
                Text(saveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(cancelLabel)
            }
        }
    )
}