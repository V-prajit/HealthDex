package com.example.phms

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.util.Calendar

@Composable
fun HomeScreen(
    firstName: String?,
    onSettingsClick: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToNotes: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when (hour) {
        in 6..11  -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        else      -> stringResource(R.string.good_evening)
    } + (firstName?.let { ", $it" } ?: "")

    val imageUrl = when (hour) {
        in 6..11  -> "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
        in 12..16 -> "https://images.unsplash.com/photo-1506744038136-46273834b3fb?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
        in 17..19 -> "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
        else      -> "https://images.unsplash.com/photo-1507400492013-162706c8c05e?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, top = 40.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = {
                Log.d("HomeScreen", "Settings icon clicked")
                onSettingsClick()
            }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            placeholder = {
                Text(stringResource(R.string.search))
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = greetingText,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistChip(
                onClick = onNavigateToNotes,
                leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                label = { Text("Add Note") }
            )
            AssistChip(
                onClick = onNavigateToVitals,
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                label = { Text("Add Vital") }
            )
            AssistChip(
                onClick = { /* todo: add medicine */ },
                leadingIcon = { Icon(Icons.Default.LocalPharmacy, contentDescription = null) },
                label = { Text("Medicine") }
            )
            AssistChip(
                onClick = { /* todo: show events */ },
                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                label = { Text("Events") }
            )
        }
    }
}