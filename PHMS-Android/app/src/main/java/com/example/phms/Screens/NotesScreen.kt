package com.example.phms.Screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.phms.ImageSourceDialog
import com.example.phms.ImageThumbnail
import com.example.phms.NoteImageViewer
import com.example.phms.R
import com.example.phms.formatNoteForSaving
import com.example.phms.parseNoteContent
import com.example.phms.repository.NotesRepository
import com.example.phms.repository.NotesRepositoryBackend
import com.example.phms.updateNoteContent
import com.example.phms.useNotesCamera
import kotlinx.coroutines.launch

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
        val tagThemeColors = when (tag.lowercase()) {
            "diet"       -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            "medication" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            "health"     -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            "misc"       -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            "images"     -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.5f) to MaterialTheme.colorScheme.onTertiaryContainer
            else         -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
        val dotColor = tagThemeColors.first
        val labelColor = tagThemeColors.second

        AssistChip(
            onClick = { /* No action */ },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = labelColor
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color = dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = tag, style = MaterialTheme.typography.labelSmall)
                }
            },
            modifier = Modifier,
            shape = RoundedCornerShape(0.dp)
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

    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialImageIndex by remember { mutableStateOf(0) }

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

    if (showImageViewer) {
        NoteImageViewer(
            imageUris = selectedImageUris,
            initialImageIndex = initialImageIndex,
            onClose = { showImageViewer = false }
        )
    } else {
        when (currentScreen) {
            "list" -> {
                NotesListScreen(
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
                        if (!userToken.isNullOrEmpty()) {
                            scope.launch {
                                val noteId = notes[index].split("\n").firstOrNull()
                                    ?.split("|")?.getOrElse(0) { "" }?.toIntOrNull() ?: (index + 1)
                                NotesRepositoryBackend.deleteNote(noteId)
                                notes = NotesRepositoryBackend.getNotes(userToken)
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
                                notes = NotesRepositoryBackend.getNotes(userToken)
                            }
                            else {
                                NotesRepository.saveNotes(context, notes)
                            }
                            currentScreen = "list"
                        }
                    },
                    onSaveAs = { updatedContent ->
                        val mutableNotes = notes.toMutableList()
                        mutableNotes.add(updatedContent)
                        notes = mutableNotes
                        scope.launch {
                            if (!userToken.isNullOrEmpty()) {
                                NotesRepositoryBackend.saveNote(userToken, updatedContent)
                                notes = NotesRepositoryBackend.getNotes(userToken)
                            }
                            else {
                                NotesRepository.saveNotes(context, notes)
                            }
                            currentScreen = "list"
                        }
                    },
                    onCancel = {
                        currentScreen = "list"
                    },
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

    val displayedNotes = remember(notes, selectedSortTag) {
        if (selectedSortTag == "All") {
            notes
        } else {
            notes.filter { noteString ->
                parseNoteContent(noteString).tag.equals(selectedSortTag, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backLabel)
                    }
                },
                actions = {
/*                    TextButton(onClick = { isListLayout = !isListLayout }) {
                        Text(text = if (isListLayout) stringResource(R.string.switch_to_grid) else stringResource(
                            R.string.switch_to_list
                        ))
                    }*/
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
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            val tagOptions = listOf("All", "diet", "medication", "health", "misc")
            var expandedSortMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        val tagThemeColors = when (tag.lowercase()) {
                            "diet"       -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                            "medication" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            "health"     -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                            "misc"       -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                            else         -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
                        }
                        val dotColor = tagThemeColors.first

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(dotColor, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tag)
                                }
                            },
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
                .padding(horizontal = 16.dp)

            if (isListLayout) {
                LazyColumn(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes) { index, note ->
                        val parsedNote = parseNoteContent(note)
                        val tag = parsedNote.tag
                        val themeColors = when (tag.lowercase()) {
                            "diet"       -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                            "medication" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            "health"     -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                            "misc"       -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                            else         -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val bgColor = themeColors.first
                        val contentColor = themeColors.second

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(0.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor).copy(contentColor = contentColor)
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
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon,
                                                contentDescription = "Note Icon",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(end = 8.dp)
                                            )
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(18.dp))
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
                                if (parsedNote.imageUris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(parsedNote.imageUris.take(3)) { uri ->
                                            Image(
                                                painter = rememberAsyncImagePainter(uri),
                                                contentDescription = "Note image preview",
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(0.dp))
                                                    .clickable {
                                                        onImageClick(
                                                            parsedNote.imageUris,
                                                            parsedNote.imageUris.indexOf(uri)
                                                        )
                                                    },
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        item {
                                            if (parsedNote.imageUris.size > 3) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(0.dp))
                                                        .background(
                                                            MaterialTheme.colorScheme.onSurface.copy(
                                                                alpha = 0.3f
                                                            )
                                                        )
                                                        .clickable {
                                                            onImageClick(
                                                                parsedNote.imageUris,
                                                                0
                                                            )
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+${parsedNote.imageUris.size - 3}",
                                                        color = MaterialTheme.colorScheme.surface,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag)
                                        if (parsedNote.imageUris.isNotEmpty()) Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (parsedNote.imageUris.isNotEmpty()) {
                                        NoteTagSmall(tag = "Images")
                                    }
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
                        val parsedNote = parseNoteContent(note)
                        val tag = parsedNote.tag

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f)
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(0.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (tag.lowercase()) {
                                    "diet"       -> MaterialTheme.colorScheme.secondaryContainer
                                    "medication" -> MaterialTheme.colorScheme.primaryContainer
                                    "health"     -> MaterialTheme.colorScheme.tertiaryContainer
                                    "misc"       -> MaterialTheme.colorScheme.surfaceVariant
                                    else         -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ).copy(
                                contentColor = when (tag.lowercase()) {
                                    "diet"       -> MaterialTheme.colorScheme.onSecondaryContainer
                                    "medication" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    "health"     -> MaterialTheme.colorScheme.onTertiaryContainer
                                    "misc"       -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else         -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
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
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(end = 4.dp)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon,
                                                contentDescription = "Note Icon",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { expanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "More options",
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
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (parsedNote.body.isNotBlank()) {
                                    Text(
                                        text = parsedNote.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (parsedNote.imageUris.isNotEmpty()) {
                                    val previewImageUri = parsedNote.imageUris.first()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(0.dp))
                                            .clickable { onImageClick(parsedNote.imageUris, 0) }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(previewImageUri),
                                            contentDescription = "Image preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (parsedNote.imageUris.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.8f
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "+${parsedNote.imageUris.size - 1}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag)
                                        if (parsedNote.imageUris.isNotEmpty()) Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if(parsedNote.imageUris.isNotEmpty()){
                                        NoteTagSmall(tag = "Images")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
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
    var fileTag  by remember { mutableStateOf("") }
    var originalId by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val tagOptions = listOf("diet", "medication", "health", "misc")
    var expandedTagMenu by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog  by remember { mutableStateOf(false) }
    val duplicateMsg = stringResource(R.string.duplicate_note_title)

    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUris += it.toString()
            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
        }
    }

    val (captureImage, _, _) = useNotesCamera(snackbarHostState) { uri ->
        imageUris += uri.toString()
        updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
    }

    LaunchedEffect(noteContent) {
        val parts = parseNoteContent(noteContent)
        originalId = parts.id
        fileName   = parts.title
        fileBody   = parts.body
        fileTag    = parts.tag
        imageUris  = parts.imageUris
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape  = RoundedCornerShape(0.dp),
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

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedTagMenu,
                        onExpandedChange = { expandedTagMenu = !expandedTagMenu }
                    ) {
                        OutlinedTextField(
                            value = fileTag,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Tag") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTagMenu) },
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

                    Spacer(Modifier.height(16.dp))

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

            if (imageUris.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape  = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Images", style = MaterialTheme.typography.titleMedium)

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

            Button(
                onClick = { showImageSourceDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.insert_image))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val noteToSave = formatNoteForSaving(originalId, fileName, fileBody, fileTag, imageUris)
                        if (fileName.isNotBlank() &&
                            existingNoteNames.filter { it != originalFileName }
                                .any { it.equals(fileName, true) }
                        ) {
                            showDuplicateDialog = true
                        } else {
                            onSave(noteToSave)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }

                Button(
                    onClick = {
                        val noteToSave = formatNoteForSaving(null, fileName, fileBody, fileTag, imageUris)
                        if (fileName.isNotBlank() &&
                            existingNoteNames.filter { it != originalFileName }
                                .any { it.equals(fileName, true) }
                        ) {
                            showDuplicateDialog = true
                        } else {
                            onSaveAs(noteToSave)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SaveAs, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_as))
                }
            }

            Spacer(Modifier.height(72.dp))
        }

        if (showImageSourceDialog) {
            ImageSourceDialog(
                onDismiss = { showImageSourceDialog = false },
                onCameraSelected  = { captureImage() },
                onGallerySelected = { imagePickerLauncher.launch("image/*") }
            )
        }

        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text(duplicateMsg) },
                text  = { Text("Rename or Replace?") },
                confirmButton = {
                    Button(onClick = {
                        val noteToSave = formatNoteForSaving(originalId, fileName, fileBody, fileTag, imageUris)
                        onSave(noteToSave)
                        showDuplicateDialog = false
                    }) { Text("Replace") }
                },
                dismissButton = {
                    Button(onClick = { showDuplicateDialog = false }) { Text("Rename") }
                }
            )
        }
    }
}