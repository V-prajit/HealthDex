package com.example.phms.Screens

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = {
                        selectedTab = "home"
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home)) },
                    label = { Text(stringResource(R.string.home)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "notes",
                    onClick  = {
                        selectedTab = "notes"
                        newNoteRequested = false
                    },
                    icon = { Icon(Icons.Default.Note, contentDescription = stringResource(R.string.notes)) },
                    label = { Text(stringResource(R.string.notes)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "chat",
                    onClick  = { selectedTab = "chat" },
                    icon     = { Icon(Icons.Default.Chat, contentDescription = stringResource(R.string.chatbot)) },
                    label    = { Text(stringResource(R.string.chatbot)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "vitals",
                    onClick  = { selectedTab = "vitals" },
                    icon     = { Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.vitals)) },
                    label    = { Text(stringResource(R.string.vitals)) }
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
                        onNavigateToSearch = { showSearchScreen = true },
                        onNavigateToChatbot = { selectedTab = "chat" },
                        onNavigateToDiet = { selectedTab = "diet" },
                        onNavigateToDoctors = { selectedTab = "doctors" }
                    )
                }
            }

            "appointments" -> {
                AppointmentsScreen(
                    userId = userToken,
                    onBackClick = { selectedTab = "home" },
                    onViewDoctors = { selectedTab = "doctors" },
                    onSettingsClick = { onSettingsClick("appointments") }
                )
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
            "diet" -> DietScreen(
                userId = userToken,
                onBackClick = { selectedTab = "home" },
                onSettingsClick = { onSettingsClick("diet") }
            )
            "doctors" -> {
                 DoctorsScreen(
                    userId = userToken,
                    onBackClick = { selectedTab = "home" }
                 )
            }
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