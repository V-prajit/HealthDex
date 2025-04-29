package com.example.phms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        title = { Text(stringResource(R.string.diet_goals_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    stringResource(R.string.diet_goals_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = calorieGoal,
                    onValueChange = {
                        calorieGoal = it
                        calorieError = it.toIntOrNull() == null
                    },
                    label = { Text(stringResource(R.string.daily_calorie_goal_label)) },
                    isError = calorieError,
                    supportingText = {
                        if (calorieError) Text(stringResource(R.string.error_invalid_number))
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
                    label = { Text(stringResource(R.string.protein_goal_label)) },
                    isError = proteinError,
                    supportingText = {
                        if (proteinError) Text(stringResource(R.string.error_invalid_number))
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
                    label = { Text(stringResource(R.string.fat_goal_label)) },
                    isError = fatError,
                    supportingText = {
                        if (fatError) Text(stringResource(R.string.error_invalid_number))
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
                    label = { Text(stringResource(R.string.carb_goal_label)) },
                    isError = carbError,
                    supportingText = {
                        if (carbError) Text(stringResource(R.string.error_invalid_number))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {

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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}