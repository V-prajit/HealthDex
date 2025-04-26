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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// NoteTag remains unused in the list/grid views now, but kept for potential future use
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

// NoteTagSmall is used in Grid view and List view
@Composable
fun NoteTagSmall(tag: String) {
    if (tag.isNotEmpty()) {
        // Color for the small dot inside the chip
        val dotColor = when (tag.lowercase()) {
            "diet"      -> Color(0xFFFAB038) // Pikachu yellow
            "medication"-> Color(0xFF99D5FF) // Squirtle blue
            "health"    -> Color(0xFF94CC7B) // Bulbasaur green
            "misc"      -> Color(0xFFE44E58) // Charmander red
            // Use Light Pink for the "Images" tag dot
            "images"    -> Color(0xFFF48FB1) // Light Pink
            else        -> Color(0xFF424242) // Default dot color
        }

        // Define chip colors: black background, white text
        val chipColors = AssistChipDefaults.assistChipColors(
            containerColor = Color.Black,
            labelColor = Color.White
        )
        // Define chip border (optional, can make it subtle)
        val chipBorder = BorderStroke(
            width = 1.dp,
            color = Color.DarkGray // Subtle border
        )


        AssistChip(
            onClick = { /* No action needed for display tag */ },
            // Apply the custom colors and border
            colors = chipColors,
            border = chipBorder,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use dotColor for the background of this Box
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color = dotColor) // Keep the colored dot
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Text color is handled by chipColors.labelColor now
                    Text(text = tag, style = MaterialTheme.typography.labelSmall)
                }
            },
            modifier = Modifier, // Removed top padding
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

    // Filter notes using parseNoteContent for accuracy
    val displayedNotes = remember(notes, selectedSortTag) { // Recompute when notes or filter changes
        if (selectedSortTag == "All") {
            notes
        } else {
            notes.filter { noteString ->
                parseNoteContent(noteString).tag.equals(selectedSortTag, ignoreCase = true) // Case-insensitive comparison
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Title changed from "Notes" resource to "Field Notes"
                title = { Text("Field Notes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = backLabel)
                    }
                },
                actions = {
                    TextButton(onClick = { isListLayout = !isListLayout }) {
                        Text(text = if (isListLayout) stringResource(R.string.switch_to_grid) else stringResource(
                            R.string.switch_to_list
                        ))
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
                        //tag color next to text
                        val tagColor = when (tag.lowercase()) {
                            "diet"      -> Color(0xFFFAB038)
                            "medication"-> Color(0xFF99D5FF)
                            "health"    -> Color(0xFF94CC7B)
                            "misc"      -> Color(0xFFE44E58)
                            else        -> Color(0xFF424242)
                        }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(tagColor, shape = CircleShape)
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
                        val tag = parsedNote.tag // Use parsed tag
                        // Determming background color based on tag, use surfaceContainerLow as default
                        val bgColor = when (tag.lowercase()) {
                            "diet" -> Color(0xFFFAB038) // Pikachu yellow
                            "medication" -> Color(0xFF99D5FF) // Squirtle blue
                            "health" -> Color(0xFF94CC7B) // Bulbasaur green
                            "misc" -> Color(0xFFE44E58) // Charmander red
                            // +fix Use surfaceContainerLow as the default color
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        // Determine content color based on background for contrast
                        val contentColor = when (tag.lowercase()) {
                            "diet", "medication", "health", "misc" -> Color.Black // Explicit black for colored backgrounds
                            else -> LocalContentColor.current // Default for surfaceContainerLow
                        }


                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(0.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            // Set background and determined content color
                            colors = CardDefaults.cardColors(containerColor = bgColor).copy(contentColor = contentColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Top Row: Title and Icons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Title Column
                                    Column {
                                        Text(
                                            text = parsedNote.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    // Spacer to push icons right
                                    Spacer(Modifier.weight(1f))

                                    // Placeholder icon based on tag
                                    if (parsedNote.tag.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Filled.CatchingPokemon, // Placeholder
                                            contentDescription = "Note Icon",
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    // More options menu
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

                                // Row for tags (original and "Images")
                                Row(
                                    modifier = Modifier.padding(top = 8.dp), // Add padding above the tag row
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag) // Display original tag
                                        // Add spacer only if both tags are present
                                        if (parsedNote.imageUris.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                    }
                                    // +new Display "Images" tag if note has images
                                    if (parsedNote.imageUris.isNotEmpty()) {
                                        NoteTagSmall(tag = "Images")
                                    }
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
                        val tag = parsedNote.tag // Use parsed tag
                        // +fix Determine background color based on tag, use surfaceContainerLow as default
                        val bgColor = when (tag.lowercase()) {
                            "diet" -> Color(0xFFFAB038) // Pikachu yellow
                            "medication" -> Color(0xFF99D5FF) // Squirtle blue
                            "health" -> Color(0xFF94CC7B) // Bulbasaur green
                            "misc" -> Color(0xFFE44E58) // Charmander red
                            // +fix Use surfaceContainerLow as the default color
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        // Determine content color based on background for contrast
                        val contentColor = when (tag.lowercase()) {
                            "diet", "medication", "health", "misc" -> Color.Black // Explicit black for colored backgrounds
                            else -> LocalContentColor.current // Default for surfaceContainerLow
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f) // Adjust aspect ratio if needed
                                .clickable { onNoteClick(index, note) },
                            shape = RoundedCornerShape(0.dp), // Slightly different rounding for grid?
                            elevation = CardDefaults.cardElevation(4.dp),
                            // Set background and determined content color
                            colors = CardDefaults.cardColors(containerColor = bgColor).copy(contentColor = contentColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp) // Adjust padding for grid if needed
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top // Align items to the top
                                ) {
                                    // Title and Body text pushed to the left
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = parsedNote.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (parsedNote.body.isNotBlank()) {
                                            Text(
                                                text = parsedNote.body.take(40), // Shorter preview for grid
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }


                                    // Icons pushed to the right
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Added placeholder icon similar to list view
                                        if (parsedNote.tag.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Filled.CatchingPokemon, // Placeholder
                                                contentDescription = "Note Icon",
                                                modifier = Modifier.size(20.dp) // Adjust size/padding for grid
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp)) // Space between icons
                                        // Edit/Delete menu
                                        var expanded by remember { mutableStateOf(false) }
                                        Box { // Wrap IconButton to control its size/position better
                                            IconButton(
                                                onClick = { expanded = true },
                                                modifier = Modifier.size(32.dp) // Smaller touch target for grid
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp) // Smaller icon
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

                                // Spacer to push tag to the bottom
                                Spacer(modifier = Modifier.weight(1f))

                                // Image preview (optional) - Keep if desired
                                if (parsedNote.imageUris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val previewImageUri = parsedNote.imageUris.first()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp) // Fixed height for image preview
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(previewImageUri),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(0.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Image count badge
                                        if (parsedNote.imageUris.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp) // Smaller badge
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
                                    Spacer(modifier = Modifier.height(4.dp)) // Space after image
                                }


                                // Tag chips (original and "Images") at the bottom left
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top=4.dp), // Add padding above tags
                                    verticalAlignment = Alignment.CenterVertically // Align tags vertically
                                ) {
                                    if (parsedNote.tag.isNotEmpty()) {
                                        NoteTagSmall(parsedNote.tag) // Using the NoteTagSmall (black background chip)
                                        // Add spacer only if both tags are present
                                        if (parsedNote.imageUris.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                    }
                                    // Display images tag if note has images
                                    if(parsedNote.imageUris.isNotEmpty()){
                                        NoteTagSmall(tag = "Images")
                                    }
                                    Spacer(modifier = Modifier.weight(1f)) // Pushes tags left
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
    //state
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

    // helpers
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

    // initialise from incoming note
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

            // Edit Card - Color does NOT change when image is added
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

                    // Tag dropdown
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

            // insert image button
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
                // Save
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

                // Save As
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

            Spacer(Modifier.height(72.dp))   // bottom padding for scroll
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