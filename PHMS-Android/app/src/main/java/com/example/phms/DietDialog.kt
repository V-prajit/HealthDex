package com.example.phms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietDialog(
    diet: Diet? = null,
    userId: String,
    onSave: (Diet) -> Unit,
    onCancel: () -> Unit
) {
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Other")

    var mealType by remember { mutableStateOf(diet?.mealType ?: mealTypes[0]) }
    var mealTypeExpanded by remember { mutableStateOf(false) }

    var calories by remember { mutableStateOf(diet?.calories?.toString() ?: "") }
    var description by remember { mutableStateOf(diet?.description ?: "") }
    var protein by remember { mutableStateOf(diet?.protein?.toString() ?: "") }
    var fats by remember { mutableStateOf(diet?.fats?.toString() ?: "") }
    var carbs by remember { mutableStateOf(diet?.carbohydrates?.toString() ?: "") }
    var weight by remember { mutableStateOf(diet?.weight?.toString() ?: "") }

    var caloriesError by remember { mutableStateOf(false) }

    val currentTimestamp = if (diet?.timestamp != null) {
        diet.timestamp
    } else {
        LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (diet == null) "Add Meal" else "Edit Meal") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Meal type dropdown
                ExposedDropdownMenuBox(
                    expanded = mealTypeExpanded,
                    onExpandedChange = { mealTypeExpanded = !mealTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = mealType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Meal Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = mealTypeExpanded,
                        onDismissRequest = { mealTypeExpanded = false }
                    ) {
                        mealTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    mealType = type
                                    mealTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Calories
                OutlinedTextField(
                    value = calories,
                    onValueChange = {
                        calories = it
                        caloriesError = it.toIntOrNull() == null
                    },
                    label = { Text("Calories") },
                    isError = caloriesError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Macronutrients - Row with 3 fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it },
                        label = { Text("Fats (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weight
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (calories.toIntOrNull() == null) {
                        caloriesError = true
                        return@Button
                    }

                    val newDiet = Diet(
                        id = diet?.id,
                        userId = userId,
                        timestamp = currentTimestamp,
                        mealType = mealType,
                        calories = calories.toInt(),
                        description = description.takeIf { it.isNotBlank() },
                        protein = protein.toIntOrNull(),
                        fats = fats.toIntOrNull(),
                        carbohydrates = carbs.toIntOrNull(),
                        weight = weight.toIntOrNull(),
                        calorieGoal = diet?.calorieGoal,
                        proteinGoal = diet?.proteinGoal,
                        fatGoal = diet?.fatGoal,
                        carbGoal = diet?.carbGoal
                    )

                    onSave(newDiet)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}