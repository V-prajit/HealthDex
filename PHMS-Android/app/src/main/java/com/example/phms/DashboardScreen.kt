package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    firstName: String? = null,
    userToken: String? = null,
    onSettingsClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("home") }
    var newNoteRequested by remember { mutableStateOf(false) }

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
                    onClick  = { selectedTab = "home" },
                    icon     = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label    = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == "notes",
                    onClick  = {
                        selectedTab = "notes"
                        newNoteRequested = false
                    },
                    icon     = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                    label    = { Text("Notes") }
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
                    label    = { Text("Vitals") }
                )
                NavigationBarItem(
                    selected = selectedTab == "diet",
                    onClick  = { selectedTab = "diet" },
                    icon     = { Icon(Icons.Default.LocalDining, contentDescription = "Diet") },
                    label    = { Text("Diet") }
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
            "home" -> HomeScreen(
                firstName = firstName,
                onSettingsClick = onSettingsClick,
                onNavigateToChat = { selectedTab = "chat" },
                onNavigateToNotes = {
                    selectedTab = "notes"
                    newNoteRequested = true
                }
            )

            "notes" -> NotesScreen(
                userToken = userToken,
                modifier = Modifier.padding(innerPadding),
                onSettingsClick = onSettingsClick,
                newNoteRequested = newNoteRequested
            )

            "chat" -> ChatScreen(onBackClick = { selectedTab = "home" })

            "vitals" -> VitalSignsScreen(
                userId = userToken,
                onBackClick = { selectedTab = "home" }
            )

            "diet" -> DietScreen(
                userToken = userToken,
                modifier = Modifier.padding(innerPadding),
                onBack = { selectedTab = "home" }
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
    onSettingsClick: () -> Unit,
    newNoteRequested: Boolean
) {
    NotesFullApp(
        userToken = userToken,
        onSettingsClick = onSettingsClick,
        newNoteRequested = newNoteRequested
    )
}
