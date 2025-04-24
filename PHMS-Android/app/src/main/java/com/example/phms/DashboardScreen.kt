package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.phms.VitalSignsScreen
import com.example.phms.SearchScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    firstName: String? = null,
    userToken: String? = null,
    initialSelectedTab: String = "home",
    onSettingsClick: (originTab: String?) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(initialSelectedTab) }
    // New flag: when true, NotesScreen will open in "edit" (new note) mode
    var newNoteRequested by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }  // +assistant: added search screen state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = {
                        selectedTab = "home"
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(stringResource(R.string.home)) }
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
                    onClick  = { selectedTab = "medications" },
                    icon     = { Icon(Icons.Default.MedicalServices, contentDescription = "Meds") },
                    label    = { Text("Meds") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> {
                Log.d("DashboardScreen", "Showing HomeScreen")
                if (showSearchScreen) {
                    SearchScreen(
                        userToken   = userToken,
                        onClose     = { showSearchScreen = false },
                        onBackClick = { showSearchScreen = false },
                        onNavigateToNotes = {
                            selectedTab = "notes"
                            newNoteRequested = false
                        },
                        onNavigateToVitals = {
                            selectedTab = "vitals"
                        },
                        onNavigateToAppointments = {
                            selectedTab = "appointments"
                        },
                        onNavigateToMedications = { selectedTab = "medications" }
                    )
                }
                else {
                    HomeScreen(
                        firstName          = firstName,
                        onSettingsClick    = {
                            Log.d("DashboardScreen", "HomeScreen settings clicked")
                            onSettingsClick("home")
                        },
                        // when Add Note is tapped on Home, go to Notes *and* open editor
                        onNavigateToNotes  = {
                            selectedTab = "notes"
                            newNoteRequested = true
                        },
                        onNavigateToVitals = {
                            selectedTab = "vitals"
                        },
                        onNavigateToAppointments = {
                          selectedTab = "appointments"
                        },
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
                Log.d("DashboardScreen", "Showing NotesScreen")
                NotesScreen(
                    userToken       = userToken,
                    modifier        = Modifier.padding(innerPadding),
                    onSettingsClick = {
                        Log.d("DashboardScreen", "NotesScreen settings clicked")
                        onSettingsClick("notes")
                    },
                    onBackClick = { selectedTab = "home" },
                    newNoteRequested= newNoteRequested
                )
            }

            "chat" -> {
                Log.d("DashboardScreen", "Showing ChatScreen")
                ChatScreen(
                    onBackClick = {
                        Log.d("DashboardScreen", "Chat back clicked")
                        selectedTab = "home"
                    },
                    onSettingsClick = { onSettingsClick("chat") }
                )
            }

            "vitals" -> VitalSignsScreen(
                userId      = userToken,
                onBackClick = { selectedTab = "home" },
                onSettingsClick = { onSettingsClick("vitals") }
            )
            "medications" -> MedicationsScreen(
                userToken = userToken,
                modifier = Modifier.padding(innerPadding),
                onBack = { selectedTab = "home" }
            )
        }
    }
}

// Note: remove any stray imports of com.example.phms.NotesScreen — this file declares NotesScreen below

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
