package com.example.phms
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun NotesFullApp(onSettingsClick: () -> Unit = {}) {
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
            NotesListScreen(
                notes = notes,
                onNoteClick = { index, note ->
                    selectedNoteIndex = index
                    noteContent = note
                    currentScreen = "edit"
                    // When a note is clicked, its index is saved and it moves on to edit
                },
                onNewNoteClick = {
                    selectedNoteIndex = null
                    noteContent = ""
                    currentScreen = "edit"
                    //for new note, sice it doesnt already have an index, idx is null
                },
                onNoteDelete = { index ->
                    val mutableNotes = notes.toMutableList()
                    mutableNotes.removeAt(index)
                    notes = mutableNotes
                    NotesRepository.saveNotes(context, notes)
                    // deletes note, removes it from the list and saves the updated list
                },
                onSettingsClick = onSettingsClick
            )
        }
        "edit" -> {
            NotesEditScreen(
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
                    // save as creates a new note and updates repository
                    notes = notes + updatedContent
                    NotesRepository.saveNotes(context, notes)
                    currentScreen = "list"
                },
                onCancel = {
                    // Just go back to the list screen if you change your mind.
                    currentScreen = "list"
                },
                // Pass original file name and list of existing note names for duplicate check.
                originalFileName = noteContent.split("\n").firstOrNull()?.trim() ?: "",
                existingNoteNames = notes.map { it.split("\n").firstOrNull()?.trim() ?: "" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    notes: List<String>,
    onNoteClick: (Int, String) -> Unit,
    onNewNoteClick: () -> Unit,
    onNoteDelete: (Int) -> Unit,
    onSettingsClick: () -> Unit  // new parameter added
) {
    var isListLayout by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            // upper bar that to display the title and action icons.
            TopAppBar(
                title = { Text("Notes", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    // Toggle between list and grid view
                    TextButton(onClick = { isListLayout = !isListLayout }) {
                        Text(text = if (isListLayout) "Switch to Grid" else "Switch to List")
                    }
                }
            )
        },
        floatingActionButton = {
            //a floating button to create a nw note
            FloatingActionButton(onClick = onNewNoteClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)

        if (isListLayout) {
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(notes) { index, note ->
                    val noteName = note.split("\n").firstOrNull() ?: ""
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(index, note) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                //displays the title of the note
                                text = noteName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            //dropdown menu. rename/delete for now. todo- add more options
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
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
        } else {
            // Display notes in a grid layout.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(notes) { index, note ->
                    val parts = note.split("\n", limit = 2)
                    val noteTitle = parts.getOrElse(0) { "" }
                    val noteSummary = parts.getOrElse(1) { "" }
                    // Split the note into two lines: title and summary
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
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesEditScreen(
    noteContent: String,
    onContentChange: (String) -> Unit,
    onSave: (String) -> Unit,
    onSaveAs: (String) -> Unit,
    onCancel: () -> Unit,
    originalFileName: String,
    existingNoteNames: List<String>
) {
    var fileName by remember { mutableStateOf("") }
    var fileBody by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // system picker to insert an image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileBody += "\n[Image: $uri]"
            onContentChange("$fileName\n$fileBody")
        }
    }

    // system picker for video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // video placeholder
            fileBody += "\n[Video: $uri]"
            onContentChange("$fileName\n$fileBody")
        }
    }

    LaunchedEffect(noteContent) {
        val lines = noteContent.split("\n", limit = 2)
        fileName = lines.getOrElse(0) { "" }
        fileBody = lines.getOrElse(1) { "" }
    }
    Scaffold(
        //top bar with back arrow
        topBar = {
            TopAppBar(
                title = { Text("Edit Note", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = {
                    fileName = it
                    errorMessage = ""
                    onContentChange("$fileName\n$fileBody")
                },
                label = { Text("File Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = fileBody,
                onValueChange = {
                    fileBody = it
                    onContentChange("$fileName\n$fileBody")
                },
                label = { Text("File Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            // new row for media insertion options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = {
                    // Launch system picker to select an image
                    imagePickerLauncher.launch("image/*")
                }) {
                    Text("Insert Image")
                }
                Button(onClick = {
                    // Launch system picker to select a video
                    videoPickerLauncher.launch("video/*")
                }) {
                    Text("Insert Video")
                }
            }
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // save or save as options
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    if (fileName in existingNoteNames && fileName != originalFileName) {
                        errorMessage = "A note with this title already exists. Please choose a different title."
                    } else {
                        onSave("$fileName\n$fileBody")
                    }
                }) {
                    Text("Save")
                }
                Button(onClick = {
                    // Save As always creates a new note
                    if (fileName in existingNoteNames && fileName != originalFileName) {
                        errorMessage = "A note with this title already exists. Please choose a different title."
                    } else {
                        onSaveAs("$fileName\n$fileBody")
                    }
                }) {
                    Text("Save As")
                }
            }
        }
    }
}
