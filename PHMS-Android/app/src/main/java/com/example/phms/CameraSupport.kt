package com.example.phms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * A composable dialog that lets users choose between camera and gallery
 * for adding images to notes
 */
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
                        onCameraSelected()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        onGallerySelected()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * A hook that provides camera functionality for the notes screen
 */
@Composable
fun useNotesCamera(
    onImageCaptured: (Uri) -> Unit
): Triple<() -> Unit, Boolean, () -> Unit> {
    val context = LocalContext.current

    // State for permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // URI for captured image
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            // Image captured successfully
            photoUri?.let { onImageCaptured(it) }
        }
    }

    // Function to launch the camera (internal implementation)
    fun launchCameraInternal() {
        try {
            // Create temporary file for the photo
            val photoFile = NoteFileUtils.createTempImageFile(context)
            photoUri = NoteFileUtils.getUriForFile(context, photoFile)

            // Launch camera with the URI
            photoUri?.let { cameraLauncher.launch(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Launcher for camera permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            // Permission granted, launch camera
            launchCameraInternal()
        }
    }

    // Function to check permission and launch camera
    fun captureImage() {
        if (hasCameraPermission) {
            launchCameraInternal()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return Triple(::captureImage, hasCameraPermission, ::launchCameraInternal)
}