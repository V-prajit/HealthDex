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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun NotesFullApp(
    userToken: String? = null,
    onSettingsClick: () -> Unit = {},
    newNoteRequested: Boolean = false
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    // loads notes only once composable composed
    LaunchedEffect(Unit) {
        notes = if (!userToken.isNullOrEmpty()) {
            NotesRepositoryBackend.getNotes(userToken)
        } else {
            NotesRepository.getNotes(context)
        }
    }

    var currentScreen by remember {
        mutableStateOf(if (newNoteRequested) "edit" else "list")
    }
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
                    // notes index is saved and it moves to edit screedn
                },
                onNewNoteClick = {
                    selectedNoteIndex = null
                    noteContent = ""
                    currentScreen = "edit"
                    //for new note, sice it doesnt already have an index, idx is null
                },
                onNoteDelete = { index ->
                    if (!userToken.isNullOrEmpty()) {
                        scope.launch {
                            val noteId = notes[index].split("\n").firstOrNull()
                                ?.split("|")?.getOrElse(0) { "" }?.toIntOrNull() ?: (index + 1)
                            NotesRepositoryBackend.deleteNote(noteId)
                            notes = NotesRepositoryBackend.getNotes(userToken) // Refresh
                        }
                    } else {
                        val mutableNotes = notes.toMutableList()
                        mutableNotes.removeAt(index)
                        notes = mutableNotes
                        NotesRepository.saveNotes(context, notes)
                    }
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
                    }
                    else {
                        val title = updatedContent.split("\n").firstOrNull()?.trim() ?: ""
                        val indexToUpdate = notes.indexOfFirst { it.split("\n").firstOrNull()?.trim() == title }
                        if (indexToUpdate != -1) {
                            val mutableNotes = notes.toMutableList()
                            mutableNotes[indexToUpdate] = updatedContent
                            notes = mutableNotes
                        } else {
                            notes = notes + updatedContent
                        }
                    }
                    scope.launch {
                        if (!userToken.isNullOrEmpty()) {
                            NotesRepositoryBackend.saveNote(userToken, updatedContent)
                            // reloads notes from backend after saving
                            notes = NotesRepositoryBackend.getNotes(userToken)
                        }
                        else {
                            NotesRepository.saveNotes(context, notes)
                        }
                        currentScreen = "list"
                    }
                },
                onSaveAs = { updatedContent ->
                    // save as creates a new note and updates repository
                    val mutableNotes = notes.toMutableList()
                    mutableNotes.add(updatedContent)
                    notes = mutableNotes
                    scope.launch {
                        if (!userToken.isNullOrEmpty()) {
                            NotesRepositoryBackend.saveNote(userToken, updatedContent)
                            // Reload notes from backend after saving
                            notes = NotesRepositoryBackend.getNotes(userToken)
                        }
                        else {
                            NotesRepository.saveNotes(context, notes)
                        }
                        currentScreen = "list"
                    }
                },
                onCancel = {
                    // goes back to the list screen
                    currentScreen = "list"
                },
                // passed original file name and list of existing note names for duplicate check.
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
    onSettingsClick: () -> Unit
) {
    var isListLayout by remember { mutableStateOf(true) }
    // toggle sort by tag
    var selectedSortTag by remember { mutableStateOf("All") }
    // display notes based on selected tag
    val displayedNotes = if (selectedSortTag == "All") {
        notes
    } else {
        notes.filter { it.split("\n").getOrElse(2) { "" } == selectedSortTag }
    }
    Scaffold(
        topBar = {
            // upper bar that shows/ displays the title and action icons.
            TopAppBar(
                title = { Text(stringResource(R.string.notes), style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    // to toggle between list and grid view
                    TextButton(onClick = { isListLayout = !isListLayout }) {
                        Text(text = if (isListLayout) stringResource(R.string.switch_to_grid) else stringResource(R.string.switch_to_list))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNoteClick,
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            // sub panel for filtering by tag using a dropdown.
            val tagOptions = listOf("All", "diet", "medication", "health", "misc")
            var expandedSortMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Filter by Tag: $selectedSortTag", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { expandedSortMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Select tag")
                }
                DropdownMenu(
                    expanded = expandedSortMenu,
                    onDismissRequest = { expandedSortMenu = false }
                ) {
                    tagOptions.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                selectedSortTag = tag
                                expandedSortMenu = false
                            }
                        )
                    }
                }
            }
            val modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            if (isListLayout) {
                LazyColumn(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes) { index, note ->
                        val noteName = note.split("\n").firstOrNull()?.let { line ->
                            if (line.contains("|")) line.split("|").getOrElse(1) { line } else line
                        } ?: ""
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
                                DropdownMenu(
                                    //dropdown menu- reame, delete options for now. todo- add more options
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename)) },
                                        onClick = {
                                            onNoteClick(index, note)
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes) { index, note ->
                        val parts = note.split("\n", limit = 3)
                        val noteTitle = parts.getOrElse(0) { "" }.let { line ->
                            if (line.contains("|")) line.split("|").getOrElse(1) { line } else line
                        }
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
                                        text = { Text(stringResource(R.string.rename)) },
                                        onClick = {
                                            onNoteClick(index, note)
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
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
    var fileTag by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val duplicateNoteMessage = stringResource(R.string.duplicate_note_title)
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var originalId by remember { mutableStateOf<Int?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileBody += "\n[Image: $uri]"
            onContentChange("${if(originalId != null) "$originalId|$fileName" else fileName}\n$fileBody\n$fileTag")
        }
    }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileBody += "\n[Video: $uri]"
            onContentChange("${if(originalId != null) "$originalId|$fileName" else fileName}\n$fileBody\n$fileTag")
        }
    }
    LaunchedEffect(noteContent) {
        val lines = noteContent.split("\n", limit = 3)
        val firstLine = lines.getOrElse(0) { "" }
        if (firstLine.contains("|")) {
            val parts = firstLine.split("|")
            originalId = parts[0].toIntOrNull()
            fileName = parts.getOrElse(1) { "" }
        } else {
            fileName = firstLine
        }
        fileBody = lines.getOrElse(1) { "" }
        fileTag = lines.getOrElse(2) { "" }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_note), style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    onContentChange("${if(originalId != null) "$originalId|$fileName" else fileName}\n$fileBody\n$fileTag")
                },
                label = { Text(stringResource(R.string.file_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val tagOptions = listOf("diet", "medication", "health", "misc")
            var expandedTagMenu by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedTagMenu = true }
            ) {
                OutlinedTextField(
                    value = fileTag,
                    onValueChange = { },
                    label = { Text("Tag") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = expandedTagMenu,
                    onDismissRequest = { expandedTagMenu = false }
                ) {
                    tagOptions.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                fileTag = tag
                                onContentChange("${if(originalId != null) "$originalId|$fileName" else fileName}\n$fileBody\n$fileTag")
                                expandedTagMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = fileBody,
                onValueChange = {
                    fileBody = it
                    onContentChange("${if(originalId != null) "$originalId|$fileName" else fileName}\n$fileBody\n$fileTag")
                },
                label = { Text(stringResource(R.string.file_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = {
                    imagePickerLauncher.launch("image/*")
                }) {
                    Text(stringResource(R.string.insert_image))
                }
            }
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    val noteToSave = if (originalId != null) "$originalId|$fileName\n$fileBody\n$fileTag" else "$fileName\n$fileBody\n$fileTag"
                    if (fileName.isNotBlank() && existingNoteNames.filter { it != originalFileName }.any { it.equals(fileName, ignoreCase = true) }) {
                        showDuplicateDialog = true
                    }
                    else {
                        onSave(noteToSave)
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
                Button(onClick = {
                    if (fileName.isNotBlank() && existingNoteNames.filter { it != originalFileName }.any { it.equals(fileName, ignoreCase = true) }) {
                        showDuplicateDialog = true
                    } else {
                        onSaveAs("$fileName\n$fileBody\n$fileTag")
                    }
                }) {
                    Text(stringResource(R.string.save_as))
                }
            }
        }
        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text(duplicateNoteMessage) },
                text = { Text("Rename or Replace?") },
                confirmButton = {
                    Button(onClick = { onSave(if (originalId != null) "$originalId|$fileName\n$fileBody\n$fileTag" else "$fileName\n$fileBody\n$fileTag"); showDuplicateDialog = false }) {
                        Text("Replace")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDuplicateDialog = false }) {
                        Text("Rename")
                    }
                }
            )
        }
    }
}