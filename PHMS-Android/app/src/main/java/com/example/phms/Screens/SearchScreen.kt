package com.example.phms.Screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    val allTagsStr = stringResource(R.string.all_tags) // Get string resource here
    val notesStr = stringResource(R.string.notes)
    val vitalsStr = stringResource(R.string.vitals_tab)
    val appointmentsStr = stringResource(R.string.appointments)
    val medicationStr = stringResource(R.string.medication)
    val dietStr = stringResource(R.string.diet)
    val doctorStr = stringResource(R.string.doctor)

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(allTagsStr) }

    val recentSearchesState = remember { mutableStateListOf<String>() }


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


    val allDiets by produceState(initialValue = emptyList<Diet>(), userToken) {
        value = userToken?.let { DietRepository.getDiets(it) } ?: emptyList()
    }

    val scope = rememberCoroutineScope()

    val filteredNotes by remember(allNotes, query, selectedCategory, allTagsStr, notesStr) { // Pass strings to remember
        derivedStateOf {
            allNotes.filter { note ->
                note.contains(query, ignoreCase = true) &&
                        (selectedCategory == allTagsStr || selectedCategory == notesStr) // Use variables
            }
        }
    }
    val filteredVitals by remember(allVitals, query, selectedCategory, allTagsStr, vitalsStr) { // Pass strings to remember
        derivedStateOf {
            allVitals.filter { vital ->
                vital.contains(query, ignoreCase = true) &&
                        (selectedCategory == allTagsStr || selectedCategory == vitalsStr) // Use variables
            }
        }
    }
    val filteredAppointments by remember(allAppointments, query, selectedCategory, allTagsStr, appointmentsStr) { // Pass strings to remember
        derivedStateOf {
            allAppointments.filter { appt ->
                (appt.doctorName?.contains(query, ignoreCase = true) ?: false ||
                        appt.reason.contains(query, ignoreCase = true)) &&
                        (selectedCategory == allTagsStr || selectedCategory == appointmentsStr) // Use variables
            }
        }
    }


    val filteredMedications by remember(allMedications, query, selectedCategory, allTagsStr, medicationStr) { // Pass strings to remember
        derivedStateOf {
            allMedications.filter { med ->
                (med.name.contains(query, ignoreCase = true) ||
                        med.category.contains(query, ignoreCase = true)) &&
                        (selectedCategory == allTagsStr || selectedCategory == medicationStr) // Use variables
            }
        }
    }


    val filteredDiets by remember(allDiets, query, selectedCategory, allTagsStr, dietStr) { // Pass strings to remember
        derivedStateOf {
            allDiets.filter { diet ->
                (diet.mealType.contains(query, ignoreCase = true) ||
                        (diet.description?.contains(query, ignoreCase = true) ?: false)) &&
                        (selectedCategory == allTagsStr || selectedCategory == dietStr) // Use variables
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
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                if (query.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.search_results_for, query),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }


                    if (filteredNotes.isNotEmpty()) {
                        item { Text(notesStr + ":", style = MaterialTheme.typography.titleSmall) } // Use variable
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


                    if (filteredVitals.isNotEmpty()) {
                        item { Text(vitalsStr + ":", style = MaterialTheme.typography.titleSmall) } // Use variable
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


                    if (filteredAppointments.isNotEmpty()) {
                        item { Text(appointmentsStr + ":", style = MaterialTheme.typography.titleSmall) } // Use variable
                        items(filteredAppointments, key = { "appt_${it.id}" }) { appt ->
                            val label = stringResource(R.string.search_appointment_label, appt.date, appt.time, appt.doctorName ?: doctorStr, appt.reason)
                            SearchResultCard(
                                text = highlightQuery(label, query),
                                icon = Icons.Default.EventNote,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToAppointments()
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }


                    if (filteredMedications.isNotEmpty()) {
                        item { Text(medicationStr + ":", style = MaterialTheme.typography.titleSmall) } // Use variable
                        items(filteredMedications, key = { "med_${it.id ?: it.hashCode()}" }) { med ->
                            val label = stringResource(R.string.search_medication_label, med.name, med.dosage)
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


                    if (filteredDiets.isNotEmpty()) {
                        item { Text(dietStr + ":", style = MaterialTheme.typography.titleSmall) } // Use variable
                        items(filteredDiets, key = { "diet_${it.id ?: it.hashCode()}" }) { diet ->
                            val label = stringResource(R.string.search_diet_label, diet.mealType, diet.description ?: "", diet.calories)
                            SearchResultCard(
                                text = highlightQuery(label, query),
                                icon = Icons.Default.Restaurant,
                                onClick = {
                                    scope.launch { updateRecentSearches(query, recentSearchesState) }
                                    onNavigateToDiet()
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
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


                if (query.isBlank()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.recent_searches_title), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { recentSearchesState.clear() }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.clear_recent_desc))
                            }
                        }
                    }

                    if (recentSearchesState.isNotEmpty()) {
                        items(recentSearchesState, key = { "recent_$it" }) { item ->
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = item
                                    }
                                    .padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        item { Text(stringResource(R.string.no_recent_searches), style = MaterialTheme.typography.bodyMedium) }
                    }


                    item { Spacer(modifier = Modifier.height(16.dp)) }


                    item { Text(stringResource(R.string.search_by_category_title), style = MaterialTheme.typography.titleMedium) }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                        CategoryChips(
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            categories = listOf(allTagsStr, notesStr, vitalsStr, appointmentsStr, medicationStr, dietStr) // Pass fetched strings
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    selected: String,
    onSelect: (String) -> Unit,
    categories: List<String>
) {
    val context = LocalContext.current

    val icons = mapOf(
        stringResource(R.string.all_tags) to Icons.Default.Search,
        stringResource(R.string.notes) to Icons.Default.Note,
        stringResource(R.string.vitals_tab) to Icons.Default.Favorite,
        stringResource(R.string.appointments) to Icons.Default.EventNote,
        stringResource(R.string.medication) to Icons.Default.LocalPharmacy,
        stringResource(R.string.diet) to Icons.Default.Restaurant
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = (selected == cat),
                onClick = { onSelect(cat) },
                leadingIcon = { icons[cat]?.let { Icon(it, contentDescription = null) } },
                label = { Text(cat) }
            )
        }
    }
}


private fun updateRecentSearches(query: String, recentSearchesState: SnapshotStateList<String>) {
    if (query.isNotBlank()) {
        recentSearchesState.remove(query)
        recentSearchesState.add(0, query)

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
            .padding(vertical = 4.dp)
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                    color = MaterialTheme.colorScheme.primary
                ),
                start = startIndex,
                end = endIndex
            )
            startIndex = text.indexOf(query, startIndex + 1, ignoreCase = true)
        }
    }
    return annotatedString
}