package com.example.phms.Screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.phms.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    firstName: String? = null,
    userToken: String? = null,
    initialSelectedTab: String = "home",
    onSettingsClick: (originTab: String?) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(initialSelectedTab) }
    var newNoteRequested by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(stringResource(R.string.home)) }
                    // Customize selected/unselected colors if needed
                    /* colors = NavigationBarItemDefaults.colors(
                         selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                         selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                         unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                         unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                         indicatorColor = MaterialTheme.colorScheme.primaryContainer
                     ) */
                )
                NavigationBarItem(
                    selected = selectedTab == "appointments",
                    onClick = { selectedTab = "appointments" },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = "Appointments") },
                    label = { Text(stringResource(R.string.appointments)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "notes",
                    onClick  = {
                        selectedTab = "notes"
                        newNoteRequested = false
                    },
                    icon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                    label = { Text(stringResource(R.string.notes)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "chat",
                    onClick  = { selectedTab = "chat" },
                    icon     = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label    = { Text(stringResource(R.string.chat)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "vitals",
                    onClick  = { selectedTab = "vitals" },
                    icon     = { Icon(Icons.Default.Favorite, contentDescription = "Vitals") },
                    label    = { Text(stringResource(R.string.vitals)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "medications",
                    onClick = {
                        selectedTab = "medications"
                        // Make sure to pass onSettingsClick here if needed from Dashboard
                        // onSettingsClick("medications") // Example if needed
                    },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Meds") },
                    label = { Text("Meds") }
                )
                NavigationBarItem(
                    selected = selectedTab == "diet",
                    onClick = { selectedTab = "diet" },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = "Diet") },
                    label = { Text("Diet") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> {
                if (showSearchScreen) {
                    SearchScreen(
                        userToken   = userToken,
                        onClose     = { showSearchScreen = false },
                        onBackClick = { showSearchScreen = false }, // Added back click handler
                        onNavigateToNotes = { selectedTab = "notes"; newNoteRequested = false },
                        onNavigateToVitals = { selectedTab = "vitals" },
                        onNavigateToAppointments = { selectedTab = "appointments" },
                        onNavigateToMedications = { selectedTab = "medications" }
                    )
                } else {
                    HomeScreen(
                        firstName          = firstName,
                        onSettingsClick    = { onSettingsClick("home") },
                        onNavigateToNotes  = { selectedTab = "notes"; newNoteRequested = true },
                        onNavigateToVitals = { selectedTab = "vitals" },
                        onNavigateToAppointments = { selectedTab = "appointments" },
                        onNavigateToMedications = { selectedTab = "medications" },
                        onNavigateToSearch = { showSearchScreen = true }
                    )
                }
            }
            "appointments" -> {
                var showDoctorsScreen by remember { mutableStateOf(false) }
                if (showDoctorsScreen) {
                    DoctorsScreen(
                        userId = userToken,
                        onBackClick = { showDoctorsScreen = false }
                    )
                } else {
                    AppointmentsScreen(
                        userId = userToken,
                        onBackClick = { selectedTab = "home" },
                        onViewDoctors = { showDoctorsScreen = true },
                        onSettingsClick = { onSettingsClick("appointments") }
                    )
                }
            }
            "notes" -> {
                NotesScreen( // Call NotesFullApp wrapper
                    userToken        = userToken,
                    modifier        = Modifier.padding(innerPadding),
                    onSettingsClick  = { onSettingsClick("notes") },
                    onBackClick = { selectedTab = "home" },
                    newNoteRequested = newNoteRequested
                )
            }
            "chat" -> {
                ChatScreen(
                    onBackClick = { selectedTab = "home" },
                    onSettingsClick = { onSettingsClick("chat") }
                )
            }
            "vitals" -> VitalSignsScreen(
                userId      = userToken,
                onBackClick = { selectedTab = "home" },
                onSettingsClick = { onSettingsClick("vitals") }
            )
            "medications" -> PokemonMedicationsScreen(
                userToken = userToken,
                modifier = Modifier.padding(innerPadding),
                onBack = { selectedTab = "home" },
                onSettingsClick = { onSettingsClick("medications") }
            )
            "diet" -> DietScreen(
                userId = userToken,
                onBackClick = { selectedTab = "home" },
                onSettingsClick = { onSettingsClick("diet") }
            )
        }
    }
}

@Composable
fun NotesScreen(
    userToken: String?,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    newNoteRequested: Boolean = false
) {
    NotesFullApp(
        userToken        = userToken,
        onSettingsClick  = onSettingsClick,
        onBackClick = onBackClick,
        newNoteRequested = newNoteRequested
    )
}
