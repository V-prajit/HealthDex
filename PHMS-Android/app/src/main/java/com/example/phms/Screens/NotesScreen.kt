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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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

object NoteTagColors {
    val diet = Color(0xFFFAB038)
    val medication = Color(0xFF99D5FF)
    val health = Color(0xFF94CC7B)
    val misc = Color(0xFFE44E58)
    val images = Color(0xFFF48FB1)
    val default = Color(0xFF424242)

    fun getThemeColors(tag: String): Pair<Color, Color> {
        return when (tag.lowercase()) {
            "diet"       -> diet to Color.Black
            "medication" -> medication to Color.Black
            "health"     -> health to Color.Black
            "misc"       -> misc to Color.White
            "images"     -> images to Color.Black
            else         -> default to Color.White
        }
    }

    fun getDotColor(tag: String): Color {
        return when (tag.lowercase()) {
            "diet"       -> diet
            "medication" -> medication
            "health"     -> health
            "misc"       -> misc
            "images"     -> images
            else         -> default
        }
    }
}


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
        val displayDotColor = NoteTagColors.getDotColor(tag)
        AssistChip(
            onClick = {  },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color = displayDotColor)
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
                        val originalIndex = notes.indexOf(note)
                        if (originalIndex != -1) {
                            selectedNoteIndex = originalIndex
                            noteContent = note
                            currentScreen = "edit"
                        }
                    },
                    onNewNoteClick = {
                        selectedNoteIndex = null
                        noteContent = ""
                        currentScreen = "edit"
                    },
                    onNoteDelete = { noteToDelete ->
                        val originalIndexToDelete = notes.indexOf(noteToDelete)
                        if (originalIndexToDelete != -1) {
                            if (!userToken.isNullOrEmpty()) {
                                scope.launch {
                                    val noteId = notes[originalIndexToDelete].split("\n").firstOrNull()
                                        ?.split("|")?.getOrElse(0) { "" }?.toIntOrNull() ?: -1
                                    if(noteId != -1) {
                                        NotesRepositoryBackend.deleteNote(noteId)
                                        notes = NotesRepositoryBackend.getNotes(userToken)
                                    }
                                }
                            } else {
                                val mutableNotes = notes.toMutableList()
                                mutableNotes.removeAt(originalIndexToDelete)
                                notes = mutableNotes
                                NotesRepository.saveNotes(context, notes)
                            }
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
                        val isUpdatingExisting = selectedNoteIndex != null && selectedNoteIndex!! < notes.size

                        scope.launch {
                            if (!userToken.isNullOrEmpty()) {
                                NotesRepositoryBackend.saveNote(userToken, updatedContent)
                                notes = NotesRepositoryBackend.getNotes(userToken)
                            } else {
                                val mutableNotes = notes.toMutableList()
                                if (isUpdatingExisting) {
                                    mutableNotes[selectedNoteIndex!!] = updatedContent
                                } else {
                                    val title = updatedContent.split("\n").firstOrNull()?.trim() ?: ""
                                    val indexToUpdate = notes.indexOfFirst { it.split("\n").firstOrNull()?.trim() == title }
                                    if (indexToUpdate != -1) {
                                        mutableNotes[indexToUpdate] = updatedContent
                                    } else {
                                        mutableNotes.add(updatedContent)
                                    }
                                }
                                notes = mutableNotes
                                NotesRepository.saveNotes(context, notes)
                            }
                            currentScreen = "list"
                        }
                    },
                    onSaveAs = { updatedContent ->
                        scope.launch {
                            if (!userToken.isNullOrEmpty()) {
                                NotesRepositoryBackend.saveNote(userToken, updatedContent)
                                notes = NotesRepositoryBackend.getNotes(userToken)
                            } else {
                                notes = notes + updatedContent
                                NotesRepository.saveNotes(context, notes)
                            }
                            currentScreen = "list"
                        }
                    },
                    onCancel = {
                        currentScreen = "list"
                    },
                    originalFileName = if (selectedNoteIndex != null && selectedNoteIndex!! < notes.size) {
                        parseNoteContent(notes[selectedNoteIndex!!]).title
                    } else {
                        ""
                    },
                    existingNoteNames = notes.map { parseNoteContent(it).title },
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
    onNoteDelete: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val backLabel = stringResource(R.string.back)
    var isListLayout by remember { mutableStateOf(true) }
    val allTagsLabel = stringResource(R.string.all_tags)
    var selectedSortTag by remember { mutableStateOf(allTagsLabel) }
    val displayedNotes = remember(notes, selectedSortTag, allTagsLabel) {
        if (selectedSortTag == allTagsLabel) {
            notes
        } else {
            notes.filter {
                parseNoteContent(it).tag.equals(selectedSortTag, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backLabel)
                    }
                },
                actions = {
                    IconButton(onClick = { isListLayout = !isListLayout }) {
                        Icon(
                            imageVector = if (isListLayout) Icons.Default.GridView else Icons.Default.List,
                            contentDescription = if (isListLayout) stringResource(R.string.switch_to_grid) else stringResource(R.string.switch_to_list)
                        )
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
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            val tagOptions = listOf(stringResource(R.string.all_tags), "diet", "medication", "health", "misc", "images")
            var expandedSortMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.filter_by_tag, selectedSortTag), style = MaterialTheme.typography.bodyMedium)
                Box {
                IconButton(onClick = { expandedSortMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.select_tag))
                }
                DropdownMenu(
                    expanded = expandedSortMenu,
                    onDismissRequest = { expandedSortMenu = false }
                ) {
                    tagOptions.forEach { tag ->
                        val dotColor = if (tag == stringResource(R.string.all_tags)) Color.Transparent else NoteTagColors.getDotColor(tag)

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (tag != stringResource(R.string.all_tags)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(dotColor, shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
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
            }

            val listModifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)

            if (isListLayout) {
                LazyColumn(
                    modifier = listModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes, key = { _, note -> note.hashCode() }) { index, note ->
                        val parsedNote = parseNoteContent(note)
                        val tag = parsedNote.tag
                        val (bgColor, contentColor) = NoteTagColors.getThemeColors(tag)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(0.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = contentColor)
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
                                            text = parsedNote.title.ifBlank { stringResource(R.string.notes_untitled) },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon,
                                                contentDescription = stringResource(R.string.tagged_note_desc),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(end = 8.dp),
                                                tint = contentColor
                                            )
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), modifier = Modifier.size(18.dp), tint = contentColor)
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
                                                    onNoteDelete(note)
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
                                                contentDescription = stringResource(R.string.note_image_preview),
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { onImageClick(parsedNote.imageUris, parsedNote.imageUris.indexOf(uri)) },
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        if (parsedNote.imageUris.size > 3) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.Black.copy(alpha = 0.5f))
                                                        .clickable { onImageClick(parsedNote.imageUris, 3) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+${parsedNote.imageUris.size - 3}",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag)
                                    }
                                    if (parsedNote.imageUris.isNotEmpty()) {
                                        NoteTagSmall(tag = stringResource(R.string.images_tag))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = listModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes, key = { _, note -> note.hashCode() }) { index, note ->
                        val parsedNote = parseNoteContent(note)
                        val tag = parsedNote.tag
                        val (bgColor, contentColor) = NoteTagColors.getThemeColors(tag)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f)
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = contentColor)
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
                                        text = parsedNote.title.ifBlank { stringResource(R.string.notes_untitled) },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(end = 4.dp)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon,
                                                contentDescription = stringResource(R.string.tagged_note_desc),
                                                modifier = Modifier.size(18.dp),
                                                tint = contentColor
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { expanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = stringResource(R.string.more_options),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = contentColor
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
                                                        onNoteDelete(note)
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
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                if (parsedNote.imageUris.isNotEmpty()) {
                                    val previewImageUri = parsedNote.imageUris.first()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onImageClick(parsedNote.imageUris, 0) }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(previewImageUri),
                                            contentDescription = stringResource(R.string.note_image_preview),
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
                                                    .background(Color.Black.copy(alpha = 0.6f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "+${parsedNote.imageUris.size - 1}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall
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
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag)
                                    }
                                    if (parsedNote.imageUris.isNotEmpty()) {
                                        NoteTagSmall(tag = stringResource(R.string.images_tag))
                                    }
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
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(noteContent) {
        val parts = parseNoteContent(noteContent)
        originalId = parts.id
        fileName   = parts.title
        fileBody   = parts.body
        fileTag    = parts.tag
        imageUris  = parts.imageUris
    }

    val tagOptions = listOf("diet", "medication", "health", "misc")
    var expandedTagMenu by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateAction by remember { mutableStateOf<(String) -> Unit>({}) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Fetch strings needed outside lambdas or in multiple places ---
    val filenameEmptyMsg = stringResource(R.string.error_filename_empty)
    val permissionDeniedText = stringResource(R.string.error_permission_denied_camera)
    val addImageButtonText = stringResource(R.string.add_image_button)
    val attachedImagesTitleText = stringResource(R.string.attached_images_title)
    val saveAsButtonText = stringResource(R.string.save_as)
    val saveIconDesc = stringResource(R.string.save)
    val cancelIconDesc = stringResource(R.string.cancel_discard_desc)
    val editNoteTitleText = stringResource(R.string.edit_note)
    val createNoteTitleText = stringResource(R.string.create_note_title)
    val backIconDesc = stringResource(R.string.back)
    val fileNameLabelText = stringResource(R.string.file_name_label)
    val noteTitleHintText = stringResource(R.string.note_title_hint)
    val tagLabelText = stringResource(R.string.tag_label)
    val tagOptionalHintText = stringResource(R.string.tag_optional_hint)
    val fileContentLabelText = stringResource(R.string.file_content_label)
    val noteDetailsHintText = stringResource(R.string.note_details_hint)
    // --- End fetching common strings ---


    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<@JvmSuppressWildcards android.net.Uri> ->
        val newImageStrings = uris.map { it.toString() }
        imageUris = imageUris + newImageStrings
        updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
    }

    val (captureImage) = useNotesCamera(snackbarHostState) { uri ->
        uri?.let {
            imageUris = imageUris + it.toString()
            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                captureImage()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(permissionDeniedText) // Use variable
                }
            }
        }
    )

    // Define the lambda outside Scaffold if needed by multiple actions
    val performSaveAction = { action: (String) -> Unit, isSaveAs: Boolean ->
        val currentId = if (isSaveAs) null else originalId
        val trimmedFileName = fileName.trim()
        val noteToSave = formatNoteForSaving(currentId, trimmedFileName, fileBody, fileTag, imageUris)

        if (trimmedFileName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(filenameEmptyMsg) } // Use variable
        } else {
            val checkName = trimmedFileName
            val isDuplicate = existingNoteNames
                .filter { if (!isSaveAs) !it.equals(originalFileName, ignoreCase = true) else true }
                .any { it.equals(checkName, ignoreCase = true) }

            if (isDuplicate) {
                duplicateAction = action
                showDuplicateDialog = true
            } else {
                action(noteToSave)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(originalId != null) editNoteTitleText else createNoteTitleText) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backIconDesc)
                    }
                },
                actions = {
                    if (fileName.isNotBlank()) {
                        IconButton(onClick = { performSaveAction(onSave, false) }) {
                            Icon(Icons.Default.Save, contentDescription = saveIconDesc)
                        }
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = cancelIconDesc)
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text(fileNameLabelText) },
                placeholder = {Text(noteTitleHintText)},
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expandedTagMenu,
                onExpandedChange = { expandedTagMenu = !expandedTagMenu }
            ) {
                OutlinedTextField(
                    value = fileTag.ifBlank { tagOptionalHintText },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(tagLabelText) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTagMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable { expandedTagMenu = true }
                )
                ExposedDropdownMenu(
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

            OutlinedTextField(
                value = fileBody,
                onValueChange = {
                    fileBody = it
                    updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
                },
                label = { Text(fileContentLabelText) },
                placeholder = {Text(noteDetailsHintText)},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Button(
                onClick = { showImageSourceDialog = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(addImageButtonText)
            }

            if (imageUris.isNotEmpty()) {
                Text(attachedImagesTitleText, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top=8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical=8.dp)
                ) {
                    items(imageUris, key = { it }) { uri ->
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { performSaveAction(onSaveAs, true) },
                ) {
                    Icon(Icons.Default.SaveAs, saveAsButtonText)
                    Spacer(Modifier.width(8.dp))
                    Text(saveAsButtonText)
                }
            }

            Spacer(Modifier.height(72.dp))
        }

        if (showImageSourceDialog) {
            ImageSourceDialog(
                onDismiss = { showImageSourceDialog = false },
                onCameraSelected = {
                    showImageSourceDialog = false
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
                onGallerySelected = {
                    showImageSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }
            )
        }

        if (showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { showDuplicateDialog = false },
                title = { Text(stringResource(R.string.duplicate_dialog_title)) }, // OK to use stringResource here
                text = { Text(stringResource(R.string.duplicate_dialog_text, fileName)) }, // OK to use stringResource here
                confirmButton = {
                    Button(onClick = {
                        val noteToSave = formatNoteForSaving(null, fileName.trim(), fileBody, fileTag, imageUris)
                        duplicateAction(noteToSave)
                        showDuplicateDialog = false
                    }) {
                        Text(stringResource(R.string.replace_button)) // Fetch string inside Composable context
                    }
                },
                dismissButton = {
                    Button(onClick = { showDuplicateDialog = false }) {
                        Text(stringResource(R.string.cancel_rename_button)) // Fetch string inside Composable context
                    }
                }
            )
        }
    }
}