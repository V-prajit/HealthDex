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
    val context = LocalContext.current
    var diets by remember { mutableStateOf<List<DietDTO>>(emptyList()) }
    var showMealDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    // Persistent goals
    var calorieGoal by remember { mutableStateOf(readPref(context, "calorieGoal")?.toIntOrNull()) }
    var proteinGoal by remember { mutableStateOf(readPref(context, "proteinGoal")?.toIntOrNull()) }
    var fatGoal by remember { mutableStateOf(readPref(context, "fatGoal")?.toIntOrNull()) }
    var carbGoal by remember { mutableStateOf(readPref(context, "carbGoal")?.toIntOrNull()) }

    // Meal dialog input states moved outside so they are accessible
    var mealType by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LaunchedEffect(userToken) {
        Log.d("DietScreen", "Loading diets for userToken: $userToken")
        userToken?.let { uid ->
            DietRepository.fetchAll(uid) { fetched ->
                Log.d("DietScreen", "Fetched diets: $fetched")
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
            if (diets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No meals found. Add one using the + button.")
                }
            } else {
                Text("Goal Progress", style = MaterialTheme.typography.titleMedium)

                val caloriesSoFar = diets.sumOf { it.calories }
                val proteinSoFar = diets.sumOf { it.protein ?: 0 }
                val fatSoFar = diets.sumOf { it.fats ?: 0 }
                val carbsSoFar = diets.sumOf { it.carbohydrates ?: 0 }

                GoalBar("Calories", caloriesSoFar, calorieGoal)
                GoalBar("Protein (g)", proteinSoFar, proteinGoal)
                GoalBar("Fats (g)", fatSoFar, fatGoal)
                GoalBar("Carbs (g)", carbsSoFar, carbGoal)

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(diets) { diet ->
                        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Meal: ${diet.mealType}", style = MaterialTheme.typography.titleMedium)
                                Text("Calories: ${diet.calories}")
                                Text("Protein: ${diet.protein ?: "-"}g, Fats: ${diet.fats ?: "-"}g, Carbs: ${diet.carbohydrates ?: "-"}g")
                                Text("Weight: ${diet.weight ?: "-"}g")
                                Text("When: ${diet.timestamp}")
                                diet.description?.let { Text("Notes: $it") }
                            }
                        }
                    }
                }
            }
        }

        if (showMealDialog) {
            AlertDialog(
                onDismissRequest = { showMealDialog = false },
                title = { Text("Add Meal") },
                text = {
                    Column {
                        OutlinedTextField(value = mealType, onValueChange = { mealType = it }, label = { Text("Meal Type") })
                        OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = fats, onValueChange = { fats = it }, label = { Text("Fats") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs") }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
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
                            description = description.ifBlank { null },
                            protein = protein.toIntOrNull(),
                            fats = fats.toIntOrNull(),
                            carbohydrates = carbs.toIntOrNull(),
                            weight = weight.toIntOrNull(),
                            calorieGoal = null,
                            proteinGoal = null,
                            fatGoal = null,
                            carbGoal = null
                        )
                        DietRepository.add(dto) {
                            DietRepository.fetchAll(userToken!!) { diets = it.orEmpty() }
                        }
                        showMealDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showMealDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showGoalDialog) {
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = { Text("Set Daily Goals") },
                text = {
                    Column {
                        GoalInput("Calories", calorieGoal) { calorieGoal = it }
                        GoalInput("Protein", proteinGoal) { proteinGoal = it }
                        GoalInput("Fats", fatGoal) { fatGoal = it }
                        GoalInput("Carbs", carbGoal) { carbGoal = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        writePref(context, "calorieGoal", calorieGoal?.toString())
                        writePref(context, "proteinGoal", proteinGoal?.toString())
                        writePref(context, "fatGoal", fatGoal?.toString())
                        writePref(context, "carbGoal", carbGoal?.toString())
                        showGoalDialog = false
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}


@Composable
fun GoalBar(label: String, value: Int, goal: Int?) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: $value / ${goal ?: "-"}")
        LinearProgressIndicator(
            progress = if (goal != null && goal > 0) value / goal.toFloat().coerceAtMost(1f) else 0f,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}

@Composable
fun GoalInput(label: String, value: Int?, onValueChange: (Int?) -> Unit) {
    OutlinedTextField(
        value = value?.toString() ?: "",
        onValueChange = { onValueChange(it.toIntOrNull()) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

fun writePref(context: Context, key: String, value: String?) {
    context.getSharedPreferences("diet_prefs", Context.MODE_PRIVATE)
        .edit().putString(key, value).apply()
}

fun readPref(context: Context, key: String): String? {
    return context.getSharedPreferences("diet_prefs", Context.MODE_PRIVATE).getString(key, null)
}