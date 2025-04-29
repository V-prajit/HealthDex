package com.example.phms.Screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
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
import androidx.compose.ui.text.style.TextOverflow
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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home"; showSearchScreen = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home)) },
                    label = {
                        Text(
                            stringResource(R.string.home),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == "notes",
                    onClick = { selectedTab = "notes"; newNoteRequested = false },
                    icon = { Icon(Icons.Default.Note, contentDescription = stringResource(R.string.notes)) },
                    label = {
                        Text(
                            stringResource(R.string.notes),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == "chat",
                    onClick = { selectedTab = "chat" },
                    icon = { Icon(Icons.Default.Chat, contentDescription = stringResource(R.string.chat)) },
                    label = {
                        Text(
                            stringResource(R.string.chat),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == "vitals",
                    onClick = { selectedTab = "vitals" },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.vitals)) },
                    label = {
                        Text(
                            stringResource(R.string.vitals),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        onBackClick = { showSearchScreen = false },
                        onNavigateToNotes = { selectedTab = "notes"; newNoteRequested = false; showSearchScreen = false },
                        onNavigateToVitals = { selectedTab = "vitals"; showSearchScreen = false },
                        onNavigateToAppointments = { selectedTab = "appointments"; showSearchScreen = false },
                        onNavigateToMedications = { selectedTab = "medications"; showSearchScreen = false },
                        onNavigateToDiet = { selectedTab = "diet"; showSearchScreen = false }
                    )
                } else {
                    HomeScreen(
                        firstName          = firstName,
                        onSettingsClick    = { onSettingsClick("home") },
                        onNavigateToNotes  = { selectedTab = "notes"; newNoteRequested = true },
                        onNavigateToVitals = { selectedTab = "vitals" },
                        onNavigateToAppointments = { selectedTab = "appointments" },
                        onNavigateToMedications = { selectedTab = "medications" },
                        onNavigateToSearch = { showSearchScreen = true },
                        onNavigateToChatbot = { selectedTab = "chat" },
                        onNavigateToDiet = { selectedTab = "diet" },
                        onNavigateToDoctors = { selectedTab = "doctors" }
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
                NotesScreen(
                    userToken        = userToken,
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
            "medications" -> {
                PokemonMedicationsScreen(
                    userToken = userToken,
                    onBack = { selectedTab = "home" },
                    onSettingsClick = { onSettingsClick("medications") }
                )
            }
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

    Box(modifier = modifier){
        NotesFullApp(
            userToken        = userToken,
            onSettingsClick  = onSettingsClick,
            onBackClick = onBackClick,
            newNoteRequested = newNoteRequested
        )
    }
}