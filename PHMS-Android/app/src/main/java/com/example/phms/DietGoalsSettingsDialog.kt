package com.example.phms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietGoalsSettingsDialog(
    userId: String,
    currentGoals: DietGoalDTO?,
    onSave: (DietGoalDTO) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var calorieGoal by remember { mutableStateOf((currentGoals?.calorieGoal ?: 2000).toString()) }
    var proteinGoal by remember { mutableStateOf((currentGoals?.proteinGoal ?: 75).toString()) }
    var fatGoal by remember { mutableStateOf((currentGoals?.fatGoal ?: 65).toString()) }
    var carbGoal by remember { mutableStateOf((currentGoals?.carbGoal ?: 300).toString()) }

    var calorieError by remember { mutableStateOf(false) }
    var proteinError by remember { mutableStateOf(false) }
    var fatError by remember { mutableStateOf(false) }
    var carbError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diet Goals Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Set your daily nutritional goals",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = calorieGoal,
                    onValueChange = {
                        calorieGoal = it
                        calorieError = it.toIntOrNull() == null
                    },
                    label = { Text("Daily Calorie Goal") },
                    isError = calorieError,
                    supportingText = {
                        if (calorieError) Text("Please enter a valid number")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = proteinGoal,
                    onValueChange = {
                        proteinGoal = it
                        proteinError = it.toIntOrNull() == null
                    },
                    label = { Text("Protein Goal (grams)") },
                    isError = proteinError,
                    supportingText = {
                        if (proteinError) Text("Please enter a valid number")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fatGoal,
                    onValueChange = {
                        fatGoal = it
                        fatError = it.toIntOrNull() == null
                    },
                    label = { Text("Fat Goal (grams)") },
                    isError = fatError,
                    supportingText = {
                        if (fatError) Text("Please enter a valid number")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = carbGoal,
                    onValueChange = {
                        carbGoal = it
                        carbError = it.toIntOrNull() == null
                    },
                    label = { Text("Carbohydrate Goal (grams)") },
                    isError = carbError,
                    supportingText = {
                        if (carbError) Text("Please enter a valid number")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate inputs
                    calorieError = calorieGoal.toIntOrNull() == null
                    proteinError = proteinGoal.toIntOrNull() == null
                    fatError = fatGoal.toIntOrNull() == null
                    carbError = carbGoal.toIntOrNull() == null

                    if (!calorieError && !proteinError && !fatError && !carbError) {
                        val goals = DietGoalDTO(
                            id = currentGoals?.id,
                            userId = userId,
                            calorieGoal = calorieGoal.toInt(),
                            proteinGoal = proteinGoal.toInt(),
                            fatGoal = fatGoal.toInt(),
                            carbGoal = carbGoal.toInt()
                        )

                        onSave(goals)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}