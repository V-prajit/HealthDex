package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.phms.VitalSignsScreen

@Composable
fun DashboardScreen(
    firstName: String? = null,
    userToken: String? = null,
    onSettingsClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("home") }
    // New flag: when true, NotesScreen will open in “edit” (new note) mode
    var newNoteRequested by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = {
                        selectedTab = "home"
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == "notes",
                    onClick = {
                        selectedTab = "notes"
                        // tapping the Notes tab itself should NOT auto‑open editor
                        newNoteRequested = false
                    },
                    icon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                    label = { Text("Notes") }
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
                    label    = { Text("Vitals") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> {
                Log.d("DashboardScreen", "Showing HomeScreen")
                HomeScreen(
                    firstName = firstName,
                    onSettingsClick = {
                        Log.d("DashboardScreen", "HomeScreen settings clicked")
                        onSettingsClick()
                    },
                    // when Add Note is tapped on Home, go to Notes *and* open editor
                    onNavigateToNotes = {
                        selectedTab = "notes"
                        newNoteRequested = true
                    },
                    onNavigateToVitals = {
                        selectedTab = "vitals"
                    }

                )
            }

            "notes" -> {
                Log.d("DashboardScreen", "Showing NotesScreen")
                NotesScreen(
                    userToken = userToken,
                    modifier = Modifier.padding(innerPadding),
                    onSettingsClick = {
                        Log.d("DashboardScreen", "NotesScreen settings clicked")
                        onSettingsClick()
                    },
                    newNoteRequested = newNoteRequested
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
        userToken = userToken,
        onSettingsClick = onSettingsClick,
        newNoteRequested = newNoteRequested
    )
}