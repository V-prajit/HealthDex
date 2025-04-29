package com.example.phms.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalSignsScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    vitalSignsViewModel: VitalSignsViewModel = viewModel()
) {
    // —— Strings ——
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
    val manualEntriesLabel = "Manual Entries"

    // —— UI state ——
    val scope = rememberCoroutineScope()
    var vitals       by remember { mutableStateOf<List<VitalSign>>(emptyList()) }
    var showDlg      by remember { mutableStateOf(false) }
    var editing      by remember { mutableStateOf<VitalSign?>(null) }
    var selectedType by remember { mutableStateOf(allLabel) }
    var expanded     by remember { mutableStateOf(false) }
    var latestByType by remember { mutableStateOf<VitalSign?>(null) }

    // —— ViewModel State ——
    val vitalHistory by vitalSignsViewModel.vitalHistory.collectAsState()
    val thresholds   by vitalSignsViewModel.thresholds.collectAsState()

    // —— Load manual data ——
    LaunchedEffect(userId) {
        if (userId != null) vitals = VitalRepository.getVitals(userId)
    }
    LaunchedEffect(userId, selectedType) {
        latestByType = if (userId != null && selectedType != allLabel && selectedType != otherLabel)
            VitalRepository.getLatestVital(userId, selectedType)
        else null
    }

    // —— NEW: timestamp formatter for manual-entry charts ——
    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    // —— Chart data ——
    val heartRateData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.heartRate?.let { hr -> ChartDataPoint(it.timestampMs, hr) } }
    }

    // ---- CHANGED: use manual entries for BP / Glucose / Chol ----
    val bpData = remember(vitals) {
        vitals.filter { it.type == bpLabel && it.manualSystolic != null && it.manualDiastolic != null }
            .mapNotNull { v ->
                val ts = df.parse(v.timestamp)?.time ?: return@mapNotNull null
                BPChartDataPoint(ts, v.manualSystolic!!.toFloat(), v.manualDiastolic!!.toFloat())
            }
    }
    val glucoseData = remember(vitals) {
        vitals.filter { it.type == glucoseLabel && it.value != null }
            .mapNotNull { v ->
                val ts = df.parse(v.timestamp)?.time ?: return@mapNotNull null
                ChartDataPoint(ts, v.value!!.toFloat())
            }
    }
    val cholesterolData = remember(vitals) {
        vitals.filter { it.type == cholLabel && it.value != null }
            .mapNotNull { v ->
                val ts = df.parse(v.timestamp)?.time ?: return@mapNotNull null
                ChartDataPoint(ts, v.value!!.toFloat())
            }
    }

    // —— Compute filtered list for MANUAL entries ——
    val filteredVitals by remember(vitals, selectedType) {
        derivedStateOf {
            when (selectedType) {
                allLabel -> vitals
                otherLabel -> vitals.filter { vs ->
                    listOf(bpLabel, glucoseLabel, cholLabel, hrLabel).none { it == vs.type }
                }
                else -> vitals.filter { it.type == selectedType }
            }
        }
    }

    // —— Main Scaffold ——
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
                        Icon(Icons.Default.Settings, contentDescription = settingsLabel)
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

            // --- Real-time Data Section ---
            item {
                Column {
                    Text(realTimeDataLabel, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))

                    // Heart-rate (simulated)
                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Heart Rate (bpm)",
                        data = heartRateData,
                        highThreshold = thresholds.hrHigh,
                        lowThreshold = thresholds.hrLow,
                        lineColor = ChartRed
                    )

                    // Blood-pressure (MANUAL)
                    BloodPressureChart(
                        modifier = Modifier.fillMaxWidth(),
                        data = bpData,
                        sysHighThreshold = thresholds.bpSysHigh,
                        sysLowThreshold = thresholds.bpSysLow,
                        diaHighThreshold = thresholds.bpDiaHigh,
                        diaLowThreshold = thresholds.bpDiaLow
                    )

                    // Glucose (MANUAL)
                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Glucose (mg/dL)",
                        data = glucoseData,
                        highThreshold = thresholds.glucoseHigh,
                        lowThreshold = thresholds.glucoseLow,
                        lineColor = ChartOrange
                    )

                    // Cholesterol (MANUAL)
                    SimpleLineChart(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Total Cholesterol (mg/dL)",
                        data = cholesterolData,
                        highThreshold = thresholds.cholesterolHigh,
                        lowThreshold = thresholds.cholesterolLow,
                        lineColor = ChartPurple
                    )
                }
            }

            // --- Manual Entries Header ---
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(manualEntriesLabel, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
            }

            // --- Manual Entries Filter Row ---
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(filterLabel, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(selectedType)
                            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(20.dp))
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

            // --- Most Recent Manual Entry ---
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
                                val valueDisplay = if (v.type == bpLabel)
                                    "${v.manualSystolic?.toInt()}/${v.manualDiastolic?.toInt()} ${v.unit}"
                                else
                                    "${v.value ?: ""} ${v.unit}"
                                Text("${v.type}: $valueDisplay", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(2.dp))
                                Text(v.timestamp, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } ?: Text(noRecentLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // --- Full filtered list of Manual Entries ---
            if (filteredVitals.isEmpty()) {
                item {
                    if (vitalHistory.isEmpty()) {
                        Text("No vital signs data available.", style = MaterialTheme.typography.bodyMedium)
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
                            headlineContent = { Text(v.type) },
                            supportingContent = {
                                val valueDisplay = if (v.type == bpLabel)
                                    "${v.manualSystolic?.toInt()}/${v.manualDiastolic?.toInt()} ${v.unit}"
                                else
                                    "${v.value ?: ""} ${v.unit}"
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

            // Bottom padding so FAB doesn’t cover list
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // — Add/Edit dialog for MANUAL entries —
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

// --- VitalDialog --- // Corrected version from previous step
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalDialog(
    initial: VitalSign?,
    userId: String?,
    onSave: (VitalSign) -> Unit,
    onCancel: () -> Unit
) {
    // timestamp formatter
    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    // --- Strings ---
    val saveLabel        = stringResource(R.string.save)
    val cancelLabel      = stringResource(R.string.cancel)
    val inputErrorTxt    = stringResource(R.string.vital_input_error)
    val bpInputErrorTxt  = stringResource(id = R.string.bp_input_error) // Ensure this exists in strings.xml
    val dialogTitle      = stringResource(
        if (initial == null) R.string.add_vital else R.string.edit_vital
    )
    val selectTypeLabel  = stringResource(R.string.select_type)
    val otherLabel       = stringResource(R.string.other)
    val specifyTypeLabel = stringResource(R.string.specify_type)
    val selectUnitLabel  = stringResource(R.string.select_unit)
    val specifyUnitLabel = stringResource(R.string.specify_unit)
    val systolicLabel    = stringResource(id = R.string.systolic_value) // Ensure this exists
    val diastolicLabel   = stringResource(id = R.string.diastolic_value) // Ensure this exists
    val valueLabel       = stringResource(R.string.value_label)


    // --- Type dropdown state ---
    val bpLabel       = stringResource(R.string.blood_pressure)
    val glucoseLabel  = stringResource(R.string.glucose)
    val cholLabel     = stringResource(R.string.cholesterol)
    val hrLabel       = stringResource(R.string.heart_rate)
    val typeOptions   = listOf(bpLabel, glucoseLabel, cholLabel, hrLabel, otherLabel)

    var typeExpanded  by remember { mutableStateOf(false) }
    var type          by remember { mutableStateOf(initial?.type ?: "") }
    var customType    by remember { mutableStateOf(initial?.type?.takeIf { it !in typeOptions } ?: "") }

    // --- Unit dropdown state ---
    val unitMap = mapOf(
      bpLabel      to listOf("mmHg","kPa","cmH₂O","inHg"),
      glucoseLabel to listOf("mg/dL","mmol/L","mg%","g/L"),
      cholLabel    to listOf("mg/dL","mmol/L","mg%","g/L"),
      hrLabel      to listOf("bpm","bps","Hz","cpm")
    )
    var unitExpanded by remember { mutableStateOf(false) }
    var unit         by remember { mutableStateOf(initial?.unit ?: "") }
    var customUnit   by remember { mutableStateOf(initial?.unit?.takeIf { it !in unitMap[type].orEmpty() } ?: "") }

    // --- Value field state ---
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

     // Effect to handle initialization and type changes
     LaunchedEffect(initial, type) {
         if (initial != null) {
             // Initialize based on editing item
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
              // Reset for adding new item or if type changes clear irrelevant fields
             if (type == bpLabel) {
                 valueText = ""
                 // Ensure default unit is set if type is BP and unit is blank
                 if(unit.isBlank()) unit = unitMap[bpLabel]?.firstOrNull() ?: ""
             } else {
                 systolicValueText = ""
                 diastolicValueText = ""
             }
         }
          error = "" // Always clear error on init/type change
     }


    AlertDialog(
      onDismissRequest = onCancel,
      title            = { Text(dialogTitle) },
      text             = {
        Column {
          // — Type selector —
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
                  type = opt // Let LaunchedEffect handle state resets
                  if (opt == otherLabel) customType = "" else customType = ""
                  // Reset unit only if type changes significantly (e.g., away from BP/Other)
                  if(opt != bpLabel && opt != otherLabel) unit = "" else if (opt == bpLabel && unit.isBlank()) unit = unitMap[bpLabel]?.firstOrNull() ?: ""
                  customUnit = "" // Always clear custom unit on type change for simplicity
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

          // — Unit selector —
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
                          unit == otherLabel -> specifyUnitLabel // Prompt if 'Other' selected but no custom unit yet
                          else -> selectUnitLabel // Default prompt if unit is blank otherwise
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
          } else { // type=="Other"
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(
                  value = customUnit, // Bind directly to customUnit for "Other" type
                  onValueChange = { customUnit = it; unit = otherLabel }, // Ensure unit state is 'Other'
                  label = { Text(specifyUnitLabel) },
                  modifier = Modifier.fillMaxWidth()
              )
          }

          Spacer(Modifier.height(12.dp))

          // — Value field(s) - Conditional Rendering —
           if (type == bpLabel) {
               OutlinedTextField(
                   value = systolicValueText,
                   onValueChange = { systolicValueText = it ; error = "" },
                   label = { Text(systolicLabel) },
                   isError = error.isNotEmpty(),
                   // *** CORRECTED KeyboardOptions usage ***
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
                    // *** CORRECTED KeyboardOptions usage ***
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
                    // *** CORRECTED KeyboardOptions usage ***
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

          vitalToSave?.let { onSave(it) } // Call onSave only if validation succeeded

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