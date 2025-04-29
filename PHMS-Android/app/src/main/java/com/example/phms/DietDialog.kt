package com.example.phms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import com.example.phms.repository.NutritionRepository

private enum class DietEntryMode {
    SEARCH,
    RESULTS,
    MANUAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietDialog(
    diet: Diet? = null,
    userId: String,
    onSave: (Diet) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val mealTypes = listOf(stringResource(R.string.meal_type_breakfast), stringResource(R.string.meal_type_lunch), stringResource(R.string.meal_type_dinner), stringResource(R.string.meal_type_snack), stringResource(R.string.meal_type_other))

    var entryMode by remember { mutableStateOf(DietEntryMode.SEARCH) }

    var mealType by remember { mutableStateOf(diet?.mealType ?: mealTypes[0]) }
    var mealTypeExpanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf(diet?.description ?: "") }
    var calories by remember { mutableStateOf(diet?.calories?.toString() ?: "") }
    var protein by remember { mutableStateOf(diet?.protein?.toString() ?: "") }
    var fats by remember { mutableStateOf(diet?.fats?.toString() ?: "") }
    var carbs by remember { mutableStateOf(diet?.carbohydrates?.toString() ?: "") }
    var weight by remember { mutableStateOf(diet?.weight?.toString() ?: "") }

    val isEditing = diet != null
    if (isEditing && entryMode == DietEntryMode.SEARCH) {
        entryMode = DietEntryMode.MANUAL
    }


    val currentTimestamp = if (diet?.timestamp != null) {
        diet.timestamp
    } else {
        LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    }

    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var caloriesError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<FoodHit>>(emptyList()) }

    fun validateAndSave() {
        caloriesError = calories.toIntOrNull() == null && entryMode == DietEntryMode.MANUAL
        descriptionError = description.isBlank()

        if (caloriesError || descriptionError) {
            return
        }

        val newDiet = Diet(
            id = diet?.id,
            userId = userId,
            timestamp = currentTimestamp,
            mealType = mealType,
            calories = calories.toIntOrNull() ?: 0,
            description = description,
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

    fun performSearch() {

        if (description.isBlank()) {
            descriptionError = true
            return
        }
        descriptionError = false
        loading = true
        apiError = null
        searchResults = emptyList()
        scope.launch {
            val hits = NutritionRepository.searchFoodsByName(description)
            loading = false
            if (hits.isNotEmpty()) {
                searchResults = hits
                entryMode = DietEntryMode.RESULTS
            } else {

                apiError = context.getString(R.string.error_no_food_found, description)
                entryMode = DietEntryMode.MANUAL

                calories = ""
                protein = ""
                fats = ""
                carbs = ""
                weight = ""
            }
        }

    }

    fun selectFood(hit: FoodHit) {
        loading = true
        apiError = null
        entryMode = DietEntryMode.MANUAL
        scope.launch {
            val details = NutritionRepository.getFoodDetails(hit.fdcId)
            loading = false
            if (details != null) {
                description = hit.description
                calories = details.calories.toString()
                protein = details.protein.toString()
                fats = details.fat.toString()
                carbs = details.carbs.toString()
                weight = ""
            } else {
                apiError = context.getString(R.string.error_fetch_details, hit.description)
                calories = ""
                protein = ""
                fats = ""
                carbs = ""
                weight = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (isEditing) stringResource(R.string.edit_meal_title) else stringResource(R.string.add_meal_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = mealTypeExpanded,
                    onExpandedChange = { mealTypeExpanded = !mealTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = mealType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.meal_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = mealTypeExpanded, onDismissRequest = { mealTypeExpanded = false }) {
                        mealTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { mealType = type; mealTypeExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (it.isNotBlank()) descriptionError = false
                    },
                    label = { Text(stringResource(R.string.food_description_label)) },
                    isError = descriptionError,
                    supportingText = { if (descriptionError) Text(stringResource(R.string.error_description_empty)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (description.isNotEmpty()) {
                            IconButton(onClick = {
                                description = ""
                                searchResults = emptyList()
                                apiError = null
                                entryMode = DietEntryMode.SEARCH
                                calories = ""
                                protein = ""
                                fats = ""
                                carbs = ""
                                weight = ""
                            }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.clear_description))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = entryMode == DietEntryMode.SEARCH && !isEditing) {
                    Button(
                        onClick = { performSearch() },
                        enabled = description.isNotBlank() && !loading,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.searching_food_button))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.search_food_button))
                        }
                    }
                }

                AnimatedVisibility(visible = entryMode == DietEntryMode.SEARCH && !isEditing) {
                    TextButton(
                        onClick = { entryMode = DietEntryMode.MANUAL },
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    ) {
                        Text(stringResource(R.string.enter_manually_button))
                    }
                }

                apiError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp))
                }


                AnimatedVisibility(visible = entryMode == DietEntryMode.RESULTS && searchResults.isNotEmpty()) {
                    Column {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(stringResource(R.string.select_match_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                        ) {
                            items(searchResults) { hit ->
                                Text(
                                    text = hit.description,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !loading) { selectFood(hit) }
                                        .padding(12.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { entryMode = DietEntryMode.MANUAL },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.enter_manually_instead_button))
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                AnimatedVisibility(visible = entryMode == DietEntryMode.MANUAL) {
                    Column {
                        if (entryMode == DietEntryMode.SEARCH && apiError != null && !apiError!!.contains("matching")) {
                            Divider(modifier = Modifier.padding(vertical=8.dp))
                            Text(stringResource(R.string.manual_entry_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = calories,
                            onValueChange = {
                                calories = it
                                caloriesError = it.toIntOrNull() == null
                            },
                            label = { Text(stringResource(R.string.calories_label)) },
                            isError = caloriesError,
                            supportingText = { if (caloriesError) Text(stringResource(R.string.error_calories_nan)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text(stringResource(R.string.protein_g_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(value = fats, onValueChange = { fats = it }, label = { Text(stringResource(R.string.fats_g_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text(stringResource(R.string.carbs_g_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text(stringResource(R.string.weight_g_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && entryMode == DietEntryMode.MANUAL && description.isNotBlank() && calories.toIntOrNull() != null,
                onClick = { validateAndSave() }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}