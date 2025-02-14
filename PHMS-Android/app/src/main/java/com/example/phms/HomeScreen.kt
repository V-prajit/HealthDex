package com.example.phms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(firstName: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (firstName.isNullOrEmpty()) "Welcome!" else "Hello, $firstName!",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
