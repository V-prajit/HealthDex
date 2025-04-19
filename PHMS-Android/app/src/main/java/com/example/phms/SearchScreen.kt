package com.example.phms

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onClose: () -> Unit, onBackClick: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var recentSearches by remember { mutableStateOf(listOf("sleep", "bp", "morning meds")) }
    var selectedCategory by remember { mutableStateOf("All") }  // +assistant: added for category filtering

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
            // +assistant: Add "Search results" section that shows when query is not empty
            if (query.isNotEmpty()) {
                Text("Search results for \"$query\"", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // This is a placeholder for search results
                // In a real implementation, you would fetch and display actual results here
                if (selectedCategory == "All" || selectedCategory == "Notes") {
                    Text(
                        text = "No results found in Notes",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (selectedCategory == "All" || selectedCategory == "Vital") {
                    Text(
                        text = "No results found in Vitals",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = {
                    // +assistant: implemented clear recent searches
                    recentSearches = emptyList()
                }) {
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
                            .clickable {
                                query = item
                                // +assistant: when clicking a recent search, add it to recents if not already there
                                if (!recentSearches.contains(item)) {
                                    recentSearches = listOf(item) + recentSearches
                                }
                            },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // search by category
            Text("Search by category", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // +assistant: Added "All" category chip
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { selectedCategory = "All" },
                    label = { Text("All") },
                    modifier = Modifier,
                    enabled = selectedCategory != "All"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { selectedCategory = "Notes" },
                        leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                        label = { Text("Notes") },
                        enabled = selectedCategory != "Notes"
                    )
                    AssistChip(
                        onClick = { selectedCategory = "Vital" },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Vital") },
                        enabled = selectedCategory != "Vital"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { selectedCategory = "Medication" },
                        leadingIcon = { Icon(Icons.Default.LocalPharmacy, contentDescription = null) },
                        label = { Text("Medication") },
                        enabled = selectedCategory != "Medication"
                    )
                    AssistChip(
                        onClick = { selectedCategory = "Diet" },
                        leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        label = { Text("Diet") },
                        enabled = selectedCategory != "Diet"
                    )
                }
            }
        }
    }
}