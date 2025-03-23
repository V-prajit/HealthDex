package com.example.phms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.phms.NotesRepository

@Composable
fun NotesFullApp() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) {
        notes = NotesRepository.getNotes(context)
    }
    // decides what screen to display
    var currentScreen by remember { mutableStateOf("list") }
    var selectedNoteIndex by remember { mutableStateOf<Int?>(null) }
    var noteContent by remember { mutableStateOf("") }

    when (currentScreen) {
        "list" -> {
            NotesFullListScreen(
                notes = notes,
                onNoteClick = { index, note ->
                    selectedNoteIndex = index
                    noteContent = note
                    currentScreen = "edit"
                },
                onNewNoteClick = {
                    selectedNoteIndex = null
                    noteContent = ""
                    currentScreen = "edit"
                },
                onNoteDelete = { index ->
                    val mutableNotes = notes.toMutableList()
                    mutableNotes.removeAt(index)
                    notes = mutableNotes
                    NotesRepository.saveNotes(context, notes)
                }
            )
        }
        "edit" -> {
            NotesFullEditScreen(
                noteContent = noteContent,
                onContentChange = { noteContent = it },
                onSave = { updatedContent ->
                    // function to save updates to the existing note or add as new.
                    if (selectedNoteIndex != null) {
                        val mutableNotes = notes.toMutableList()
                        mutableNotes[selectedNoteIndex!!] = updatedContent
                        notes = mutableNotes
                    } else {
                        notes = notes + updatedContent
                    }
                    NotesRepository.saveNotes(context, notes)
                    currentScreen = "list"
                },
                onSaveAs = { updatedContent ->
                    // save as creates a new note
                    notes = notes + updatedContent
                    NotesRepository.saveNotes(context, notes)
                    currentScreen = "list"
                },
                onCancel = {
                    // Just go back to the list screen if you change your mind.
                    currentScreen = "list"
                }
            )
        }
    }
}

@Composable
fun NotesFullListScreen(
    notes: List<String>,
    onNoteClick: (Int, String) -> Unit,
    onNewNoteClick: () -> Unit,
    onNoteDelete: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            // to display "Notes".
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Display notes in a grid layout. todo- add diff layout styles (maybe list view)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(notes) { index, note ->
                    var expanded by remember { mutableStateOf(false) }
                    val parts = note.split("\n", limit = 2)
                    val noteTitle = parts.getOrElse(0) { "" }
                    val noteSummary = parts.getOrElse(1) { "" }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { onNoteClick(index, note) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = noteTitle, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = noteSummary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        onNoteClick(index, note)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        onNoteDelete(index)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        // a action button to create a new note.
        FloatingActionButton(
            onClick = onNewNoteClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)  // minimal change to lift it above the nav bar
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Note")
        }
    }
}

@Composable
fun NotesFullEditScreen(
    noteContent: String,
    onContentChange: (String) -> Unit,
    onSave: (String) -> Unit,
    onSaveAs: (String) -> Unit,
    onCancel: () -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var fileBody by remember { mutableStateOf("") }
    LaunchedEffect(noteContent) {
        val lines = noteContent.split("\n", limit = 2)
        fileName = lines.getOrElse(0) { "" }
        fileBody = lines.getOrElse(1) { "" }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        SimpleAppBar(title = "Edit Note", onBackClick = onCancel)
        // Main editing area.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = {
                    fileName = it
                    onContentChange(fileName + "\n" + fileBody)
                },
                label = { Text("File Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = fileBody,
                onValueChange = {
                    fileBody = it
                    onContentChange(fileName + "\n" + fileBody)
                },
                label = { Text("File Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                singleLine = false,
                maxLines = 20
            )
            Spacer(modifier = Modifier.height(16.dp))
            // save or save as options
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { onSave(fileName + "\n" + fileBody) }) {
                    Text("Save")
                }
                Button(onClick = { onSaveAs(fileName + "\n" + fileBody) }) {
                    Text("Save As")
                }
            }
        }
    }
}

@Composable
fun SimpleAppBar(title: String, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            // minimal addition below so the back arrow is visible and clickable
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
