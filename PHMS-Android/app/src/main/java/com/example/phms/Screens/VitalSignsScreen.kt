package com.example.phms.Screens

import com.example.phms.R
import android.util.Log
import androidx.compose.foundation.layout.*
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
import com.example.phms.*      // RetrofitClient, VitalAlertRequest, charts, etc.
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    vitalSignsViewModel: VitalSignsViewModel = viewModel()
) {
    /* ---------------- Strings ---------------- */
    val vitalsLabel       = stringResource(R.string.vitals)
    val backLabel         = stringResource(R.string.back)
    val addVitalDesc      = stringResource(R.string.add_vital)
    val allLabel          = stringResource(R.string.all)
    val bpLabel           = stringResource(R.string.blood_pressure)
    val glucoseLabel      = stringResource(R.string.glucose)
    val cholLabel         = stringResource(R.string.cholesterol)
    val otherLabel        = stringResource(R.string.other)
    val filterLabel       = stringResource(R.string.filter_by_type_label)
    val mostRecentLabel   = stringResource(R.string.most_recent)
    val noEntriesLabel    = stringResource(R.string.no_entries_yet)
    val noRecentLabel     = stringResource(R.string.no_most_recent)
    val editDesc          = stringResource(R.string.edit_vital)
    val deleteDesc        = stringResource(R.string.delete)
    val settingsLabel     = stringResource(R.string.settings)
    val realTimeDataLabel = stringResource(R.string.real_time_vitals_header)
    val manualEntriesLabel = "Manual Entries"

    /* ---------------- State ---------------- */
    val scope = rememberCoroutineScope()
    var vitals       by remember { mutableStateOf<List<VitalSign>>(emptyList()) }
    var showDlg      by remember { mutableStateOf(false) }
    var editing      by remember { mutableStateOf<VitalSign?>(null) }
    var selectedType by remember { mutableStateOf(allLabel) }
    var expanded     by remember { mutableStateOf(false) }
    var latestByType by remember { mutableStateOf<VitalSign?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    /* ---------------- View-Model flows ---------------- */
    val vitalHistory  by vitalSignsViewModel.vitalHistory.collectAsState()
    val thresholds    by vitalSignsViewModel.thresholds.collectAsState()

    /* ---------------- Load manual entries ---------------- */
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

    /* ---------------- Chart data ---------------- */
    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    val heartRateData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.heartRate?.let { hr -> ChartDataPoint(it.timestampMs, hr) } }
    }

    val bpData = remember(vitals) {
        vitals.filter { it.type == bpLabel && it.manualSystolic != null && it.manualDiastolic != null }
            .mapNotNull { v ->
                df.parse(v.timestamp)?.time?.let {
                    BPChartDataPoint(it, v.manualSystolic!!.toFloat(), v.manualDiastolic!!.toFloat())
                }
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

    /* ---------------- Filtered manual list ---------------- */
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

    /* ---------------- helper: alert on manual save ---------------- */
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
                Log.e("MANUAL_ALERT", "Alert failed ${resp.code()}")
        } catch (e: Exception) {
            Log.e("MANUAL_ALERT", "Alert error", e)
        }
    }

    /* **************************************************************
                               UI
       ************************************************************** */
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                onClick = { editing = null; showDlg = true },
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
            /* ----- Real-time section ----- */
            item {
                Column {
                    Text(realTimeDataLabel, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))

                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Heart Rate (bpm)",
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
                        title = "Glucose (mg/dL)",
                        data = glucoseData,
                        highThreshold = thresholds.glucoseHigh,
                        lowThreshold = thresholds.glucoseLow,
                        lineColor = ChartOrange
                    )

                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Total Cholesterol (mg/dL)",
                        data = cholData,
                        highThreshold = thresholds.cholesterolHigh,
                        lowThreshold = thresholds.cholesterolLow,
                        lineColor = ChartPurple
                    )
                }
            }

            /* ----- Manual entries header ----- */
            item {
                Divider(Modifier.padding(vertical = 8.dp))
                Text(manualEntriesLabel, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
            }

            /* ----- Filter row (HR removed) ----- */
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

            /* ----- Most-recent card ----- */
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
                                val display = if (v.type == bpLabel)
                                    "${v.manualSystolic?.toInt()}/${v.manualDiastolic?.toInt()} ${v.unit}"
                                else
                                    "${v.value ?: ""} ${v.unit}"
                                Text("${v.type}: $display", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(2.dp))
                                Text(v.timestamp, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } ?: Text(noRecentLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }

            /* ----- Full manual list ----- */
            if (filteredVitals.isEmpty()) {
                item {
                    val msg = if (vitalHistory.isEmpty() && vitals.isEmpty())
                        "No vital signs data available."
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
                                val d = if (v.type == bpLabel)
                                    "${v.manualSystolic?.toInt()}/${v.manualDiastolic?.toInt()} ${v.unit}"
                                else
                                    "${v.value ?: ""} ${v.unit}"
                                Text(d)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editing = v; showDlg = true }) {
                                        Icon(Icons.Default.Edit, editDesc)
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
                                        Icon(Icons.Default.Delete, deleteDesc)
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

            item { Spacer(Modifier.height(80.dp)) } // bottom padding
        }
    }

    /* ---------------- Add / Edit dialog ---------------- */
    if (showDlg) {
        VitalDialog(
            initial = editing,
            userId  = userId,
            onSave = { newV ->
                scope.launch {
                    // 1) Save or update the entry
                    if (newV.id == null) VitalRepository.addVital(newV)
                    else                 VitalRepository.updateVital(newV)

                    // 2) Refresh your lists
                    vitals = VitalRepository.getVitals(userId!!)
                    latestByType = if (selectedType != allLabel && selectedType != otherLabel)
                                    VitalRepository.getLatestVital(userId, selectedType)
                                else null

                    // 3) Pre‐compute whether it should trigger an alert
                    val shouldAlert = when (newV.type) {
                    bpLabel -> {
                        newV.manualSystolic!!  > thresholds.bpSysHigh ||
                        newV.manualSystolic!!  < thresholds.bpSysLow  ||
                        newV.manualDiastolic!! > thresholds.bpDiaHigh ||
                        newV.manualDiastolic!! < thresholds.bpDiaLow
                    }
                    glucoseLabel -> {
                        newV.value!! > thresholds.glucoseHigh ||
                        newV.value!! < thresholds.glucoseLow
                    }
                    cholLabel -> {
                        newV.value!! > thresholds.cholesterolHigh ||
                        newV.value!! < thresholds.cholesterolLow
                    }
                    else -> false
                    }

                    // 4) Send the email alert (Unit) and close the dialog
                    sendAlertIfNeeded(newV)
                    showDlg = false

                    // 5) Show snackbar if it breached
                    if (shouldAlert) {
                    snackbarHostState.showSnackbar("An emergency alert was sent")
                    }
                }
            },
            onCancel = { showDlg = false }
        )
    }
}

/* ******************************************************************
   VitalDialog  —  HR option removed
   ****************************************************************** */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalDialog(
    initial: VitalSign?,
    userId: String?,
    onSave: (VitalSign) -> Unit,
    onCancel: () -> Unit
) {
    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    /* ---- strings ---- */
    val saveLabel   = stringResource(R.string.save)
    val cancelLabel = stringResource(R.string.cancel)
    val inputErr    = stringResource(R.string.vital_input_error)
    val bpErr       = stringResource(R.string.bp_input_error)

    val dialogTitle     = stringResource(if (initial == null) R.string.add_vital else R.string.edit_vital)
    val selectTypeLabel = stringResource(R.string.select_type)
    val otherLabel      = stringResource(R.string.other)
    val specifyType     = stringResource(R.string.specify_type)
    val selectUnit      = stringResource(R.string.select_unit)
    val specifyUnit     = stringResource(R.string.specify_unit)
    val systolicLabel   = stringResource(R.string.systolic_value)
    val diastolicLabel  = stringResource(R.string.diastolic_value)
    val valueLabel      = stringResource(R.string.value_label)

    /* ---- local constants ---- */
    val bpLabel      = stringResource(R.string.blood_pressure)
    val glucoseLabel = stringResource(R.string.glucose)
    val cholLabel    = stringResource(R.string.cholesterol)

    val typeOptions = listOf(bpLabel, glucoseLabel, cholLabel, otherLabel)

    /* ---- state ---- */
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

    var valueText by remember { mutableStateOf(if (initial?.type != bpLabel) initial?.value?.toString() ?: "" else "") }
    var sysText   by remember { mutableStateOf(if (initial?.type == bpLabel) initial.manualSystolic?.toString() ?: "" else "") }
    var diaText   by remember { mutableStateOf(if (initial?.type == bpLabel) initial.manualDiastolic?.toString() ?: "" else "") }

    var error by remember { mutableStateOf("") }

    /* ---- UI ---- */
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(dialogTitle) },
        text = {
            Column {
                /* type selector */
                Box {
                    OutlinedButton(onClick = { typeExpanded = true }, Modifier.fillMaxWidth()) {
                        val txt = when {
                            type.isBlank() -> selectTypeLabel
                            type == otherLabel && customType.isNotBlank() -> customType
                            type == otherLabel -> "$otherLabel (${specifyType.lowercase()})"
                            else -> type
                        }
                        Text(txt); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        typeOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                type = opt; typeExpanded = false; customType = ""; unit = ""; customUnit = ""; error = ""
                            })
                        }
                    }
                }
                if (type == otherLabel) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        customType,
                        { customType = it },
                        label = { Text(specifyType) },
                        isError = error.isNotEmpty() && customType.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                /* unit selector */
                if (type.isNotBlank()) {
                    if (type != otherLabel) {
                        val opts = unitMap[type] ?: emptyList()
                        Box {
                            OutlinedButton(onClick = { unitExpanded = true }, Modifier.fillMaxWidth()) {
                                val txt = when {
                                    unit.isBlank() -> selectUnit
                                    unit == otherLabel && customUnit.isNotBlank() -> customUnit
                                    unit == otherLabel -> "$otherLabel (${specifyUnit.lowercase()})"
                                    else -> unit
                                }
                                Text(txt); Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                opts.plus(otherLabel).forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                                        unit = opt; unitExpanded = false; if (opt != otherLabel) customUnit = ""
                                    })
                                }
                            }
                        }
                        if (unit == otherLabel) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                customUnit,
                                { customUnit = it },
                                label = { Text(specifyUnit) },
                                isError = error.isNotEmpty() && customUnit.isBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else { /* type == Other (custom) */
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            customUnit,
                            { customUnit = it; unit = otherLabel },
                            label = { Text(specifyUnit) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (type) {
                    bpLabel -> {
                        OutlinedTextField(
                            sysText,
                            { sysText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(systolicLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            diaText,
                            { diaText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(diastolicLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    glucoseLabel, cholLabel -> {
                        OutlinedTextField(
                            valueText,
                            { valueText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(valueLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
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
                /* validation + build object */
                val finalType = if (type == otherLabel) customType.trim() else type
                val finalUnit = if (unit == otherLabel) customUnit.trim() else unit

                val saved: VitalSign? = when (finalType) {
                    bpLabel -> {
                        val s = sysText.toDoubleOrNull()
                        val d = diaText.toDoubleOrNull()
                        if (s == null || d == null) { error = bpErr; null }
                        else VitalSign(
                            id = initial?.id,
                            userId = userId ?: "",
                            type = bpLabel,
                            value = s,
                            unit = finalUnit.ifBlank { "mmHg" },
                            timestamp = df.format(Date()),
                            manualSystolic = s,
                            manualDiastolic = d
                        )
                    }
                    glucoseLabel, cholLabel -> {
                        val v = valueText.toDoubleOrNull()
                        if (v == null) { error = inputErr; null }
                        else VitalSign(
                            id = initial?.id,
                            userId = userId ?: "",
                            type = finalType,
                            value = v,
                            unit = finalUnit.ifBlank { "mg/dL" },
                            timestamp = df.format(Date())
                        )
                    }
                    else -> { error = inputErr; null }
                }

                saved?.let { onSave(it) }
            }) { Text(saveLabel) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(cancelLabel) } }
    )
}