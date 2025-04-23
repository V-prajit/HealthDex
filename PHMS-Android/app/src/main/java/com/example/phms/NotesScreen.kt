package com.example.phms

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.example.phms.ImageSourceDialog
import com.example.phms.useNotesCamera

@Composable
fun NoteTag(tag: String) {
    if (tag.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        AssistChip(
            onClick = { },
            label = { Text(tag) },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun NoteTagSmall(tag: String) {
    if (tag.isNotEmpty()) {
        AssistChip(
            onClick = { },
            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun NotesFullApp(
    userToken: String? = null,
    onSettingsClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    newNoteRequested: Boolean = false
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    // Image viewer state variables
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialImageIndex by remember { mutableStateOf(0) }

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

    // Decide which screen to show
    if (showImageViewer) {
        // Show image viewer
        NoteImageViewer(
            imageUris = selectedImageUris,
            initialImageIndex = initialImageIndex,
            onClose = { showImageViewer = false }
        )
    } else {
        // Show normal screens
        when (currentScreen) {
            "list" -> {
                NotesListScreen(
                    notes = notes,
                    onNoteClick = { index, note ->
                        selectedNoteIndex = index
                        noteContent = note
                        currentScreen = "edit"
                        // notes index is saved and it moves to edit screen
                    },
                    onNewNoteClick = {
                        selectedNoteIndex = null
                        noteContent = ""
                        currentScreen = "edit"
                        //for new note, since it doesn't already have an index, idx is null
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
                    onSettingsClick = onSettingsClick,
                    onBackClick = onBackClick,
                    onImageClick = { uris, index ->
                        selectedImageUris = uris
                        initialImageIndex = index
                        showImageViewer = true
                    }
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
                    existingNoteNames = notes.map { it.split("\n").firstOrNull()?.trim() ?: "" },
                    onImageClick = { uris, index ->
                        selectedImageUris = uris
                        initialImageIndex = index
                        showImageViewer = true
                    }
                )
            }
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
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val backLabel = stringResource(R.string.back)
    var isListLayout by remember { mutableStateOf(true) }
    var selectedSortTag by remember { mutableStateOf("All") }

    // Filter notes based on selected tag
    val displayedNotes = if (selectedSortTag == "All") {
        notes
    } else {
        notes.filter { it.split("\n").getOrElse(2) { "" } == selectedSortTag }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backLabel)
                    }
                },
                actions = {
                    TextButton(onClick = { isListLayout = !isListLayout }) {
                        Text(text = if (isListLayout) stringResource(R.string.switch_to_grid) else stringResource(R.string.switch_to_list))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewNoteClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_note)) },
                modifier = Modifier
                    .padding(bottom = 72.dp, end = 16.dp)
                    .navigationBarsPadding()
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // Tag filter dropdown
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

            // Note list/grid
            val modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)

            if (isListLayout) {
                LazyColumn(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes) { index, note ->
                        val parsedNote = parseNoteContent(note)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = parsedNote.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (parsedNote.body.isNotBlank()) {
                                            Text(
                                                text = parsedNote.body.take(60),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }

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

                                if (parsedNote.imageUris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(parsedNote.imageUris.take(3)) { uri ->
                                            Image(
                                                painter = rememberAsyncImagePainter(uri),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { onImageClick(parsedNote.imageUris, parsedNote.imageUris.indexOf(uri)) },
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Show count if there are more images
                                        if (parsedNote.imageUris.size > 3) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                        .clickable { onImageClick(parsedNote.imageUris, 0) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+${parsedNote.imageUris.size - 3}",
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Show tag if present
                                if (parsedNote.tag.isNotEmpty()) {
                                    NoteTag(parsedNote.tag)
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
                    itemsIndexed(displayedNotes) { index, note ->
                        val parsedNote = parseNoteContent(note)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f)
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = parsedNote.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    var expanded by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
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

                                Spacer(modifier = Modifier.height(4.dp))

                                // Body preview
                                if (parsedNote.body.isNotBlank()) {
                                    Text(
                                        text = parsedNote.body.take(40),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Images preview
                                if (parsedNote.imageUris.isNotEmpty()) {
                                    val previewImageUri = parsedNote.imageUris.first()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(previewImageUri),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Show count if there are more images
                                        if (parsedNote.imageUris.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "+${parsedNote.imageUris.size - 1}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Tag
                                if (parsedNote.tag.isNotEmpty()) {
                                    NoteTagSmall(parsedNote.tag)
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
    existingNoteNames: List<String>,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    var fileName by remember { mutableStateOf("") }
    var fileBody by remember { mutableStateOf("") }
    var fileTag by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val duplicateNoteMessage = stringResource(R.string.duplicate_note_title)
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var originalId by remember { mutableStateOf<Int?>(null) }
    val tagOptions = listOf("diet", "medication", "health", "misc")
    var expandedTagMenu by remember { mutableStateOf(false) }

    // List to store image URIs
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Add the image URI to our separate list instead of the fileBody
            imageUris = imageUris + uri.toString()
            // Update the note content with the new structure
            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
        }
    }

    LaunchedEffect(noteContent) {
        val parts = parseNoteContent(noteContent)
        originalId = parts.id
        fileName = parts.title
        fileBody = parts.body
        fileTag = parts.tag
        imageUris = parts.imageUris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_note), style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Note Details", style = MaterialTheme.typography.titleMedium)

            // Main note content card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            errorMessage = ""
                            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
                        },
                        label = { Text(stringResource(R.string.file_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedTagMenu,
                        onExpandedChange = { expandedTagMenu = !expandedTagMenu }
                    ) {
                        OutlinedTextField(
                            value = fileTag,
                            onValueChange = { },
                            label = { Text("Tag") },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTagMenu)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
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
                                        updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
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
                            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
                        },
                        label = { Text(stringResource(R.string.file_content)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // Image section
            if (imageUris.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Images",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(imageUris) { uri ->
                                ImageThumbnail(
                                    uri = uri,
                                    onDelete = {
                                        imageUris = imageUris.filter { it != uri }
                                        updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
                                    },
                                    onClick = { onImageClick(imageUris, imageUris.indexOf(uri)) }
                                )
                            }
                        }
                    }
                }
            }

            // Image add button
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.insert_image))
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val noteToSave = formatNoteForSaving(originalId, fileName, fileBody, fileTag, imageUris)
                        if (fileName.isNotBlank() && existingNoteNames.filter { it != originalFileName }.any { it.equals(fileName, ignoreCase = true) }) {
                            showDuplicateDialog = true
                        } else {
                            onSave(noteToSave)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }

                Button(
                    onClick = {
                        val noteToSave = formatNoteForSaving(null, fileName, fileBody, fileTag, imageUris)
                        if (fileName.isNotBlank() && existingNoteNames.filter { it != originalFileName }.any { it.equals(fileName, ignoreCase = true) }) {
                            showDuplicateDialog = true
                        } else {
                            onSaveAs(noteToSave)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SaveAs, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_as))
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text(duplicateNoteMessage) },
                text = { Text("Rename or Replace?") },
                confirmButton = {
                    Button(onClick = {
                        val noteToSave = formatNoteForSaving(originalId, fileName, fileBody, fileTag, imageUris)
                        onSave(noteToSave)
                        showDuplicateDialog = false
                    }) {
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

@Composable
fun ImageThumbnail(
    uri: String,
    onDelete: () -> Unit,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Delete button overlay
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

data class NoteContent(
    val id: Int?,
    val title: String,
    val body: String,
    val tag: String,
    val imageUris: List<String>
)

// Parse the note content string into components
fun parseNoteContent(content: String): NoteContent {
    var id: Int? = null
    var title = ""
    var body = ""
    var tag = ""
    val imageUris = mutableListOf<String>()

    // Split into lines
    val lines = content.split("\n")

    // Process the first line (title/id)
    if (lines.isNotEmpty()) {
        val firstLine = lines[0]
        if (firstLine.contains("|")) {
            val parts = firstLine.split("|")
            id = parts[0].toIntOrNull()
            title = parts.getOrElse(1) { "" }
        } else {
            title = firstLine
        }
    }

    // Process the remaining lines
    if (lines.size > 1) {
        val remainingLines = lines.drop(1)

        // Extract images and build the actual body
        val bodyLines = mutableListOf<String>()

        for (line in remainingLines) {
            // If it's an image marker, extract it
            if (line.startsWith("[Image: ") && line.endsWith("]")) {
                val imageUri = line.substring(8, line.length - 1)
                imageUris.add(imageUri)
            }
            // If it's a tag marker
            else if (line.matches(Regex("^(diet|medication|health|misc)$"))) {
                tag = line
            }
            // Otherwise it's part of the body
            else {
                bodyLines.add(line)
            }
        }

        body = bodyLines.joinToString("\n")
    }

    return NoteContent(id, title, body, tag, imageUris)
}

// Format the note components back into a string for saving
fun formatNoteForSaving(id: Int?, title: String, body: String, tag: String, imageUris: List<String>): String {
    val sb = StringBuilder()

    // Add title with optional ID
    if (id != null) {
        sb.append("$id|$title\n")
    } else {
        sb.append("$title\n")
    }

    // Add body
    sb.append(body)

    // Add image markers
    for (uri in imageUris) {
        sb.append("\n[Image: $uri]")
    }

    // Add tag at the end
    if (tag.isNotEmpty()) {
        sb.append("\n$tag")
    }

    return sb.toString()
}

// Helper function to update note content
fun updateNoteContent(
    id: Int?,
    title: String,
    body: String,
    tag: String,
    imageUris: List<String>,
    onContentChange: (String) -> Unit
) {
    val content = formatNoteForSaving(id, title, body, tag, imageUris)
    onContentChange(content)
}