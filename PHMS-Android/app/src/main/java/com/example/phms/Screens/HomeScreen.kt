package com.example.phms.Screens

import android.util.Log
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import com.example.phms.Appointment
import com.example.phms.R
import com.example.phms.repository.AppointmentRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    firstName: String?,
    onSettingsClick: () -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAppointments: () -> Unit
) {

    val context = LocalContext.current

    // for upcoming appointments
    var upcomingAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.getString("LAST_USER_UID", null)?.let { uid ->
            if (uid.isNotEmpty()) {
                launch {
                    upcomingAppointments = AppointmentRepository.getUpcomingAppointments(uid)
                }
            }
        }
    }
    //greeting pane (it uses time to change message acc)
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when (hour) {
        in 6..11  -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        else      -> stringResource(R.string.good_evening)
    } + (firstName?.let { ", $it" } ?: "")
    //greeting pane background photo- again, changes acc to the time
    val imageUrl = when (hour) {
        in 6..11  -> "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
        in 12..16 -> "https://images.unsplash.com/photo-1506744038136-46273834b3fb?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
        in 17..19 -> "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
        else      -> "https://images.unsplash.com/photo-1507400492013-162706c8c05e?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, top = 40.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
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

        // search bar
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
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

        Spacer(modifier = Modifier.height(16.dp))
        //quick action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistChip(
                onClick = onNavigateToNotes,
                leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                label = { Text(stringResource(R.string.add_note)) }
            )
            AssistChip(
                onClick = onNavigateToVitals,
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                label = { Text(stringResource(R.string.add_vital)) }
            )
            AssistChip(
                onClick = onNavigateToAppointments,
                leadingIcon = { Icon(Icons.Default.EventNote, contentDescription = null) },
                label = { Text(stringResource(R.string.appointments)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistChip(
                onClick = onNavigateToAppointments,
                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                label = { Text(stringResource(R.string.doctors)) }
            )
            AssistChip(
                onClick = onNavigateToMedications,
                leadingIcon = { Icon(Icons.Default.LocalPharmacy, contentDescription = null) },
                label = { Text(stringResource(R.string.medications)) }
            )
        }

        // show upcoming appointment
        if (upcomingAppointments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.upcoming_appointments),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(upcomingAppointments.take(1)) { appt ->
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
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${appt.time} (${appt.duration} min)")
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = appt.status.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appt.doctorName ?: "")
                            }
                            appt.reason.takeIf { it.isNotBlank() }?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
