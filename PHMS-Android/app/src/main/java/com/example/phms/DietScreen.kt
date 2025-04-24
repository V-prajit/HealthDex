package com.example.phms

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    userToken: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var diets by remember { mutableStateOf<List<DietDTO>>(emptyList()) }
    var showMealDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var calorieGoal by remember { mutableStateOf(0) }
    var proteinGoal by remember { mutableStateOf(0) }
    var fatGoal by remember { mutableStateOf(0) }
    var carbGoal by remember { mutableStateOf(0) }
    var mealType by remember { mutableStateOf("Breakfast") }
    var calories by remember { mutableStateOf("") }
    var carbohydrates by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = { showGoalDialog = true }) {
                    Text("🎯")
                }
                FloatingActionButton(onClick = { showMealDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Meal")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).padding(16.dp)) {
            if (showMealDialog) {
                AlertDialog(
                    onDismissRequest = { showMealDialog = false },
                    title = { Text("Add Meal") },
                    text = {
                        val mealOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                        var expanded by remember { mutableStateOf(false) }

                        Column {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = mealType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Meal Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    mealOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                mealType = option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = carbohydrates, onValueChange = { carbohydrates = it }, label = { Text("Carbohydrates") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = fats, onValueChange = { fats = it }, label = { Text("Fats") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notes") })
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val dto = DietDTO(
                                id = null,
                                userId = userToken ?: return@TextButton,
                                timestamp = DateFormat.format("yyyy-MM-dd'T'HH:mm:ss", Date()).toString(),
                                mealType = mealType,
                                calories = calories.toIntOrNull() ?: 0,
                                carbohydrates = carbohydrates.toIntOrNull(),
                                protein = protein.toIntOrNull(),
                                fats = fats.toIntOrNull(),
                                weight = weight.toIntOrNull(),
                                description = description.ifBlank { null }
                            )
                            DietRepository.add(dto) {
                                userToken?.let { DietRepository.fetchAll(it) { diets = it.orEmpty() } }
                            }
                            showMealDialog = false
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMealDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

fun writePref(context: Context, key: String, value: String?) {
    context.getSharedPreferences("diet_prefs", Context.MODE_PRIVATE)
        .edit().putString(key, value).apply()
}

fun readPref(context: Context, key: String): String? {
    return context.getSharedPreferences("diet_prefs", Context.MODE_PRIVATE).getString(key, null)
}
