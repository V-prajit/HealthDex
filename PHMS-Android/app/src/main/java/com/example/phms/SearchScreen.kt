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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    userToken: String?,                                      // +assistant: added userToken
    onClose: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current                              // +assistant: for local notes
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var recentSearches by remember { mutableStateOf(listOf("sleep", "bp", "morning meds")) }

    // +assistant: load all notes and vitals
    var allNotes by remember { mutableStateOf(listOf<String>()) }
    var allVitals by remember { mutableStateOf(listOf<String>()) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text("Search notes or vitals...") },
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
                }

                if (filteredNotes.isEmpty() && filteredVitals.isEmpty()) {
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
    val categories = listOf("All", "Notes", "Vital", "Medication", "Diet")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
            FilterChip(
                selected = (selected == cat),
                onClick = { onSelect(cat) },
                label = { Text(cat) }
            )
        }
    }
}
