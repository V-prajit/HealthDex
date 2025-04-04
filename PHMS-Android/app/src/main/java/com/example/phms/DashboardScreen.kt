package com.example.phms

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    firstName: String? = null,
    userToken: String? = null,
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
                    icon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
                    label = { Text("Notes") }
                )
                NavigationBarItem(
                    selected = selectedTab == "chat",
                    onClick = { selectedTab = "chat" },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text(stringResource(R.string.chat)) }
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
                    }
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
        }
    }
}

@Composable
fun NotesScreen(userToken: String? = null, modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {}) {
    NotesFullApp(userToken = userToken, onSettingsClick = onSettingsClick)
}
