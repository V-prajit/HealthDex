package com.example.phms.Screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.phms.Appointment
import com.example.phms.R
import com.example.phms.repository.AppointmentRepository
import com.example.phms.repository.DietGoalRepository
import com.example.phms.repository.DietRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar

@Composable
fun HomeScreen(
    firstName: String?,
    onSettingsClick: () -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToDiet: () -> Unit,
    onNavigateToDoctors: () -> Unit
) {

    val context = LocalContext.current
    var upcomingAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var latestAppointment by remember { mutableStateOf<Appointment?>(null) } // State for the grid card

    // States for Diet Summary Card
    var todaysCalories by remember { mutableStateOf(0) }
    var todaysProtein by remember { mutableStateOf(0) }
    var todaysFat by remember { mutableStateOf(0) }
    var todaysCarbs by remember { mutableStateOf(0) }
    var calorieGoal by remember { mutableStateOf(2000) }
    var proteinGoal by remember { mutableStateOf(75) }
    var fatGoal by remember { mutableStateOf(65) }
    var carbGoal by remember { mutableStateOf(300) }
    var userId by remember { mutableStateOf<String?>(null) }

    val motivationalQuotes = stringArrayResource(id = R.array.motivational_quotes)
    var currentQuote by remember {
        mutableStateOf(if (motivationalQuotes.isNotEmpty()) motivationalQuotes.random() else "")
    }

    Log.d("HomeScreen", "Composing HomeScreen with firstName: $firstName")

    // Fetch User ID from SharedPreferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = prefs.getString("LAST_USER_UID", null)
        if (motivationalQuotes.isNotEmpty()) {
            currentQuote = motivationalQuotes.random()
        }
    }

    // Fetch Appointment and Diet data when userId is available
    LaunchedEffect(userId) {
        userId?.let { uid ->
            if (uid.isNotEmpty()) {
                launch {
                    // Fetch appointments
                    val upcoming = AppointmentRepository.getUpcomingAppointments(uid)
                    upcomingAppointments = upcoming // For the bottom card
                    latestAppointment = upcoming.firstOrNull() // For the grid card

                    // Fetch diet goals
                    val goals = DietGoalRepository.getDietGoals(uid)
                    goals?.let {
                        calorieGoal = it.calorieGoal
                        proteinGoal = it.proteinGoal
                        fatGoal = it.fatGoal
                        carbGoal = it.carbGoal
                    }

                    val today = LocalDate.now()
                    val allDiets = DietRepository.getDiets(uid)
                    val todaysDiets = allDiets.filter {
                        try {
                            LocalDateTime.parse(it.timestamp).toLocalDate() == today
                        } catch (e: Exception) {
                            false
                        }
                    }
                    todaysCalories = todaysDiets.sumOf { it.calories }
                    todaysProtein = todaysDiets.sumOf { it.protein ?: 0 }
                    todaysFat = todaysDiets.sumOf { it.fats ?: 0 }
                    todaysCarbs = todaysDiets.sumOf { it.carbohydrates ?: 0 }
                }
            }
        }
    }


    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingBaseText = when (hour) {
        in 6..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        else -> stringResource(R.string.good_evening)
    }
    val greetingText = greetingBaseText + (firstName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: "")
    Log.d("HomeScreen", "Calculated greetingText: $greetingText")

    val imageUrl = when (hour) {
        in 6..11  -> "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?ixlib=rb-4.0.3&auto=format&fit=crop&w=1080&q=80"
        in 12..16 -> "https://images.unsplash.com/photo-1506744038136-46273834b3fb?ixlib=rb-4.0.3&auto=format&fit=crop&w=1080&q=80"
        in 17..19 -> "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?ixlib=rb-4.0.3&auto=format&fit=crop&w=1080&q=80"
        else      -> "https://images.unsplash.com/photo-1507400492013-162706c8c05e?ixlib=rb-4.0.3&auto=format&fit=crop&w=1080&q=80"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Quote and Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentQuote,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            IconButton(onClick = {
                Log.d("HomeScreen", "Settings icon clicked")
                onSettingsClick()
            }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxSize(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onNavigateToSearch() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Greeting Image Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(0.dp))
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = greetingText,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Access Section
        Text(
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessCard(
                text = stringResource(R.string.add_note),
                icon = Icons.Filled.NoteAdd,
                onClick = onNavigateToNotes,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                text = stringResource(R.string.add_vital),
                icon = Icons.Filled.MonitorHeart,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                text = stringResource(R.string.chatbot),
                icon = Icons.Default.Chat,
                onClick = onNavigateToChatbot,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Health Dashboard Grid
        Text(
            text = stringResource(R.string.health_dashboard),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp), // Adjust height based on card size
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false // Grid itself shouldn't scroll
        ) {
            item {
                DietSummaryCard(
                    calories = todaysCalories,
                    calorieGoal = calorieGoal,
                    protein = todaysProtein,
                    proteinGoal = proteinGoal,
                    fat = todaysFat,
                    fatGoal = fatGoal,
                    carbs = todaysCarbs,
                    carbGoal = carbGoal,
                    onClick = onNavigateToDiet
                )
            }
            item {
                AppointmentSummaryCard(
                    appointment = latestAppointment,
                    onClick = onNavigateToAppointments
                )
            }
            item {
                DashboardCard(
                    text = stringResource(R.string.medications),
                    icon = Icons.Default.LocalPharmacy,
                    onClick = onNavigateToMedications
                )
            }
            item {
                DashboardCard(
                    text = stringResource(R.string.doctors),
                    icon = Icons.Default.MedicalServices,
                    onClick = onNavigateToDoctors
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (upcomingAppointments.isNotEmpty()) {
            Text(
                text = stringResource(R.string.upcoming_appointments),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Show only the first upcoming appointment here
                val nextAppt = upcomingAppointments.firstOrNull()
                nextAppt?.let { appt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAppointments() },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = formatDate(appt.date),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${appt.time} (${appt.duration} min)", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = appt.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appt.doctorName ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                            }
                            appt.reason.takeIf { it.isNotBlank() }?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun QuickAccessCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DashboardCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppointmentSummaryCard(appointment: Appointment?, onClick: () -> Unit) {
    val titleText = "Appointment:"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // Match original
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Match original
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally, // Center content like original
            verticalArrangement = Arrangement.Center // Center content like original
        ) {
            if (appointment != null) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${"Dr." + appointment.doctorName ?: "Dr. ?"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${formatDate(appointment.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${appointment.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            } else {
                // Display default content if no appointment
                Icon(
                    Icons.Default.EventNote,
                    contentDescription = titleText,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DietSummaryCard(
    calories: Int,
    calorieGoal: Int,
    protein: Int,
    proteinGoal: Int,
    fat: Int,
    fatGoal: Int,
    carbs: Int,
    carbGoal: Int,
    onClick: () -> Unit
) {
    val titleText = stringResource(R.string.diet)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Cals: $calories/$calorieGoal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
            LinearProgressIndicator(
                progress = { (calories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.width(60.dp).padding(top = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("P: $protein/$proteinGoal", style = MaterialTheme.typography.labelSmall, maxLines=1, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("F: $fat/$fatGoal", style = MaterialTheme.typography.labelSmall, maxLines=1, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("C: $carbs/$carbGoal", style = MaterialTheme.typography.labelSmall, maxLines=1, fontSize = 16.sp)
        }
    }
}