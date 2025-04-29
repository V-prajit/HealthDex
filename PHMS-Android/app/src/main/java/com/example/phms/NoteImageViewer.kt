package com.example.phms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteImageViewer(
    imageUris: List<String>,
    initialImageIndex: Int = 0,
    onClose: () -> Unit
) {
    if (imageUris.isEmpty()) {
        onClose()
        return
    }

    var currentImageIndex by remember { mutableStateOf(initialImageIndex.coerceIn(0, imageUris.size - 1)) }
    val currentImageUri = imageUris[currentImageIndex]


    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }


    LaunchedEffect(currentImageIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }


    val state = rememberTransformableState { zoomChange, offsetChange, _ ->

        scale = (scale * zoomChange).coerceIn(0.5f, 5f)


        val maxX = (scale - 1) * 500f
        val maxY = (scale - 1) * 500f

        offsetX = (offsetX + offsetChange.x).coerceIn(-maxX, maxX)
        offsetY = (offsetY + offsetChange.y).coerceIn(-maxY, maxY)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.note_image_viewer_title, currentImageIndex + 1, imageUris.size)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {

            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(currentImageUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = stringResource(R.string.note_image_content_desc),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )


            if (imageUris.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    FilledTonalIconButton(
                        onClick = {
                            if (currentImageIndex > 0) {
                                currentImageIndex--
                            }
                        },
                        enabled = currentImageIndex > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous_image)
                        )
                    }


                    FilledTonalIconButton(
                        onClick = {
                            if (currentImageIndex < imageUris.size - 1) {
                                currentImageIndex++
                            }
                        },
                        enabled = currentImageIndex < imageUris.size - 1
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_image)
                        )
                    }
                }
            }


            Text(
                stringResource(R.string.zoom_pan_instructions),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}