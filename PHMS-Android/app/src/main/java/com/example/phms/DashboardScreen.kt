package com.example.phms

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    firstName: String? = null,
    onSettingsClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("home") }
    Scaffold(
        bottomBar = {
            NavigationBar {
                //items on navigation bar: home & notes for now. todo add more features later(health,etc)
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == "notes",
                    onClick = { selectedTab = "notes" },
                    icon = { Icon(Icons.Filled.Note, contentDescription = "Notes") },
                    label = { Text("Notes") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> HomeScreen(
                firstName = firstName,
                onSettingsClick = onSettingsClick
            )
            "notes" -> NotesScreen(
                modifier = Modifier.padding(innerPadding),
                onSettingsClick = onSettingsClick
            )
        }
    }
}
@Composable
fun NotesScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {}) {
    NotesFullApp(onSettingsClick = onSettingsClick)
}
