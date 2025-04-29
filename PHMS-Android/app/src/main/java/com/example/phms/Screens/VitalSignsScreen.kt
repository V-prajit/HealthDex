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
import androidx.compose.foundation.text.KeyboardOptions // Keep necessary imports
import androidx.compose.ui.text.input.KeyboardType    // Keep necessary imports
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
    val realTimeDataLabel = stringResource(R.string.real_time_vitals_header) // Use the correct string resource
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

    // —— Timestamp formatter for parsing manual entry timestamps —— (From Version 2)
    val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    // Prepare data for charts (Using logic from Version 2)
    val heartRateData = remember(vitalHistory) {
        vitalHistory.mapNotNull { it.heartRate?.let { hr -> ChartDataPoint(it.timestampMs, hr) } }
    }
    // Use manual entries for BP / Glucose / Chol charts
    val bpData = remember(vitals) {
        vitals.filter { it.type == bpLabel && it.manualSystolic != null && it.manualDiastolic != null }
            .mapNotNull { v ->
                try { // Add try-catch for robustness during parsing
                    val ts = df.parse(v.timestamp)?.time ?: return@mapNotNull null
                    BPChartDataPoint(ts, v.manualSystolic!!.toFloat(), v.manualDiastolic!!.toFloat())
                } catch (e: Exception) {
                    // Log error or handle gracefully if timestamp format is wrong
                    null
                }
            }
    }
    val glucoseData = remember(vitals) {
        vitals.filter { it.type == glucoseLabel && it.value != null }
            .mapNotNull { v ->
                 try {
                    val ts = df.parse(v.timestamp)?.time ?: return@mapNotNull null
                    ChartDataPoint(ts, v.value!!.toFloat())
                 } catch (e: Exception) {
                     null
                 }
            }
    }
    val cholesterolData = remember(vitals) {
        vitals.filter { it.type == cholLabel && it.value != null }
            .mapNotNull { v ->
                 try {
                    val ts = df.parse(v.timestamp)?.time ?: return@mapNotNull null
                    ChartDataPoint(ts, v.value!!.toFloat())
                 } catch (e: Exception) {
                     null
                 }
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
                     Text(realTimeDataLabel, style = MaterialTheme.typography.headlineSmall)
                     Spacer(Modifier.height(12.dp))

                     // Heart Rate Chart (Using vitalHistory data)
                     SimpleLineChart(
                         modifier = Modifier.fillMaxWidth(),
                         title = "Heart Rate (bpm)",
                         data = heartRateData,
                         highThreshold = thresholds.hrHigh,
                         lowThreshold = thresholds.hrLow,
                         lineColor = ChartRed
                     )

                     // Blood Pressure Chart (Using manual 'vitals' data - From Version 2)
                      BloodPressureChart(
                          modifier = Modifier.fillMaxWidth(),
                          data = bpData,
                          sysHighThreshold = thresholds.bpSysHigh,
                          sysLowThreshold = thresholds.bpSysLow,
                          diaHighThreshold = thresholds.bpDiaHigh,
                          diaLowThreshold = thresholds.bpDiaLow
                      )


                     // Glucose Chart (Using manual 'vitals' data - From Version 2)
                     SimpleLineChart(
                         modifier = Modifier.fillMaxWidth(),
                         title = "Glucose (mg/dL)",
                         data = glucoseData,
                         highThreshold = thresholds.glucoseHigh,
                         lowThreshold = thresholds.glucoseLow,
                         lineColor = ChartOrange
                     )

                     // Cholesterol Chart (Using manual 'vitals' data - From Version 2)
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
                             shape = RoundedCornerShape(0.dp),
                             elevation = CardDefaults.elevatedCardElevation(4.dp)
                         ) {
                             Column(Modifier.padding(16.dp)) {
                                 Text(mostRecentLabel, style = MaterialTheme.typography.titleSmall)
                                 Spacer(Modifier.height(4.dp))
                                 val valueDisplay =
                                    if (v.type == bpLabel)
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
                      // Display a general message if both real-time and manual are empty,
                      // or specific manual message if only manual is empty.
                      val message = if(vitalHistory.isEmpty() && vitals.isEmpty()) {
                          "No vital signs data available."
                      } else {
                           noEntriesLabel // "No entries yet for the selected filter."
                      }
                      Text(message, style = MaterialTheme.typography.bodyMedium)
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
                                      "${v.manualSystolic?.toInt()}/${v.manualDiastolic?.toInt()} ${v.unit}"
                                  } else {
                                      "${v.value ?: ""} ${v.unit}"
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
                                                     // Optionally, refetch latestByType if the deleted one was the latest
                                                     latestByType = if (userId != null && selectedType != allLabel && selectedType != otherLabel)
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
                             text = v.timestamp,
                             style = MaterialTheme.typography.bodySmall,
                             modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
                         )
                     }
                 }
             }

             // Add some bottom padding to prevent FAB overlap
             item {
                 Spacer(Modifier.height(80.dp))
             }
        }
    }

    // — Add/Edit dialog for MANUAL entries —
    if (showDlg) {
        VitalDialog( // Using the identical VitalDialog composable from both versions
            initial  = editing,
            userId   = userId,
            onSave   = { newV ->
                scope.launch {
                    if (newV.id == null) VitalRepository.addVital(newV)
                    else                  VitalRepository.updateVital(newV)
                    vitals = VitalRepository.getVitals(userId!!) // Reload manual entries
                    // Refetch latest after save/update
                     latestByType = if (userId != null && selectedType != allLabel && selectedType != otherLabel)
                          VitalRepository.getLatestVital(userId, selectedType)
                     else null
                    showDlg = false
                }
            },
            onCancel = { showDlg = false }
        )
    }
}

// --- VitalDialog Composable (Identical in both provided versions) ---
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
    // Correct initialization for customUnit based on initial data and type
     var customUnit   by remember { mutableStateOf(initial?.unit?.takeIf { initial.type == otherLabel || it !in unitMap[initial.type].orEmpty() } ?: "") }


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
             // Recalculate customUnit based on potentially updated type/unit
             customUnit = initial.unit.takeIf { initial.type == otherLabel || it !in unitMap[initial.type].orEmpty() } ?: ""

         } else {
              // Reset for adding new item or if type changes clear irrelevant fields
             if (type == bpLabel) {
                 valueText = ""
                 // Ensure default unit is set if type is BP and unit is blank OR invalid for BP
                 if(unit.isBlank() || unit !in (unitMap[bpLabel] ?: emptyList())) {
                     unit = unitMap[bpLabel]?.firstOrNull() ?: ""
                 }
             } else if (type != otherLabel){ // If type is specific (Glucose, HR, Chol) reset SBP/DBP
                 systolicValueText = ""
                 diastolicValueText = ""
                  // Reset unit if it's not valid for the new type (unless it's blank or Other)
                 if(unit.isNotBlank() && unit != otherLabel && unit !in (unitMap[type] ?: emptyList())) {
                    unit = unitMap[type]?.firstOrNull() ?: "" // Default or clear based on preference
                 }
             } else { // Type is "Other" or blank
                 systolicValueText = ""
                 diastolicValueText = ""
                 // Unit handling for "Other" is managed by its specific field/logic below
             }
             // If not editing, ensure custom fields are clear unless type/unit dictates them
             if(type != otherLabel) customType = ""
             if(unit != otherLabel) customUnit = ""

         }
          error = "" // Always clear error on init/type change
     }


    AlertDialog(
      onDismissRequest = onCancel,
        // Optional: Set background color and elevation if needed
        // containerColor = MaterialTheme.colorScheme.surface,
        // tonalElevation = 0.dp,
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
                type == otherLabel              -> "$otherLabel (${specifyTypeLabel.lowercase()})" // Indicate custom input needed
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
                  val oldType = type // Store old type to compare
                  type = opt // Let LaunchedEffect handle state resets based on new type
                  if (opt != oldType) { // Only reset unit if type *actually* changed
                      unit = "" // Clear unit to force re-selection or default setting in LaunchedEffect
                      customUnit = "" // Clear custom unit
                      if (opt != otherLabel) customType = "" // Clear custom type only if not selecting 'Other'
                  }
                })
              }
            }
          }

          if (type == otherLabel) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = customType,
              onValueChange = { customType = it; error = "" }, // Clear error on change
              label = { Text(specifyTypeLabel) },
              isError = error.isNotEmpty() && customType.isBlank(), // Error if "Other" type but no custom type specified
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
          } else if (type != otherLabel) { // Specific type selected (BP, Glucose, etc.)
               val currentUnitOptions = unitMap[type] ?: emptyList()
               // Auto-select default unit if unit is blank or invalid for current type
               LaunchedEffect(type, unit) {
                    if(unit.isBlank() || (unit != otherLabel && unit !in currentUnitOptions)) {
                       unit = currentUnitOptions.firstOrNull() ?: otherLabel // Default to first option or 'Other' if no options
                    }
               }

              Box {
                  OutlinedButton(
                      onClick = { unitExpanded = true },
                      modifier = Modifier.fillMaxWidth(),
                      // Enable button only if type has defined units or allows 'Other'
                      enabled = currentUnitOptions.isNotEmpty()
                  ) {
                      val displayUnit = when {
                          unit == otherLabel && customUnit.isNotBlank() -> customUnit
                          unit == otherLabel -> "$otherLabel (${specifyUnitLabel.lowercase()})"
                          unit.isNotBlank() -> unit
                          else -> selectUnitLabel // Should ideally be handled by LaunchedEffect above
                      }
                      Text(displayUnit)
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                  }
                  DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                      currentUnitOptions.plus(otherLabel).forEach { opt ->
                          DropdownMenuItem(text = { Text(opt) }, onClick = {
                              unitExpanded = false
                              unit = opt
                              if (opt != otherLabel) customUnit = "" // Clear custom unit if standard one selected
                              error = "" // Clear error
                          })
                      }
                  }
              }
              if (unit == otherLabel) {
                  Spacer(Modifier.height(8.dp))
                  OutlinedTextField(
                      value = customUnit,
                      onValueChange = { customUnit = it; error = "" }, // Clear error on change
                      label = { Text(specifyUnitLabel) },
                      isError = error.isNotEmpty() && customUnit.isBlank(), // Error if 'Other' unit selected but not specified
                      modifier = Modifier.fillMaxWidth()
                  )
              }
          } else { // type == "Other"
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(
                  value = customUnit, // Bind directly to customUnit for "Other" type
                  onValueChange = {
                      customUnit = it
                      unit = otherLabel // Ensure unit state is 'Other'
                      error = "" // Clear error
                  },
                  label = { Text(specifyUnitLabel) }, // Use the specific label
                   isError = error.isNotEmpty() && customUnit.isBlank(), // Error if type is Other and unit is blank
                  modifier = Modifier.fillMaxWidth()
              )
          }

          Spacer(Modifier.height(12.dp))

          // — Value field(s) - Conditional Rendering —
           if (type == bpLabel) {
               OutlinedTextField(
                   value = systolicValueText,
                   onValueChange = { systolicValueText = it.filter { char -> char.isDigit() || char == '.' }; error = "" },
                   label = { Text(systolicLabel) },
                   isError = error.isNotEmpty(),
                   keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                   singleLine = true,
                   modifier = Modifier.fillMaxWidth()
               )
               Spacer(Modifier.height(8.dp))
               OutlinedTextField(
                   value = diastolicValueText,
                   onValueChange = { diastolicValueText = it.filter { char -> char.isDigit() || char == '.' }; error = "" },
                   label = { Text(diastolicLabel) },
                   isError = error.isNotEmpty(),
                   keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                   singleLine = true,
                   modifier = Modifier.fillMaxWidth()
               )
           } else if (type.isNotBlank()) { // Show single value field for non-BP, non-blank types
               OutlinedTextField(
                   value = valueText,
                   onValueChange = { valueText = it.filter { char -> char.isDigit() || char == '.' }; error = "" },
                   label = { Text(valueLabel) },
                   isError = error.isNotEmpty(),
                   keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                   singleLine = true,
                   modifier = Modifier.fillMaxWidth()
               )
           }

          if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          // --- Validation Logic ---
          val finalType = if (type == otherLabel) customType.trim() else type
          val finalUnit = if (unit == otherLabel) customUnit.trim() else unit

          var isValid = true
          var vitalToSave: VitalSign? = null
          error = "" // Clear previous errors

          // 1. Check Type
          if (finalType.isBlank()) {
              error = "Please select or specify a vital sign type."
              isValid = false
          }
          // 2. Check Unit
          else if (finalUnit.isBlank()) {
              error = "Please select or specify a unit."
              isValid = false
          }
          // 3. Check Values based on Type
          else {
              if (type == bpLabel) {
                  val systolicDbl = systolicValueText.toDoubleOrNull()
                  val diastolicDbl = diastolicValueText.toDoubleOrNull()
                  if (systolicDbl == null || diastolicDbl == null || systolicDbl <= 0 || diastolicDbl <= 0) {
                      error = bpInputErrorTxt // "Invalid systolic/diastolic values."
                      isValid = false
                  } else {
                      vitalToSave = VitalSign(
                          id = initial?.id,
                          userId = userId ?: "",
                          type = finalType,
                          value = systolicDbl, // Store primary value (systolic) here too if needed by some logic
                          unit = finalUnit,
                          timestamp = df.format(Date()),
                          manualSystolic = systolicDbl,
                          manualDiastolic = diastolicDbl
                      )
                  }
              } else { // Non-BP types
                  val dbl = valueText.toDoubleOrNull()
                  if (dbl == null || dbl < 0) { // Allow 0? Adjust if needed.
                      error = inputErrorTxt // "Invalid value."
                      isValid = false
                  } else {
                      vitalToSave = VitalSign(
                          id = initial?.id,
                          userId = userId ?: "",
                          type = finalType,
                          value = dbl,
                          unit = finalUnit,
                          timestamp = df.format(Date()),
                          manualSystolic = null, // Ensure these are null for non-BP
                          manualDiastolic = null
                      )
                  }
              }
          }
          // --- End Validation Logic ---

          if (isValid && vitalToSave != null) {
              onSave(vitalToSave)
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