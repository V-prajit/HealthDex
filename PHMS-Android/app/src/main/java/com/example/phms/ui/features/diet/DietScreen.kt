package com.example.phms.ui.features.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Edit
import com.example.phms.data.model.Diet
import com.example.phms.data.model.DietGoalDTO
import com.example.phms.domain.repository.DietGoalRepository
import com.example.phms.domain.repository.DietRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietScreen(
    userId: String?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var diets by remember { mutableStateOf<List<Diet>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedDiet by remember { mutableStateOf<Diet?>(null) }

    var totalCalories by remember { mutableStateOf(0) }
    var totalProtein by remember { mutableStateOf(0) }
    var totalFats by remember { mutableStateOf(0) }
    var totalCarbs by remember { mutableStateOf(0) }

    var calorieGoal by remember { mutableStateOf(2000) }
    var proteinGoal by remember { mutableStateOf(75) }
    var fatGoal by remember { mutableStateOf(65) }
    var carbGoal by remember { mutableStateOf(300) }


    var dietGoals by remember { mutableStateOf<DietGoalDTO?>(null) }
    var showGoalsDialog by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(userId, selectedDate) {
        if (userId != null) {
            diets = DietRepository.getDiets(userId)

            // Filter diets for the selected date and calculate totals
            val todayDiets = diets.filter {
                try {
                    val entryDate = LocalDateTime.parse(it.timestamp).toLocalDate()
                    entryDate == selectedDate
                } catch (e: Exception) {
                    false
                }
            }

            totalCalories = todayDiets.sumOf { it.calories }
            totalProtein = todayDiets.sumOf { it.protein ?: 0 }
            totalFats = todayDiets.sumOf { it.fats ?: 0 }
            totalCarbs = todayDiets.sumOf { it.carbohydrates ?: 0 }
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            // Load diet goals
            val goals = DietGoalRepository.getDietGoals(userId)
            if (goals != null) {
                dietGoals = goals
                calorieGoal = goals.calorieGoal
                proteinGoal = goals.proteinGoal
                fatGoal = goals.fatGoal
                carbGoal = goals.carbGoal
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diet Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showGoalsDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goals")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedDiet = null
                    showDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Date selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectedDate = selectedDate.minusDays(1)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                }

                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = {
                    selectedDate = selectedDate.plusDays(1)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                }
            }

            // Nutrition summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Daily Summary",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calories progress
                    val calorieProgress = (totalCalories.toFloat() / calorieGoal).coerceIn(0f, 1f)
                    Text(
                        text = "Calories: $totalCalories / $calorieGoal kcal",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = calorieProgress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Macronutrients
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MacronutrientItem(
                            name = "Protein",
                            value = totalProtein,
                            goal = proteinGoal,
                            color = Color(0xFF66BB6A)  // Green
                        )

                        MacronutrientItem(
                            name = "Fats",
                            value = totalFats,
                            goal = fatGoal,
                            color = Color(0xFFFFB74D)  // Orange
                        )

                        MacronutrientItem(
                            name = "Carbs",
                            value = totalCarbs,
                            goal = carbGoal,
                            color = Color(0xFF42A5F5)  // Blue
                        )
                    }
                }
            }

            // Meals list
            Text(
                text = "Meals",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (diets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No meals recorded yet.\nTap + to add your first meal.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val filteredDiets = diets.filter {
                    try {
                        val entryDate = LocalDateTime.parse(it.timestamp).toLocalDate()
                        entryDate == selectedDate
                    } catch (e: Exception) {
                        false
                    }
                }

                if (filteredDiets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No meals recorded for this date.\nTap + to add a meal.",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredDiets) { diet ->
                            DietItem(
                                diet = diet,
                                onEdit = {
                                    selectedDiet = diet
                                    showDialog = true
                                },
                                onDelete = {
                                    scope.launch {
                                        diet.id?.let { id ->
                                            if (DietRepository.deleteDiet(id)) {
                                                diets = diets.filter { it.id != id }

                                                // Update totals
                                                totalCalories -= diet.calories
                                                totalProtein -= diet.protein ?: 0
                                                totalFats -= diet.fats ?: 0
                                                totalCarbs -= diet.carbohydrates ?: 0
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Show dialog when add/edit is requested
    if (showDialog) {
        DietDialog(
            diet = selectedDiet,
            userId = userId ?: return,
            onSave = { diet ->
                scope.launch {
                    if (diet.id == null) {
                        // Add new diet
                        val added = DietRepository.addDiet(diet)
                        if (added != null) {
                            diets = listOf(added) + diets

                            // Update totals if the meal is for today
                            val entryDate = LocalDateTime.parse(added.timestamp).toLocalDate()
                            if (entryDate == selectedDate) {
                                totalCalories += added.calories
                                totalProtein += added.protein ?: 0
                                totalFats += added.fats ?: 0
                                totalCarbs += added.carbohydrates ?: 0
                            }
                        }
                    } else {
                        // Update existing diet
                        if (DietRepository.updateDiet(diet)) {
                            // Find old diet to update totals
                            val oldDiet = diets.find { it.id == diet.id }
                            diets = diets.map { if (it.id == diet.id) diet else it }

                            // Update totals if the meal is for today
                            val entryDate = LocalDateTime.parse(diet.timestamp).toLocalDate()
                            if (entryDate == selectedDate && oldDiet != null) {
                                totalCalories = totalCalories - oldDiet.calories + diet.calories
                                totalProtein = totalProtein - (oldDiet.protein ?: 0) + (diet.protein ?: 0)
                                totalFats = totalFats - (oldDiet.fats ?: 0) + (diet.fats ?: 0)
                                totalCarbs = totalCarbs - (oldDiet.carbohydrates ?: 0) + (diet.carbohydrates ?: 0)
                            }
                        }
                    }
                    showDialog = false
                }
            },
            onCancel = {
                showDialog = false
            }
        )
    }
    if (showGoalsDialog) {
        DietGoalsSettingsDialog(
            userId = userId ?: return,
            currentGoals = dietGoals,
            onSave = { goals ->
                scope.launch {
                    val savedGoals = DietGoalRepository.setDietGoals(goals)
                    if (savedGoals != null) {
                        dietGoals = savedGoals
                        calorieGoal = savedGoals.calorieGoal
                        proteinGoal = savedGoals.proteinGoal
                        fatGoal = savedGoals.fatGoal
                        carbGoal = savedGoals.carbGoal
                    }
                    showGoalsDialog = false
                }
            },
            onDismiss = { showGoalsDialog = false }
        )
    }
}

@Composable
fun MacronutrientItem(
    name: String,
    value: Int,
    goal: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value\ng",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "$value/$goal g",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DietItem(
    diet: Diet,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timestamp = try {
        LocalDateTime.parse(diet.timestamp)
    } catch (e: Exception) {
        LocalDateTime.now()
    }
    val formatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Meal type indicator
                    val color = when (diet.mealType) {
                        "Breakfast" -> Color(0xFFFFC107)  // Amber
                        "Lunch" -> Color(0xFF4CAF50)      // Green
                        "Dinner" -> Color(0xFF3F51B5)     // Indigo
                        "Snack" -> Color(0xFFFF9800)      // Orange
                        else -> Color(0xFF9C27B0)         // Purple
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = diet.mealType,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = formatter.format(timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = diet.description ?: "No description",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${diet.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    diet.protein?.let {
                        Text(
                            text = "$it g P",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    diet.fats?.let {
                        Text(
                            text = "$it g F",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    diet.carbohydrates?.let {
                        Text(
                            text = "$it g C",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}