package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.phms.VitalSignsScreen
import com.example.phms.SearchScreen

@Composable
fun DashboardScreen(
    firstName: String? = null,
    userToken: String? = null,
    onSettingsClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("home") }
    // New flag: when true, NotesScreen will open in "edit" (new note) mode
    var newNoteRequested by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }  // +assistant: added search screen state

    Scaffold(
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
                    onClick = {
                        selectedTab = "notes"
                        newNoteRequested = false
                    },
                    icon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                    label = { Text(stringResource(R.string.notes)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "chat",
                    onClick = { selectedTab = "chat" },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text(stringResource(R.string.chat)) }
                )
                NavigationBarItem(
                    selected = selectedTab == "vitals",
                    onClick  = { selectedTab = "vitals" },
                    icon     = { Icon(Icons.Default.Favorite, contentDescription = "Vitals") },
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
                        }
                    )
                }
                else {
                    HomeScreen(
                        firstName          = firstName,
                        onSettingsClick    = {
                            Log.d("DashboardScreen", "HomeScreen settings clicked")
                            onSettingsClick()
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
                        onViewDoctors = { showDoctorsScreen = true }
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
                        onSettingsClick()
                    },
                    newNoteRequested= newNoteRequested
                )
            }

            "chat" -> {
                Log.d("DashboardScreen", "Showing ChatScreen")
                ChatScreen(
                    onBackClick = {
                        Log.d("DashboardScreen", "Chat back clicked")
                        selectedTab = "home"
                    }
                )
            }

            "vitals" -> VitalSignsScreen(
                userId      = userToken,
                onBackClick = { selectedTab = "home" }
            )
        }
    }
}

@Composable
fun NotesScreen(
    userToken: String? = null,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    newNoteRequested: Boolean = false
) {
    NotesFullApp(
        userToken        = userToken,
        onSettingsClick  = onSettingsClick,
        newNoteRequested = newNoteRequested
    )
}
