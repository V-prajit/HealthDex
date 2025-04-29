package com.example.phms.Screens

import com.example.phms.R
import android.util.Log
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.phms.*
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    vitalSignsViewModel: VitalSignsViewModel = viewModel()
) {
    val vitalsLabel = stringResource(R.string.vitals)
    val backLabel = stringResource(R.string.back)
    val addVitalDesc = stringResource(R.string.add_vital)
    val allLabel = stringResource(R.string.all)
    val bpLabel = stringResource(R.string.blood_pressure)
    val glucoseLabel = stringResource(R.string.glucose)
    val cholLabel = stringResource(R.string.cholesterol)
    val otherLabel = stringResource(R.string.other)
    val filterLabel = stringResource(R.string.filter_by_type_label)
    val mostRecentLabel = stringResource(R.string.most_recent)
    val noEntriesLabel = stringResource(R.string.no_entries_yet)
    val noRecentLabel = stringResource(R.string.no_most_recent)
    val editDesc = stringResource(R.string.edit_vital)
    val deleteDesc = stringResource(R.string.delete)
    val settingsLabel = stringResource(R.string.settings)
    val realTimeDataLabel = stringResource(R.string.real_time_vitals_header)
    val manualEntriesLabel = stringResource(R.string.manual_entries_title)
    val hrChartTitle = stringResource(R.string.chart_hr_title)
    val glucoseChartTitle = stringResource(R.string.chart_glucose_title)
    val cholesterolChartTitle = stringResource(R.string.chart_cholesterol_title)
    val noVitalsDataLabel = stringResource(R.string.no_vitals_data)

    val scope = rememberCoroutineScope()
    var vitals by remember { mutableStateOf<List<VitalSign>>(emptyList()) }
    var showDlg by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VitalSign?>(null) }
    var selectedType by remember { mutableStateOf(allLabel) }
    var expanded by remember { mutableStateOf(false) }
    var latestByType by remember { mutableStateOf<VitalSign?>(null) }

    val vitalHistory by vitalSignsViewModel.vitalHistory.collectAsState()
    val thresholds by vitalSignsViewModel.thresholds.collectAsState()

    LaunchedEffect(userId) {
        if (userId != null) vitals = VitalRepository.getVitals(userId)
    }

    LaunchedEffect(userId, selectedType) {
        latestByType = if (
            userId != null &&
            selectedType != allLabel &&
            selectedType != otherLabel
        )
            VitalRepository.getLatestVital(userId, selectedType)
        else null
    }

    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    val heartRateData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.heartRate?.let { hr -> ChartDataPoint(it.timestampMs, hr) } }
    }

    val bpData = remember(vitalHistory) {
        vitalHistory.mapNotNull {
            if(it.bpSystolic != null && it.bpDiastolic != null)
                BPChartDataPoint(it.timestampMs, it.bpSystolic, it.bpDiastolic)
            else null
        }
    }

     val glucoseData = remember(vitals) {
         vitals.filter { it.type == glucoseLabel && it.value != null }
             .mapNotNull { v ->
                 df.parse(v.timestamp)?.time?.let { ChartDataPoint(it, v.value!!.toFloat()) }
             }
     }
     val cholData = remember(vitals) {
         vitals.filter { it.type == cholLabel && it.value != null }
             .mapNotNull { v ->
                 df.parse(v.timestamp)?.time?.let { ChartDataPoint(it, v.value!!.toFloat()) }
             }
     }

    val filteredVitals by remember(vitals, selectedType) {
        derivedStateOf {
            when (selectedType) {
                allLabel -> vitals
                otherLabel -> vitals.filter { vs ->
                    listOf(bpLabel, glucoseLabel, cholLabel).none { it == vs.type }
                }
                else -> vitals.filter { it.type == selectedType }
            }
        }
    }

    suspend fun sendAlertIfNeeded(v: VitalSign) {
        val (breachValue, threshold, isHigh) = when (v.type) {
            bpLabel -> {
                val sys = v.manualSystolic!!
                val dia = v.manualDiastolic!!
                when {
                    sys > thresholds.bpSysHigh -> Triple(sys, thresholds.bpSysHigh, true)
                    sys < thresholds.bpSysLow  -> Triple(sys, thresholds.bpSysLow, false)
                    dia > thresholds.bpDiaHigh -> Triple(dia, thresholds.bpDiaHigh, true)
                    dia < thresholds.bpDiaLow  -> Triple(dia, thresholds.bpDiaLow, false)
                    else -> return
                }
            }
            glucoseLabel -> {
                val g = v.value!!
                when {
                    g > thresholds.glucoseHigh -> Triple(g, thresholds.glucoseHigh, true)
                    g < thresholds.glucoseLow  -> Triple(g, thresholds.glucoseLow, false)
                    else -> return
                }
            }
            cholLabel -> {
                val c = v.value!!
                when {
                    c > thresholds.cholesterolHigh -> Triple(c, thresholds.cholesterolHigh, true)
                    c < thresholds.cholesterolLow  -> Triple(c, thresholds.cholesterolLow, false)
                    else -> return
                }
            }
            else -> return
        }

        try {
            val resp: Response<Map<String, Int>> = RetrofitClient.apiService.sendVitalAlert(
                VitalAlertRequest(
                    userId = userId ?: return,
                    vitalName = v.type,
                    value = breachValue.toFloat(),
                    threshold = threshold,
                    isHigh = isHigh
                )
            )
            if (!resp.isSuccessful)
                Log.e("VitalSignAlert", "Manual alert failed: ${resp.code()} - ${resp.message()}")
            else
                Log.i("VitalSignAlert", "Manual alert sent successfully for ${v.type}.")
        } catch (e: Exception) {
            Log.e("VitalSignAlert", "Manual alert error", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vitalsLabel) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, backLabel)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, settingsLabel)
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
                Icon(Icons.Default.Add, addVitalDesc)
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
                        title = hrChartTitle,
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
                        title = glucoseChartTitle,
                        data = glucoseData,
                        highThreshold = thresholds.glucoseHigh,
                        lowThreshold = thresholds.glucoseLow,
                        lineColor = ChartOrange
                    )

                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = cholesterolChartTitle,
                        data = cholData,
                        highThreshold = thresholds.cholesterolHigh,
                        lowThreshold = thresholds.cholesterolLow,
                        lineColor = ChartPurple
                    )
                }
            }

            item {
                Divider(Modifier.padding(vertical = 8.dp))
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
                            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(allLabel, bpLabel, glucoseLabel, cholLabel, otherLabel)
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
                    val msg = if (vitalHistory.isEmpty() && vitals.isEmpty())
                        noVitalsDataLabel
                    else
                        noEntriesLabel
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(filteredVitals, key = { it.id ?: UUID.randomUUID() }) { v ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        elevation = CardDefaults.elevatedCardElevation(2.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(v.type) },
                            supportingContent = {
                                val valueDisplay = if (v.type == bpLabel) {
                                    stringResource(R.string.bp_value_display, v.manualSystolic?.toInt() ?: 0, v.manualDiastolic?.toInt() ?: 0, v.unit)
                                } else {
                                    stringResource(R.string.vital_value_display, v.value ?: "", v.unit)
                                }
                                Text(valueDisplay)
                            },
                            trailingContent = {
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
                                                    latestByType =
                                                        if (selectedType != allLabel && selectedType != otherLabel)
                                                            VitalRepository.getLatestVital(userId, selectedType)
                                                        else null
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
                            v.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDlg) {
        VitalDialog(
            initial = editing,
            userId = userId,
            onSave = { newV ->
                scope.launch {
                    if (newV.id == null) VitalRepository.addVital(newV)
                    else VitalRepository.updateVital(newV)

                    vitals = VitalRepository.getVitals(userId!!)
                    latestByType =
                        if (selectedType != allLabel && selectedType != otherLabel)
                            VitalRepository.getLatestVital(userId, selectedType)
                        else null

                    sendAlertIfNeeded(newV)
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

    val saveLabel = stringResource(R.string.save)
    val cancelLabel = stringResource(R.string.cancel)
    val inputErrorTxt = stringResource(R.string.vital_input_error)
    val bpInputErrorTxt = stringResource(R.string.bp_input_error)
    val dialogTitle = stringResource(if (initial == null) R.string.add_vital else R.string.edit_vital)
    val selectTypeLabel = stringResource(R.string.select_type)
    val otherLabel = stringResource(R.string.other)
    val specifyTypeLabel = stringResource(R.string.specify_type)
    val selectUnitLabel = stringResource(R.string.select_unit)
    val specifyUnitLabel = stringResource(R.string.specify_unit)
    val systolicLabel = stringResource(R.string.systolic_value)
    val diastolicLabel = stringResource(R.string.diastolic_value)
    val valueLabel = stringResource(R.string.value_label)

    val bpLabel = stringResource(R.string.blood_pressure)
    val glucoseLabel = stringResource(R.string.glucose)
    val cholLabel = stringResource(R.string.cholesterol)
    val typeOptions = listOf(bpLabel, glucoseLabel, cholLabel, otherLabel)

    var typeExpanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(initial?.type ?: "") }
    var customType by remember { mutableStateOf(initial?.type?.takeIf { it !in typeOptions } ?: "") }

    val unitMap = mapOf(
        bpLabel to listOf("mmHg", "kPa"),
        glucoseLabel to listOf("mg/dL", "mmol/L"),
        cholLabel to listOf("mg/dL", "mmol/L")
    )

    var unitExpanded by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var customUnit by remember {
         mutableStateOf(initial?.unit?.takeIf { initial.type == otherLabel || it !in unitMap[initial.type].orEmpty() } ?: "")
    }

    var valueText by remember { mutableStateOf(initial?.takeIf { it.type != bpLabel }?.value?.toString() ?: "") }
    var systolicValueText by remember { mutableStateOf(initial?.takeIf { it.type == bpLabel }?.manualSystolic?.toString() ?: "") }
    var diastolicValueText by remember { mutableStateOf(initial?.takeIf { it.type == bpLabel }?.manualDiastolic?.toString() ?: "") }

    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(dialogTitle) },
        text = {
            Column {
                Box {
                    OutlinedButton(onClick = { typeExpanded = true }, Modifier.fillMaxWidth()) {
                        val txt = when {
                            type.isBlank() -> selectTypeLabel
                            type == otherLabel && customType.isNotBlank() -> customType
                            type == otherLabel -> "$otherLabel (${specifyTypeLabel.lowercase()})"
                            else -> type
                        }
                        Text(txt); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        typeOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                type = opt
                                typeExpanded = false
                                customType = ""
                                unit = ""
                                customUnit = ""
                                error = ""
                                if (opt == bpLabel) valueText = "" else { systolicValueText = ""; diastolicValueText = ""}
                            })
                        }
                    }
                }
                if (type == otherLabel) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it; error = "" },
                        label = { Text(specifyTypeLabel) },
                        isError = error.isNotEmpty() && customType.isBlank() && type == otherLabel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (type.isNotBlank()) {
                    if (type != otherLabel) {
                        val opts = unitMap[type] ?: emptyList()
                        Box {
                             OutlinedButton(onClick = { unitExpanded = true }, Modifier.fillMaxWidth()) {
                                 val txt = when {
                                     unit.isBlank() -> selectUnitLabel
                                     unit == otherLabel && customUnit.isNotBlank() -> customUnit
                                     unit == otherLabel -> "$otherLabel (${specifyUnitLabel.lowercase()})"
                                     else -> unit
                                 }
                                 Text(txt); Icon(Icons.Default.ArrowDropDown, null)
                             }
                             DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                 opts.plus(otherLabel).forEach { opt ->
                                     DropdownMenuItem(text = { Text(opt) }, onClick = {
                                         unit = opt
                                         unitExpanded = false
                                         if (opt != otherLabel) customUnit = ""
                                         error = ""
                                     })
                                 }
                             }
                         }
                         if (unit == otherLabel) {
                            Spacer(Modifier.height(8.dp))
                             OutlinedTextField(
                                 value = customUnit,
                                 onValueChange = { customUnit = it; error = "" },
                                 label = { Text(specifyUnitLabel) },
                                 isError = error.isNotEmpty() && customUnit.isBlank() && unit == otherLabel,
                                 modifier = Modifier.fillMaxWidth()
                             )
                         }
                    } else {
                        Spacer(Modifier.height(8.dp))
                         OutlinedTextField(
                             value = customUnit,
                             onValueChange = { customUnit = it; unit = otherLabel; error = "" },
                             label = { Text(specifyUnitLabel) },
                             isError = error.isNotEmpty() && customUnit.isBlank(),
                             modifier = Modifier.fillMaxWidth()
                         )
                    }
                }

                Spacer(Modifier.height(12.dp))

                 when (type) {
                     bpLabel -> {
                         OutlinedTextField(
                             value = systolicValueText,
                             onValueChange = { systolicValueText = it.filter { c -> c.isDigit() || c == '.' }; error = "" },
                             label = { Text(systolicLabel) },
                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                             singleLine = true,
                             isError = error.isNotEmpty() && systolicValueText.toDoubleOrNull() == null,
                             modifier = Modifier.fillMaxWidth()
                         )
                         Spacer(Modifier.height(8.dp))
                         OutlinedTextField(
                             value = diastolicValueText,
                             onValueChange = { diastolicValueText = it.filter { c -> c.isDigit() || c == '.' }; error = "" },
                             label = { Text(diastolicLabel) },
                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                             singleLine = true,
                             isError = error.isNotEmpty() && diastolicValueText.toDoubleOrNull() == null,
                             modifier = Modifier.fillMaxWidth()
                         )
                     }
                     glucoseLabel, cholLabel, otherLabel -> {
                         OutlinedTextField(
                             value = valueText,
                             onValueChange = { valueText = it.filter { c -> c.isDigit() || c == '.' }; error = "" },
                             label = { Text(valueLabel) },
                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                             singleLine = true,
                             isError = error.isNotEmpty() && valueText.toDoubleOrNull() == null,
                             modifier = Modifier.fillMaxWidth()
                         )
                     }
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
                val finalUnit = if (unit == otherLabel) customUnit.trim() else unit

                if (finalType.isBlank()) {
                    error = inputErrorTxt
                    return@TextButton
                }
                 if (finalUnit.isBlank() && type != otherLabel) {
                     if (type == bpLabel || type == glucoseLabel || type == cholLabel) {
                         error = inputErrorTxt
                         return@TextButton
                     }
                 } else if (finalUnit.isBlank() && type == otherLabel && customUnit.isBlank()) {
                      error = inputErrorTxt
                      return@TextButton
                 }

                val saved: VitalSign? = when (finalType) {
                    bpLabel -> {
                        val s = systolicValueText.toDoubleOrNull()
                        val d = diastolicValueText.toDoubleOrNull()
                        if (s == null || d == null) { error = bpInputErrorTxt; null }
                        else VitalSign(
                            id = initial?.id,
                            userId = userId ?: "",
                            type = bpLabel,
                            value = s,
                            unit = finalUnit.ifBlank { unitMap[bpLabel]?.firstOrNull() ?: "mmHg" },
                            timestamp = df.format(Date()),
                            manualSystolic = s,
                            manualDiastolic = d
                        )
                    }
                    glucoseLabel, cholLabel, otherLabel -> {
                        val v = valueText.toDoubleOrNull()
                        if (v == null) { error = inputErrorTxt; null }
                        else VitalSign(
                            id = initial?.id,
                            userId = userId ?: "",
                            type = finalType,
                            value = v,
                            unit = finalUnit.ifBlank { if (finalType == glucoseLabel || finalType == cholLabel) "mg/dL" else "" },
                            timestamp = df.format(Date())
                        )
                    }
                    else -> { error = inputErrorTxt; null }
                }

                saved?.let {
                    error = ""
                    onSave(it)
                }
            }) { Text(saveLabel) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(cancelLabel) } }
    )
}