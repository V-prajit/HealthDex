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
import androidx.lifecycle.viewmodel.compose.viewModel // Ensure this import is present
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Settings
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
    vitalSignsViewModel: VitalSignsViewModel = viewModel() // Inject ViewModel
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
    // --- Use new string resource for the chart header ---
    val realTimeDataLabel = stringResource(R.string.real_time_vitals_header)
    val manualEntriesLabel = "Manual Entries" // Keep or change this as needed

    // —— UI state ——
    val scope = rememberCoroutineScope()
    var vitals       by remember { mutableStateOf<List<VitalSign>>(emptyList()) } // For manual entries
    var showDlg      by remember { mutableStateOf(false) }
    var editing      by remember { mutableStateOf<VitalSign?>(null) }
    var selectedType by remember { mutableStateOf(allLabel) }
    var expanded     by remember { mutableStateOf(false) }
    var latestByType by remember { mutableStateOf<VitalSign?>(null) }

    // -- ViewModel State --
    val vitalHistory by vitalSignsViewModel.vitalHistory.collectAsState()
    val thresholds by vitalSignsViewModel.thresholds.collectAsState()


    // —— Load manual data (existing functionality) ——
    LaunchedEffect(userId) {
        if (userId != null) vitals = VitalRepository.getVitals(userId)
    }
    // Get latest MANUAL entry (existing functionality)
    LaunchedEffect(userId, selectedType) {
        latestByType = if (userId != null && selectedType != allLabel && selectedType != otherLabel)
            VitalRepository.getLatestVital(userId, selectedType)
        else null
    }

    // Prepare data for charts
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


    // —— Compute filtered list for MANUAL entries (existing functionality) ——
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
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = settingsLabel
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton( // FAB for adding MANUAL entries
                onClick = {
                    editing = null
                    showDlg = true
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp) // Keep padding if needed for navigation bar
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = addVitalDesc)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        // Use LazyColumn for overall scrollability including charts and manual list
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp), // Apply horizontal padding once
             contentPadding = PaddingValues(vertical = 16.dp), // Padding for top/bottom of list
             verticalArrangement = Arrangement.spacedBy(16.dp) // Spacing between items
        ) {

             // --- Real-time Data Section ---
             item {
                 Column {
                     // --- Use the new string resource here ---
                     Text(realTimeDataLabel, style = MaterialTheme.typography.headlineSmall)
                     Spacer(Modifier.height(12.dp))

                     // Heart Rate Chart
                     SimpleLineChart(
                         modifier = Modifier.fillMaxWidth(),
                         title = "Heart Rate (bpm)",
                         data = heartRateData,
                         highThreshold = thresholds.hrHigh,
                         lowThreshold = thresholds.hrLow,
                         lineColor = ChartRed // Assign specific colors if desired
                     )

                     // Blood Pressure Chart
                      BloodPressureChart(
                          modifier = Modifier.fillMaxWidth(),
                          data = bpData,
                          sysHighThreshold = thresholds.bpSysHigh,
                          sysLowThreshold = thresholds.bpSysLow,
                          diaHighThreshold = thresholds.bpDiaHigh,
                          diaLowThreshold = thresholds.bpDiaLow
                      )


                     // Glucose Chart
                     SimpleLineChart(
                         modifier = Modifier.fillMaxWidth(),
                         title = "Glucose (mg/dL)",
                         data = glucoseData,
                         highThreshold = thresholds.glucoseHigh,
                         lowThreshold = thresholds.glucoseLow,
                         lineColor = ChartOrange
                     )

                     // Cholesterol Chart
                     SimpleLineChart(
                         modifier = Modifier.fillMaxWidth(),
                         title = "Total Cholesterol (mg/dL)",
                         data = cholesterolData,
                         highThreshold = thresholds.cholesterolHigh,
                         lowThreshold = thresholds.cholesterolLow,
                          lineColor = ChartPurple // Assign specific colors if desired
                     )
                 }
             }

             // --- Manual Entries Section Separator ---
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


             // --- Most Recent Manual Entry section ---
             if (selectedType != allLabel && selectedType != otherLabel) {
                 item {
                     latestByType?.let { v ->
                         ElevatedCard(
                             modifier = Modifier.fillMaxWidth(),
                             shape = RoundedCornerShape(12.dp),
                             elevation = CardDefaults.elevatedCardElevation(4.dp)
                         ) {
                             Column(Modifier.padding(16.dp)) {
                                 Text(mostRecentLabel, style = MaterialTheme.typography.titleSmall)
                                 Spacer(Modifier.height(4.dp))
                                 Text("${v.type}: ${v.value} ${v.unit}", style = MaterialTheme.typography.bodyLarge)
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
                     // Conditionally display message if *both* manual and simulated are empty maybe?
                      // For now, just the manual entry message
                      if(vitalHistory.isEmpty()) { // Check if simulated history is also empty
                          Text("No vital signs data available.", style = MaterialTheme.typography.bodyMedium)
                      } else {
                          Text(noEntriesLabel, style = MaterialTheme.typography.bodyMedium)
                      }
                 }
             } else {
                 items(filteredVitals, key = { it.id ?: UUID.randomUUID() }) { v -> // Use a key for stability
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
                             modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, end = 16.dp) // Padding added
                         )
                     }
                 }
             }

             // Add some bottom padding to prevent FAB overlap if needed
             item {
                 Spacer(Modifier.height(80.dp))
             }
        }
    }

    // — Add/Edit dialog for MANUAL entries (Existing Functionality - Unchanged) —
    if (showDlg) {
        VitalDialog(
            initial  = editing,
            userId   = userId,
            onSave   = { newV ->
                scope.launch {
                    if (newV.id == null) VitalRepository.addVital(newV)
                    else                  VitalRepository.updateVital(newV)
                    vitals = VitalRepository.getVitals(userId!!) // Reload manual entries
                    showDlg = false
                }
            },
            onCancel = { showDlg = false }
        )
    }
}

// --- VitalDialog (Existing Functionality - Unchanged) ---
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
    val dialogTitle      = stringResource(
        if (initial == null) R.string.add_vital else R.string.edit_vital
    )
    val selectTypeLabel  = stringResource(R.string.select_type)
    val otherLabel       = stringResource(R.string.other)
    val specifyTypeLabel = stringResource(R.string.specify_type)
    val selectUnitLabel  = stringResource(R.string.select_unit)
    val specifyUnitLabel = stringResource(R.string.specify_unit)

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
    var customUnit   by remember { mutableStateOf(initial?.unit.takeIf { it !in unitMap[type].orEmpty() } ?: "") }

    // --- Value field state ---
    var valueText    by remember { mutableStateOf(initial?.value?.toString() ?: "") }
    var error        by remember { mutableStateOf("") }

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
                  type = opt
                  if (opt == otherLabel) customType = ""
                  unit = ""         // reset unit whenever type changes
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

          // — Unit selector — always shown as “dropdown” button
          if (type.isBlank()) {
            // nothing happens until a type is chosen
            OutlinedButton(
              onClick = { /* no-op */ },
              enabled = false,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(selectUnitLabel)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
          }
          else if (type != otherLabel) {
            // built‑in type → its unit list + Other
            Box {
              OutlinedButton(
                onClick = { unitExpanded = true },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(if (unit.isBlank()) selectUnitLabel else unit)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
              }
              DropdownMenu(
                expanded = unitExpanded,
                onDismissRequest = { unitExpanded = false }
              ) {
                (unitMap[type] ?: emptyList()).plus(otherLabel).forEach { opt ->
                  DropdownMenuItem(text = { Text(opt) }, onClick = {
                    unitExpanded = false
                    unit = opt
                    if (opt == otherLabel) customUnit = ""
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
          }
          else {
            // type=="Other" → free‑form unit
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = customUnit,
              onValueChange = { customUnit = it },
              label = { Text(specifyUnitLabel) },
              modifier = Modifier.fillMaxWidth()
            )
          }

          Spacer(Modifier.height(12.dp))

          // — Value field —
          OutlinedTextField(
            value = valueText,
            onValueChange = { valueText = it },
            label = { Text(stringResource(R.string.value_label)) },
            isError = error.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
          )

          if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          // finalize
          val finalType = if (type == otherLabel) customType.trim() else type
          val finalUnit = when {
            type == otherLabel            -> customUnit.trim()
            unit == otherLabel            -> customUnit.trim()
            else                          -> unit
          }
          val dbl = valueText.toDoubleOrNull()
          if (finalType.isBlank() || dbl == null || finalUnit.isBlank()) {
            error = inputErrorTxt
          } else {
            val now = df.format(Date())
            onSave(VitalSign(initial?.id, userId ?: "", finalType, dbl, finalUnit, now))
          }
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