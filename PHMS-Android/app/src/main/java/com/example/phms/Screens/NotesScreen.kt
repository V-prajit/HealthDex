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
import androidx.compose.foundation.lazy.grid.itemsIndexed // Keep this if used elsewhere
import androidx.compose.foundation.lazy.items // Keep this if used elsewhere
import androidx.compose.foundation.lazy.itemsIndexed // Use this one for lists with index
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CatchingPokemon // Assuming this icon represents tags generally
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
import androidx.compose.ui.graphics.Color // Needed for custom Color values
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

// Define colors centrally (optional but good practice)
object NoteTagColors {
    val diet = Color(0xFFFAB038) // Pikachu yellow
    val medication = Color(0xFF99D5FF) // Squirtle blue
    val health = Color(0xFF94CC7B) // Bulbasaur green
    val misc = Color(0xFFE44E58) // Charmander red
    val images = Color(0xFFF48FB1) // Light Pink
    val default = Color(0xFF424242) // Default gray

    // Function to get background and appropriate content color pair
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

    // Function to get just the main color (e.g., for dots)
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
    // This composable doesn't use tag-specific colors in the original code.
    // If you want it colored, you'd need to modify it similarly to NoteTagSmall.
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
        // Use the new color definitions
        val (dotColor, labelColor) = NoteTagColors.getThemeColors(tag)
        // Note: getThemeColors returns bg/content. For the dot, we want the primary color.
        // For the label on a neutral chip background, we might want a specific color.
        // Let's use the dotColor for the dot, and decide label color based on context.
        // Using the 'contentColor' from the pair ensures contrast if the chip *itself* was colored.
        // Since the chip background is neutral (surface), let's use the default text color or black/white.
        // Let's keep it simple: use the dotColor for the dot. Use default label color or black.

        val displayDotColor = NoteTagColors.getDotColor(tag)
        // Use a standard label color unless the tag implies needing contrast (like misc/default)
        val displayLabelColor = if (tag.lowercase() in listOf("misc", "default_tag_name")) Color.White else Color.Black


        AssistChip(
            onClick = { /* No action */ },
            // Keep chip background neutral, color the dot
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant // Standard contrast label
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color = displayDotColor) // Use the specific tag color for the dot
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = tag, style = MaterialTheme.typography.labelSmall)
                }
            },
            modifier = Modifier,
            shape = RoundedCornerShape(0.dp) // Original shape
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
                        // Find the original index if notes were filtered
                        val originalIndex = notes.indexOf(note)
                        if (originalIndex != -1) {
                            selectedNoteIndex = originalIndex
                            noteContent = note
                            currentScreen = "edit"
                        }
                        // Fallback if somehow not found (shouldn't happen with current logic)
                        // else { Log.e("NotesFullApp", "Clicked note not found in original list") }
                    },
                    onNewNoteClick = {
                        selectedNoteIndex = null
                        noteContent = ""
                        currentScreen = "edit"
                    },
                    onNoteDelete = { noteToDelete ->
                        // Find the original index to delete
                        val originalIndexToDelete = notes.indexOf(noteToDelete)
                        if (originalIndexToDelete != -1) {
                            if (!userToken.isNullOrEmpty()) {
                                scope.launch {
                                    val noteId = notes[originalIndexToDelete].split("\n").firstOrNull()
                                        ?.split("|")?.getOrElse(0) { "" }?.toIntOrNull() ?: -1 // Use -1 or handle error if ID parsing fails
                                    if(noteId != -1) { // Ensure we have a valid ID before trying to delete
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
                        // else { Log.e("NotesFullApp", "Note to delete not found in original list") }
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
                                NotesRepositoryBackend.saveNote(userToken, updatedContent) // Assumes backend handles create vs update
                                notes = NotesRepositoryBackend.getNotes(userToken) // Refresh list
                            } else {
                                val mutableNotes = notes.toMutableList()
                                if (isUpdatingExisting) {
                                    mutableNotes[selectedNoteIndex!!] = updatedContent
                                } else {
                                    // Check if title exists for replacement (part of original logic)
                                    val title = updatedContent.split("\n").firstOrNull()?.trim() ?: ""
                                    val indexToUpdate = notes.indexOfFirst { it.split("\n").firstOrNull()?.trim() == title }
                                    if (indexToUpdate != -1) {
                                        mutableNotes[indexToUpdate] = updatedContent
                                    } else {
                                        mutableNotes.add(updatedContent) // Add as new if not updating and title doesn't match existing
                                    }
                                }
                                notes = mutableNotes
                                NotesRepository.saveNotes(context, notes)
                            }
                            currentScreen = "list"
                        }
                    },
                    onSaveAs = { updatedContent -> // Save As always adds a new note
                        scope.launch {
                            if (!userToken.isNullOrEmpty()) {
                                NotesRepositoryBackend.saveNote(userToken, updatedContent) // Backend handles assigning new ID
                                notes = NotesRepositoryBackend.getNotes(userToken) // Refresh
                            } else {
                                notes = notes + updatedContent // Add to local list
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
                        "" // No original name if it's a new note
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
    onNoteClick: (Int, String) -> Unit, // Pass index AND note content string
    onNewNoteClick: () -> Unit,
    onNoteDelete: (String) -> Unit, // Pass note content string to delete
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val backLabel = stringResource(R.string.back)
    var isListLayout by remember { mutableStateOf(true) } // Default to list layout
    var selectedSortTag by remember { mutableStateOf("All") }

    // Filter notes based on the selected tag
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
                    // Layout toggle button removed as per original code comment
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
                    .padding(bottom = 72.dp) // Added padding as in original
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // Tag filter dropdown
            val tagOptions = listOf("All", "diet", "medication", "health", "misc", "images") // Added "images"
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
                        // Use new color function for the dot
                        val dotColor = if (tag == "All") Color.Transparent else NoteTagColors.getDotColor(tag)

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (tag != "All") { // Don't show dot for "All"
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

            val listModifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)

            if (isListLayout) {
                LazyColumn(
                    modifier = listModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes, key = { _, note -> note.hashCode() }) { index, note -> // Use key if content can change
                        val parsedNote = parseNoteContent(note)
                        val tag = parsedNote.tag
                        // Use new color function for background and content
                        val (bgColor, contentColor) = NoteTagColors.getThemeColors(tag)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(index, note) }, // Pass index relative to displayedNotes and the full note string
                            shape = RoundedCornerShape(0.dp), // Original shape
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = contentColor) // Apply new colors
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
                                            text = parsedNote.title.ifBlank { "(Untitled)" }, // Handle blank title
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Icon representing a tag exists
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon, // Or another relevant icon
                                                contentDescription = "Tagged Note",
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(end = 8.dp),
                                                tint = contentColor // Match icon tint to content color
                                            )
                                        }
                                        // More options menu
                                        var expanded by remember { mutableStateOf(false) }
                                        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More options", modifier = Modifier.size(18.dp), tint = contentColor)
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.rename)) }, // "Rename" implies editing
                                                onClick = {
                                                    onNoteClick(index, note) // Trigger edit flow
                                                    expanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.delete)) },
                                                onClick = {
                                                    onNoteDelete(note) // Pass the actual note string to delete
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                // Image Previews
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
                                                    .clip(RoundedCornerShape(4.dp)) // Slightly rounded corners for images
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
                                                        .background(Color.Black.copy(alpha = 0.5f)) // Dark overlay
                                                        .clickable { onImageClick(parsedNote.imageUris, 3) }, // Go to the 4th image
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+${parsedNote.imageUris.size - 3}",
                                                        color = Color.White, // White text on overlay
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Tags Row (including "Images" tag if applicable)
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Add spacing between tags
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag)
                                    }
                                    if (parsedNote.imageUris.isNotEmpty()) {
                                        // Use the specific "images" tag color defined
                                        NoteTagSmall(tag = "Images")
                                    }
                                }
                            }
                        }
                    }
                }
            } else { // Grid Layout (Less common based on original commented-out toggle)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = listModifier, // Reuse modifier
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayedNotes, key = { _, note -> note.hashCode() }) { index, note ->
                        val parsedNote = parseNoteContent(note)
                        val tag = parsedNote.tag
                        // Use new color function for background and content
                        val (bgColor, contentColor) = NoteTagColors.getThemeColors(tag)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f) // Maintain aspect ratio
                                .clickable { onNoteClick(index, note) }, // Pass index and note string
                            shape = RoundedCornerShape(8.dp), // Slightly more rounded for grid cards
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = contentColor) // Apply new colors
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                // Top row: Title and options menu
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top // Align items to top
                                ) {
                                    Text(
                                        text = parsedNote.title.ifBlank { "(Untitled)" },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false) // Take space needed, allow menu icon space
                                            .padding(end = 4.dp)
                                    )
                                    // Tag Icon and More options menu
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon,
                                                contentDescription = "Tagged Note",
                                                modifier = Modifier.size(18.dp),
                                                tint = contentColor // Match icon tint
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        var expanded by remember { mutableStateOf(false) }
                                        Box { // Wrap IconButton for better menu positioning if needed
                                            IconButton(
                                                onClick = { expanded = true },
                                                modifier = Modifier.size(32.dp) // Consistent size
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "More options",
                                                    modifier = Modifier.size(16.dp), // Smaller icon for grid
                                                    tint = contentColor // Match icon tint
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.rename)) },
                                                    onClick = {
                                                        onNoteClick(index, note) // Edit
                                                        expanded = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete)) },
                                                    onClick = {
                                                        onNoteDelete(note) // Delete by passing note string
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Body text (if present)
                                if (parsedNote.body.isNotBlank()) {
                                    Text(
                                        text = parsedNote.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3, // Limit lines in grid view
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f)) // Push image and tags to bottom

                                // Image Preview (if present)
                                if (parsedNote.imageUris.isNotEmpty()) {
                                    val previewImageUri = parsedNote.imageUris.first()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp) // Fixed height for preview
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onImageClick(parsedNote.imageUris, 0) } // Click opens viewer at first image
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(previewImageUri),
                                            contentDescription = "Image preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop // Crop to fit
                                        )
                                        // Image count indicator
                                        if (parsedNote.imageUris.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f)), // Semi-transparent indicator background
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "+${parsedNote.imageUris.size - 1}",
                                                    color = Color.White, // White text
                                                    style = MaterialTheme.typography.labelSmall // Smaller label
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp)) // Space below image
                                }

                                // Tags row at the bottom
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Space between tags
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag)
                                    }
                                    if (parsedNote.imageUris.isNotEmpty()) {
                                        NoteTagSmall(tag = "Images")
                                    }
                                    // Spacer might not be needed if tags are spacedBy
                                    // Spacer(modifier = Modifier.weight(1f)) // Pushes tags left if needed
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
    originalFileName: String, // Pass the original name for comparison during save
    existingNoteNames: List<String>, // Pass all existing names
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
) {
    // --- State Variables ---
    var fileName by remember { mutableStateOf("") }
    var fileBody by remember { mutableStateOf("") }
    var fileTag  by remember { mutableStateOf("") }
    var originalId by remember { mutableStateOf<Int?>(null) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    // LaunchedEffect to parse initial content
    LaunchedEffect(noteContent) {
        val parts = parseNoteContent(noteContent)
        originalId = parts.id
        fileName   = parts.title
        fileBody   = parts.body
        fileTag    = parts.tag
        imageUris  = parts.imageUris
    }

    // --- UI State ---
    val tagOptions = listOf("diet", "medication", "health", "misc") // Available tags
    var expandedTagMenu by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateAction by remember { mutableStateOf<(String) -> Unit>({}) }

    // --- Context, Scope, Snackbar ---
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val duplicateMsg = stringResource(R.string.duplicate_note_title) // Ensure this string exists

    // --- Image Handling ---

    // Gallery Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<@JvmSuppressWildcards android.net.Uri> ->
        val newImageStrings = uris.map { it.toString() }
        imageUris = imageUris + newImageStrings
        updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
    }

    // Assume useNotesCamera returns a function to capture image AFTER permission is granted
    // And potentially other things, but *not* the permission launcher itself for this approach
    val (captureImage /* , possibly other values */) = useNotesCamera(snackbarHostState) { uri ->
        uri?.let {
            imageUris = imageUris + it.toString()
            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
        }
    }

    // Define the Camera Permission Launcher HERE
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, now call the captureImage function
                captureImage()
            } else {
                // Permission denied, show snackbar
                scope.launch {
                    snackbarHostState.showSnackbar("Camera permission denied.")
                }
            }
        }
    )

    // --- Helper Function for Save/SaveAs Logic ---
    val performSaveAction = { action: (String) -> Unit, isSaveAs: Boolean ->
        val currentId = if (isSaveAs) null else originalId
        val trimmedFileName = fileName.trim()
        val noteToSave = formatNoteForSaving(currentId, trimmedFileName, fileBody, fileTag, imageUris)

        if (trimmedFileName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Filename cannot be empty.") }
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

    // --- UI Structure ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(originalId != null) stringResource(R.string.edit_note) else "Create Note") }, // Use "Create Note" or add R.string.create_note
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (fileName.isNotBlank()) {
                        IconButton(onClick = { performSaveAction(onSave, false) }) {
                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                        }
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel / Discard")
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
            // --- Title Input ---
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text(stringResource(R.string.file_name)) },
                placeholder = {Text("Enter note title")},
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Tag Selection ---
            ExposedDropdownMenuBox(
                expanded = expandedTagMenu,
                onExpandedChange = { expandedTagMenu = !expandedTagMenu }
            ) {
                OutlinedTextField(
                    value = fileTag.ifBlank { "Select Tag (Optional)" },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Tag") },
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
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            fileTag = ""
                            updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
                            expandedTagMenu = false
                        }
                    )
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

            // --- Body Input ---
            OutlinedTextField(
                value = fileBody,
                onValueChange = {
                    fileBody = it
                    updateNoteContent(originalId, fileName, fileBody, fileTag, imageUris, onContentChange)
                },
                label = { Text(stringResource(R.string.file_content)) },
                placeholder = {Text("Enter note details...")},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            // --- Image Section ---
            Button(
                onClick = { showImageSourceDialog = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.insert_image))
            }

            if (imageUris.isNotEmpty()) {
                Text("Attached Images:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top=8.dp))
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


            // --- Action Buttons ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { performSaveAction(onSaveAs, true) }, // Call helper
                ) {
                    Icon(Icons.Default.SaveAs, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_as))
                }
            }

            Spacer(Modifier.height(72.dp))
        } // End Column

        // --- Dialogs ---
        if (showImageSourceDialog) {
            ImageSourceDialog(
                onDismiss = { showImageSourceDialog = false },
                onCameraSelected = {
                    showImageSourceDialog = false
                    // Launch the locally defined permission launcher
                    permissionLauncher.launch(android.Manifest.permission.CAMERA) // THIS IS THE CORRECTED CALL
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
                title = { Text(duplicateMsg) },
                text = { Text("A note with the name \"$fileName\" already exists. Replace it or cancel to rename?") },
                confirmButton = {
                    Button(onClick = {
                        val noteToSave = formatNoteForSaving(null, fileName.trim(), fileBody, fileTag, imageUris)
                        duplicateAction(noteToSave)
                        showDuplicateDialog = false
                    }) { Text("Replace") }
                },
                dismissButton = {
                    Button(onClick = { showDuplicateDialog = false }) { Text("Cancel (Rename)") }
                }
            )
        }
    }
}