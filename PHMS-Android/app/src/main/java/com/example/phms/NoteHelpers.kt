package com.example.phms

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.lang.StringBuilder
import androidx.compose.material.icons.filled.Close

@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Image") },
        text = { Text("Choose image source") },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onCameraSelected(); onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        onGallerySelected(); onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .padding(4.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
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

fun parseNoteContent(content: String): NoteContent {
    var id: Int? = null
    var title = ""
    var body = ""
    var tag = ""
    val imageUris = mutableListOf<String>()

    val lines = content.split("\n")

    if (lines.isNotEmpty()) {
        val first = lines[0]
        if (first.contains("|")) {
            val parts = first.split("|")
            id = parts[0].toIntOrNull()
            title = parts.getOrElse(1) { "" }
        } else title = first
    }

    if (lines.size > 1) {
        val bodyLines = mutableListOf<String>()
        for (line in lines.drop(1)) {
            when {
                line.startsWith("[Image: ") && line.endsWith("]") ->
                    imageUris += line.substring(8, line.length - 1)

                line.matches(Regex("^(diet|medication|health|misc)$")) ->
                    tag = line

                else -> bodyLines += line
            }
        }
        body = bodyLines.joinToString("\n")
    }

    return NoteContent(id, title, body, tag, imageUris)
}

fun formatNoteForSaving(
    id: Int?,
    title: String,
    body: String,
    tag: String,
    imageUris: List<String>
): String = buildString {
    append(if (id != null) "$id|$title\n" else "$title\n")
    append(body)
    imageUris.forEach { append("\n[Image: $it]") }
    if (tag.isNotEmpty()) append("\n$tag")
}

fun updateNoteContent(
    id: Int?,
    title: String,
    body: String,
    tag: String,
    imageUris: List<String>,
    onContentChange: (String) -> Unit
) {
    onContentChange(formatNoteForSaving(id, title, body, tag, imageUris))
}
