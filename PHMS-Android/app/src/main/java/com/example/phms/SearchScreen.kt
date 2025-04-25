package com.example.phms

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.phms.NotesRepository
import com.example.phms.NotesRepositoryBackend
import com.example.phms.VitalRepository
import com.example.phms.Appointment
import com.example.phms.AppointmentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    userToken: String?,
    onClose: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToMedications: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val recentSearchesState = remember { mutableStateListOf<String>() }

    var allNotes by remember { mutableStateOf(listOf<String>()) }
    var allVitals by remember { mutableStateOf(listOf<String>()) }
    var allAppointments by remember { mutableStateOf(listOf<Appointment>()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(userToken) {
        allNotes = if (!userToken.isNullOrEmpty()) {
            NotesRepositoryBackend.getNotes(userToken)
        } else {
            NotesRepository.getNotes(context)
        }
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
                        onValueChange = {
                            query = it
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text("Search notes, vitals, appointments...") },
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
            if (query.isNotEmpty()) {
                Text("Search results for \"$query\"", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredNotes.isNotEmpty()) {
                    Text("Notes:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn {
                        items(filteredNotes) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val currentQuery = query
                                        if (currentQuery.isNotBlank()) {
                                            recentSearchesState.remove(currentQuery)
                                            recentSearchesState.add(0, currentQuery)
                                            if (recentSearchesState.size > 5)
                                                recentSearchesState.removeAt(recentSearchesState.lastIndex)
                                        }
                                        onNavigateToNotes()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Note, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = highlightQuery(note, query),
                                        style = MaterialTheme.typography.bodyMedium,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredVitals.isNotEmpty()) {
                    Text("Vitals:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn {
                        items(filteredVitals) { vital ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val currentQuery = query
                                        if (currentQuery.isNotBlank()) {
                                            recentSearchesState.remove(currentQuery)
                                            recentSearchesState.add(0, currentQuery)
                                            if (recentSearchesState.size > 5)
                                                recentSearchesState.removeAt(recentSearchesState.lastIndex)
                                        }
                                        onNavigateToVitals()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = highlightQuery(vital, query),
                                        style = MaterialTheme.typography.bodyMedium,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredAppointments.isNotEmpty()) {
                    Text("Appointments:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn {
                        items(filteredAppointments) { appt ->
                            val label = "${appt.date} ${appt.time} with ${appt.doctorName ?: "Doctor"}"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val currentQuery = query
                                        if (currentQuery.isNotBlank()) {
                                            recentSearchesState.remove(currentQuery)
                                            recentSearchesState.add(0, currentQuery)
                                            if (recentSearchesState.size > 5)
                                                recentSearchesState.removeAt(recentSearchesState.lastIndex)
                                        }
                                        onNavigateToAppointments()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EventNote, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = highlightQuery(label, query),
                                        style = MaterialTheme.typography.bodyMedium,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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

            if (query.isBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { recentSearchesState.clear() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Recent")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(recentSearchesState) { item ->
                        Text(
                            text = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    query = item
                                    recentSearchesState.remove(item)
                                    recentSearchesState.add(0, item)
                                },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

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
                        leadingIcon = { Icon(icons[cat]!!, contentDescription = null) },
                        label = { Text(cat) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun highlightQuery(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)

    val lowercaseText = text.lowercase()
    val lowercaseQuery = query.lowercase()
    val start = lowercaseText.indexOf(lowercaseQuery)
    if (start == -1) return AnnotatedString(text)

    val end = start + query.length
    val highlightColor = MaterialTheme.colorScheme.primary

    return buildAnnotatedString {
        append(text.substring(0, start))
        pushStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
        )
        append(text.substring(start, end))
        pop()
        append(text.substring(end))
    }
}
