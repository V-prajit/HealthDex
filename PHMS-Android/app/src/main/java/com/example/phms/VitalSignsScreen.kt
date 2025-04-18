package com.example.phms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
  val scope = rememberCoroutineScope()
  var vitals  by remember { mutableStateOf<List<VitalSign>>(emptyList()) }
  var showDlg by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<VitalSign?>(null) }

  // Load on first composition
  LaunchedEffect(userId) {
    if (userId != null) vitals = VitalRepository.getVitals(userId)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.vitals)) },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { editing = null; showDlg = true },
        modifier = Modifier.padding(bottom = 64.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_vital))
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
    ) {
      if (vitals.isEmpty()) {
        Text(stringResource(R.string.no_entries_yet))
      } else {
        LazyColumn {
          items(vitals) { v ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { editing = v; showDlg = true }
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(v.type, style = MaterialTheme.typography.titleMedium)
                  Text("${v.value} ${v.unit}", style = MaterialTheme.typography.bodyMedium)
                  Text(v.timestamp, style = MaterialTheme.typography.bodySmall)
                }

                // Edit button
                IconButton(onClick = {
                  editing = v
                  showDlg = true
                }) {
                  Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_vital))
                }

                // Delete button
                IconButton(onClick = {
                  scope.launch {
                    v.id?.let { id ->
                      if (VitalRepository.deleteVital(id)) {
                        vitals = VitalRepository.getVitals(userId!!)
                      }
                    }
                  }
                }) {
                  Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
              }
            }
          }
        }
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
  // Formatter for "now"
  val df = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

  var type      by remember { mutableStateOf(initial?.type ?: "") }
  var valueText by remember { mutableStateOf(initial?.value?.toString() ?: "") }
  var unit      by remember { mutableStateOf(initial?.unit ?: "") }
  var error     by remember { mutableStateOf("") }

  val vitalInputErrorText = stringResource(R.string.vital_input_error)

  AlertDialog(
    onDismissRequest = onCancel,
    title            = {
      Text(
        if (initial == null)
          stringResource(R.string.add_vital)
        else
          stringResource(R.string.edit_vital)
      )
    },
    text             = {
      Column {
        OutlinedTextField(
          value = type,
          onValueChange = { type = it },
          label = { Text(stringResource(R.string.type_label)) },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = valueText,
          onValueChange = { valueText = it },
          label = { Text(stringResource(R.string.value_label)) },
          isError = error.isNotEmpty(),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = unit,
          onValueChange = { unit = it },
          label = { Text(stringResource(R.string.unit_label)) },
          modifier = Modifier.fillMaxWidth()
        )
        if (error.isNotEmpty()) {
          Text(error, color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton   = {
      TextButton(onClick = {
        val dbl = valueText.toDoubleOrNull()
        if (type.isBlank() || dbl == null || unit.isBlank()) {
          error = vitalInputErrorText
        } else {
          // Auto‑set timestamp to now
          val now = df.format(Date())
          onSave(VitalSign(initial?.id, userId ?: "", type, dbl, unit, now))
        }
      }) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton   = {
      TextButton(onClick = onCancel) {
        Text(stringResource(R.string.cancel))
      }
    }
  )
}
