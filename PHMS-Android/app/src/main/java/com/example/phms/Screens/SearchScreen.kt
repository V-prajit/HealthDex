package com.example.phms.Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.phms.Appointment
import com.example.phms.Diet
import com.example.phms.Medication
import com.example.phms.R
import com.example.phms.VitalRepository
import com.example.phms.repository.AppointmentRepository
import com.example.phms.repository.DietRepository
import com.example.phms.repository.MedicationRepository
import com.example.phms.repository.NotesRepository
import com.example.phms.repository.NotesRepositoryBackend
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    userToken: String?,
    onClose: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToDiet: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val recentSearchesState = remember { mutableStateListOf<String>() }

    // Use produceState for cleaner async data loading, including Meds and Diet
    val allNotes by produceState(initialValue = emptyList<String>(), userToken) {
        value = if (!userToken.isNullOrEmpty()) {
            NotesRepositoryBackend.getNotes(userToken)
        } else {
            NotesRepository.getNotes(context)
        }
    }

    val allVitals by produceState(initialValue = emptyList<String>(), userToken) {
        value = userToken?.let {
            VitalRepository.getVitals(it).map { v -> "${v.type}: ${v.value} ${v.unit}" }
        } ?: emptyList()
    }

    val allAppointments by produceState(initialValue = emptyList<Appointment>(), userToken) {
        value = userToken?.let { AppointmentRepository.getUpcomingAppointments(it) } ?: emptyList()
    }

    // fetch medications
    val allMedications by produceState(initialValue = emptyList<Medication>(), userToken) {
        if (userToken != null) {
            value = suspendCoroutine { continuation ->
                MedicationRepository.fetchAll(userToken) { fetchedMeds ->
                    continuation.resume(fetchedMeds ?: emptyList())
                }
            }
        } else {
            value = emptyList()
        }
    }

    // fetching diet entries
    val allDiets by produceState(initialValue = emptyList<Diet>(), userToken) {
        value = userToken?.let { DietRepository.getDiets(it) } ?: emptyList()
    }

    val scope = rememberCoroutineScope()

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
                (appt.doctorName?.contains(query, ignoreCase = true) ?: false ||
                        appt.reason.contains(query, ignoreCase = true)) && // Also search reason
                        (selectedCategory == "All" || selectedCategory == "Appointments")
            }
        }
    }

    // filter meds
    val filteredMedications by remember(allMedications, query, selectedCategory) {
        derivedStateOf {
            allMedications.filter { med ->
                (med.name.contains(query, ignoreCase = true) ||
                        med.category.contains(query, ignoreCase = true)) && // Search name and category
                        (selectedCategory == "All" || selectedCategory == "Medication")
            }
        }
    }

    // filtering diet entries
    val filteredDiets by remember(allDiets, query, selectedCategory) {
        derivedStateOf {
            allDiets.filter { diet ->
                (diet.mealType.contains(query, ignoreCase = true) ||
                        (diet.description?.contains(query, ignoreCase = true) ?: false)) && // Search type and description
                        (selectedCategory == "All" || selectedCategory == "Diet")
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
                        placeholder = { Text(stringResource(R.string.search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp) // Apply horizontal padding here
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between sections/items
            ) {

                // Search Results Section (only shown when query is not blank)
                if (query.isNotEmpty()) {
                    item {
                        Text(
                            "Search results for \"$query\"",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp) // Add padding below title
                        )
                    }

                    // Notes Results
                    if (filteredNotes.isNotEmpty()) {
                        item { Text("Notes:", style = MaterialTheme.typography.titleSmall) }
                        items(filteredNotes, key = { "note_${it.hashCode()}" }) { note ->
                            SearchResultCard(
                                text = highlightQuery(note, query),
                                icon = Icons.Default.Note,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToNotes()
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Vitals Results
                    if (filteredVitals.isNotEmpty()) {
                        item { Text("Vitals:", style = MaterialTheme.typography.titleSmall) }
                        items(filteredVitals, key = { "vital_${it.hashCode()}" }) { vital ->
                            SearchResultCard(
                                text = highlightQuery(vital, query),
                                icon = Icons.Default.Favorite,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToVitals()
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Appointments Results
                    if (filteredAppointments.isNotEmpty()) {
                        item { Text("Appointments:", style = MaterialTheme.typography.titleSmall) }
                        items(filteredAppointments, key = { "appt_${it.id}" }) { appt ->
                            val label = "${appt.date} ${appt.time} with ${appt.doctorName ?: "Doctor"} - ${appt.reason}"
                            SearchResultCard(
                                text = highlightQuery(label, query),
                                icon = Icons.Default.EventNote,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToAppointments()
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) } // Spacer after section
                    }

                    // medications results
                    if (filteredMedications.isNotEmpty()) {
                        item { Text("Medications:", style = MaterialTheme.typography.titleSmall) }
                        items(filteredMedications, key = { "med_${it.id ?: it.hashCode()}" }) { med ->
                            val label = "${med.name} (${med.dosage})"
                            SearchResultCard(
                                text = highlightQuery(label, query),
                                icon = Icons.Default.LocalPharmacy,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToMedications()
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Diet Results
                    if (filteredDiets.isNotEmpty()) {
                        item { Text("Diet:", style = MaterialTheme.typography.titleSmall) }
                        items(filteredDiets, key = { "diet_${it.id ?: it.hashCode()}" }) { diet ->
                            val label = "${diet.mealType}: ${diet.description ?: ""} (${diet.calories} kcal)" // Example label
                            SearchResultCard(
                                text = highlightQuery(label, query),
                                icon = Icons.Default.Restaurant,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToDiet() // Use the new navigation function
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) } // Spacer after section
                    }


                    if (filteredNotes.isEmpty() && filteredVitals.isEmpty() && filteredAppointments.isEmpty() && filteredMedications.isEmpty() && filteredDiets.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_results_found),
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Recent Searches and Categories Section
                if (query.isBlank()) {
                    item {
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
                    }
                    // Recent Searches List
                    if (recentSearchesState.isNotEmpty()) {
                        items(recentSearchesState, key = { "recent_$it" }) { item ->
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = item // Set query on click
                                    }
                                    .padding(vertical = 8.dp), // Add padding
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        item { Text("No recent searches", style = MaterialTheme.typography.bodyMedium) }
                    }


                    item { Spacer(modifier = Modifier.height(16.dp)) } // Spacer before categories

                    // Categories Section
                    item { Text("Search by category", style = MaterialTheme.typography.titleMedium) }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { CategoryChips(selectedCategory) { selectedCategory = it } }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) } // Bottom padding
            }
        }
    }
}

// function to update recent searches
private fun updateRecentSearches(query: String, recentSearchesState: SnapshotStateList<String>) {
    if (query.isNotBlank()) {
        recentSearchesState.remove(query) //avoding duplicates and move to top
        recentSearchesState.add(0, query) //adding at top
        // limit the size of recent searches
        while (recentSearchesState.size > 5) {
            recentSearchesState.removeAt(recentSearchesState.lastIndex)
        }
    }
}

@Composable
private fun SearchResultCard(text: AnnotatedString, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Adding pading between cards
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, // Limit lines
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    selected: String,
    onSelect: (String) -> Unit
) {
    //categories
    val categories = listOf("All", "Notes", "Vital", "Appointments", "Medication", "Diet")
    val icons = mapOf(
        "All" to Icons.Default.Search,
        "Notes" to Icons.Default.Note,
        "Vital" to Icons.Default.Favorite,
        "Appointments" to Icons.Default.EventNote,
        "Medication" to Icons.Default.LocalPharmacy,
        "Diet" to Icons.Default.Restaurant
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Horizontal spacing
        verticalArrangement = Arrangement.spacedBy(8.dp) // Vertical spacing for wrapped rows
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = (selected == cat),
                onClick = { onSelect(cat) },
                leadingIcon = { Icon(icons[cat]!!, contentDescription = null) },
                label = { Text(cat) }
            )
        }
    }
}

@Composable
private fun highlightQuery(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)

    val annotatedString = buildAnnotatedString {
        append(text)
        var startIndex = text.indexOf(query, ignoreCase = true)
        while (startIndex != -1) {
            val endIndex = startIndex + query.length
            addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // Highlight color
                ),
                start = startIndex,
                end = endIndex
            )
            startIndex = text.indexOf(query, startIndex + 1, ignoreCase = true)
        }
    }
    return annotatedString
}