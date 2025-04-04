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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun NotesFullApp(
    userToken: String? = null,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    var noteTags by remember { mutableStateOf<Map<Int, List<String>>>(mapOf()) }
    var currentFilter by remember { mutableStateOf<String?>(null) }

    // Helper function to extract tags from note
    fun extractTags(notes: List<String>): Map<Int, List<String>> {
        return notes.mapIndexed { index, note ->
            val firstLine = note.split("\n").firstOrNull() ?: ""
            val tagMatch = "\\[(.*?)\\]".toRegex().find(firstLine)
            val tagString = tagMatch?.groupValues?.get(1) ?: ""
            val tags = tagString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            index to tags
        }.toMap()
    }

    // Load notes only once when the composable is first composed.
    LaunchedEffect(Unit) {
        if (!userToken.isNullOrEmpty()) {
            try {
                val (fetchedNotes, fetchedTags) = NotesRepositoryBackend.getNotesWithTags(userToken)
                if (fetchedNotes.isNotEmpty()) {
                    notes = fetchedNotes
                    noteTags = fetchedTags
                    // Store them locally so they appear on next login or offline
                    NotesRepository.saveNotes(context, notes)
                } else {
                    // Fallback to local storage if backend returns empty
                    notes = NotesRepository.getNotes(context)
                    noteTags = extractTags(notes)  // Calculate tags from note content
                }
            } catch (e: Exception) {
                // Fallback to local storage if backend fails
                notes = NotesRepository.getNotes(context)
                noteTags = extractTags(notes)  // Calculate tags from note content
            }
        } else {
            notes = NotesRepository.getNotes(context)
            noteTags = extractTags(notes)  // Calculate tags from note content
        }
    }

    var currentScreen by remember { mutableStateOf("list") }
    var selectedNoteIndex by remember { mutableStateOf<Int?>(null) }
    var noteContent by remember { mutableStateOf("") }

    when (currentScreen) {
        "list" -> {
            NotesListScreen(
                notes = notes,
                tags = noteTags,
                currentFilter = currentFilter,
                onFilterChange = { currentFilter = it },
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

                    // Update tags after deletion
                    // Removed tag reindexing code to fix crashing issue.

                    if (!userToken.isNullOrEmpty()) {
                        scope.launch {
                            val refreshed = NotesRepositoryBackend.getNotes(userToken) // Refresh
                            notes = if (refreshed.isNotEmpty()) refreshed else NotesRepository.getNotes(context)
                            // Calculate tags based on new notes
                            noteTags = extractTags(notes)
                            // Save locally for persistence
                            NotesRepository.saveNotes(context, notes)
                        }
                    } else {
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
                    } else {
                        notes = notes + updatedContent
                    }
                    // Here we remove tag data before saving to backend,
                    // preserving old behavior (note saved as "fileName\nfileBody").
                    val noteToSave = updatedContent
                    scope.launch {
                        if (!userToken.isNullOrEmpty()) {
                            NotesRepositoryBackend.saveNote(userToken, noteToSave)
                            val refreshed = NotesRepositoryBackend.getNotes(userToken)
                            notes = if (refreshed.isNotEmpty()) refreshed else NotesRepository.getNotes(context)
                            // Recalculate tags (will be empty if backend didn't save them)
                            noteTags = extractTags(notes)
                            // Save locally for persistence
                            NotesRepository.saveNotes(context, notes)
                        } else {
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
                    val noteToSave = updatedContent
                    scope.launch {
                        if (!userToken.isNullOrEmpty()) {
                            NotesRepositoryBackend.saveNote(userToken, noteToSave)
                            val refreshed = NotesRepositoryBackend.getNotes(userToken)
                            notes = if (refreshed.isNotEmpty()) refreshed else NotesRepository.getNotes(context)
                            // Recalculate tags
                            noteTags = extractTags(notes)
                            // Save locally for persistence
                            NotesRepository.saveNotes(context, notes)
                        } else {
                            NotesRepository.saveNotes(context, notes)
                        }
                        currentScreen = "list"
                    }
                },
                onCancel = {
                    // Just go back to the list screen if you change your mind
                    currentScreen = "list"
                },
                originalFileName = noteContent.split("\n").firstOrNull()?.replace("\\[.*?\\]".toRegex(), "")?.trim() ?: "",
                existingNoteNames = notes.map { it.split("\n").firstOrNull()?.replace("\\[.*?\\]".toRegex(), "")?.trim() ?: "" }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    notes: List<String>,
    tags: Map<Int, List<String>>,
    currentFilter: String?,
    onNoteClick: (Int, String) -> Unit,
    onNewNoteClick: () -> Unit,
    onNoteDelete: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onFilterChange: (String?) -> Unit
) {
    var isListLayout by remember { mutableStateOf(true) }

    // Filter notes based on current tag filter
    val filteredIndices = remember(notes, tags, currentFilter) {
        if (currentFilter == null) {
            notes.indices.toList()
        } else {
            notes.indices.filter { index ->
                tags[index]?.contains(currentFilter) ?: false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes), style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = onNewNoteClick) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    TextButton(onClick = { isListLayout = !isListLayout }) {
                        Text(text = if (isListLayout) stringResource(R.string.switch_to_grid) else stringResource(R.string.switch_to_list))
                    }
                }
            )
        },
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)

        Column {
            // Tag filter section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Tags: ")
                Spacer(modifier = Modifier.width(8.dp))

                // Create a flattened list of unique tags
                val allTags = tags.values.flatten().toSet().toList()
                var expanded by remember { mutableStateOf(false) }

                Box {
                    Button(onClick = { expanded = true }) {
                        Text(currentFilter ?: "All")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                onFilterChange(null)
                                expanded = false
                            }
                        )
                        allTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    onFilterChange(tag)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Content based on layout selection
            if (isListLayout) {
                LazyColumn(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredIndices) { _, originalIndex ->
                        val note = notes[originalIndex]
                        val noteParts = note.split("\n", limit = 2)
                        val noteTitle = noteParts.getOrElse(0) { "" }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(originalIndex, note) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = noteTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename)) },
                                        onClick = {
                                            onNoteClick(originalIndex, note)
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        onClick = {
                                            onNoteDelete(originalIndex)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Grid layout
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredIndices) { _, originalIndex ->
                        val note = notes[originalIndex]
                        val parts = note.split("\n", limit = 2)
                        val noteTitle = parts.getOrElse(0) { "" }
                        val noteSummary = parts.getOrElse(1) { "" }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { onNoteClick(originalIndex, note) }
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
                                            onNoteClick(originalIndex, note)
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        onClick = {
                                            onNoteDelete(originalIndex)
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
    var tagText by remember { mutableStateOf("") } // Tag input
    var errorMessage by remember { mutableStateOf("") }
    val duplicateNoteMessage = stringResource(R.string.duplicate_note_title)

    // System picker to insert an image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileBody += "\n[Image: $uri]"
            onContentChange("$fileName [${tagText.trim()}]\n$fileBody")
        }
    }

    // System picker for video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Video placeholder
            fileBody += "\n[Video: $uri]"
            onContentChange("$fileName [${tagText.trim()}]\n$fileBody")
        }
    }

    LaunchedEffect(noteContent) {
        val lines = noteContent.split("\n", limit = 2)
        var rawTitle = lines.getOrElse(0) { "" }
        val tagMatch = "\\[(.*?)\\]".toRegex().find(rawTitle)
        tagText = tagMatch?.groupValues?.get(1) ?: ""
        fileName = rawTitle.replace("\\[.*?\\]".toRegex(), "").trim()
        fileBody = lines.getOrElse(1) { "" }
    }

    Scaffold(
        // Top bar with back arrow
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
                    onContentChange("$fileName [${tagText.trim()}]\n$fileBody")
                },
                label = { Text(stringResource(R.string.file_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Add a field for tags
            OutlinedTextField(
                value = tagText,
                onValueChange = {
                    tagText = it
                    onContentChange("$fileName [${tagText.trim()}]\n$fileBody")
                },
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fileBody,
                onValueChange = {
                    fileBody = it
                    onContentChange("$fileName [${tagText.trim()}]\n$fileBody")
                },
                label = { Text(stringResource(R.string.file_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // Row for media insertion options
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

            // Save or save as options
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    if (fileName in existingNoteNames && fileName != originalFileName) {
                        errorMessage = duplicateNoteMessage
                    } else {
                        // Save using old format (without tags) for backend compatibility
                        onSave("$fileName\n$fileBody")
                    }
                }) {
                    Text(stringResource(R.string.save))
                }

                Button(onClick = {
                    if (fileName in existingNoteNames && fileName != originalFileName) {
                        errorMessage = duplicateNoteMessage
                    } else {
                        onSaveAs("$fileName\n$fileBody")
                    }
                }) {
                    Text(stringResource(R.string.save_as))
                }
            }
        }
    }
}
