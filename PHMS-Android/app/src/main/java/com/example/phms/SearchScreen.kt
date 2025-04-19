package com.example.phms

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
// +assistant: added for filter chips
import androidx.compose.material3.FilterChip

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.phms.NotesRepository
import com.example.phms.NotesRepositoryBackend
import com.example.phms.VitalRepository
import com.example.phms.Appointment
import com.example.phms.AppointmentRepository
import androidx.compose.material.icons.filled.EventNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    userToken: String?,
    onClose: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var recentSearches by remember { mutableStateOf(listOf("sleep", "bp", "morning meds")) }

    var allNotes by remember { mutableStateOf(listOf<String>()) }
    var allVitals by remember { mutableStateOf(listOf<String>()) }
    var allAppointments by remember { mutableStateOf(listOf<Appointment>()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userToken) {
        // fetch notes
        allNotes = if (!userToken.isNullOrEmpty()) {
            NotesRepositoryBackend.getNotes(userToken)
        } else {
            NotesRepository.getNotes(context)
        }
        // fetch vitals
        allVitals = userToken?.let {
            VitalRepository.getVitals(it).map { v -> "${v.type}: ${v.value} ${v.unit}" }
        } ?: emptyList()
        allAppointments = userToken?.let { AppointmentRepository.getUpcomingAppointments(it) } ?: emptyList()
    }

    val filteredNotes by remember(allNotes, query, selectedCategory) {
        derivedStateOf {
            allNotes.filter { note ->
                note.contains(query, ignoreCase = true) &&
                        (selectedCategory == "All" || selectedCategory == "Notes")
            }
        }
    }
    val filteredVitals by remember(allVitals, query, selectedCategory) {
        derivedStateOf {
            allVitals.filter { vital ->
                vital.contains(query, ignoreCase = true) &&
                        (selectedCategory == "All" || selectedCategory == "Vital")
            }
        }
    }
    val filteredAppointments by remember(allAppointments, query, selectedCategory) {
        derivedStateOf {
            allAppointments.filter { appt ->
                (appt.doctorName?.contains(query, ignoreCase = true) ?: false) &&
                        (selectedCategory == "All" || selectedCategory == "Appointments")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text("Search notes, vitals, appointments...") }, // +assistant: updated placeholder
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // search results
            if (query.isNotEmpty()) {
                Text("Search results for \"$query\"", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredNotes.isNotEmpty()) {
                    Text("Notes:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn {
                        items(filteredNotes) { note ->
                            Text(
                                text = note,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { /* handle click */ },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredVitals.isNotEmpty()) {
                    Text("Vitals:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn {
                        items(filteredVitals) { vital ->
                            Text(
                                text = vital,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { /* handle click */ },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // +assistant: display appointment results
                if (filteredAppointments.isNotEmpty()) {
                    Text("Appointments:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn {
                        items(filteredAppointments) { appt ->
                            Text(
                                text = "${appt.date} ${appt.time} with ${appt.doctorName ?: "Doctor"}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { /* handle click */ },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredNotes.isEmpty() && filteredVitals.isEmpty() && filteredAppointments.isEmpty()) {
                    Text(
                        text = "No results found",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // recent searches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { recentSearches = emptyList() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear Recent")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(recentSearches) { item ->
                    Text(
                        text = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { query = item },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // categories
            Text("Search by category", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            CategoryChips(selectedCategory) { selectedCategory = it }
        }
    }
}

@Composable
private fun CategoryChips(
    selected: String,
    onSelect: (String) -> Unit
) {
    val categories = listOf("All", "Notes", "Vital", "Appointments", "Medication", "Diet")
    val icons = mapOf(
        "All" to Icons.Default.Search,
        "Notes" to Icons.Default.Note,
        "Vital" to Icons.Default.Favorite,
        "Appointments" to Icons.Default.EventNote,
        "Medication" to Icons.Default.LocalPharmacy,
        "Diet" to Icons.Default.Restaurant
    )

    Column {
        categories.chunked(3).forEach { rowCats ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCats.forEach { cat ->
                    FilterChip(
                        selected = (selected == cat),
                        onClick = { onSelect(cat) },
                        leadingIcon = { Icon(icons[cat]!!, contentDescription = null) }, // +assistant: added icon
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}