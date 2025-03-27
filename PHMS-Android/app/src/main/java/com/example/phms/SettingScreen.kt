package com.example.phms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import android.app.Activity


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(onBackClick: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguageCode = remember { LocaleHelper.getCurrentLanguageCode(context) }
    val currentLanguage = remember(currentLanguageCode) {
        LocaleHelper.supportedLanguages.find { it.code == currentLanguageCode } ?: LocaleHelper.supportedLanguages[0]
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.language)) },
                supportingContent = { Text(currentLanguage.displayName) },
                trailingContent = {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )

            Divider()

            ListItem(
                headlineContent = { Text("Logout") },
                supportingContent = { Text("Sign out of your account") },
                trailingContent = {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier.clickable { onLogout() }
            )

            Divider()
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(stringResource(R.string.select_language)) },
                text = {
                    LazyColumn {
                        items(LocaleHelper.supportedLanguages) { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            LocaleHelper.applyLanguage(context, language.code)
                                            (context as? Activity)?.recreate()
                                            showLanguageDialog = false
                                        }
                                    }
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(language.displayName)

                                if (language.code == currentLanguageCode) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (language != LocaleHelper.supportedLanguages.last()) {
                                Divider()
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}