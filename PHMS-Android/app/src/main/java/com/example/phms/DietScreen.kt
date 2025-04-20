package com.example.phms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    userToken: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var diets by remember { mutableStateOf<List<DietDTO>>(emptyList()) }

    LaunchedEffect(userToken) {
        userToken?.let { uid ->
            DietRepository.fetchAll(uid) { fetched ->
                diets = fetched.orEmpty()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diet History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
        ) {
            items(diets) { diet ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Meal: ${diet.mealType}", style = MaterialTheme.typography.titleMedium)
                        Text("Calories: ${diet.calories}")
                        Text("When: ${diet.timestamp}")
                        diet.description?.let { note ->
                            Text("Notes: $note", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
