package com.example.phms

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.compose.ui.res.stringResource

@Composable
fun useNotesCamera(
    snackbarHostState: SnackbarHostState,
    onImageCaptured: (Uri) -> Unit
): Triple<() -> Unit, Boolean, () -> Unit> {
    val TAG = "NotesCamera"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val failedCaptureMsg = stringResource(R.string.error_capture_failed)
    val permissionDeniedMsg = stringResource(R.string.error_permission_denied_camera)
    val cameraLaunchErrorMsg = stringResource(R.string.error_camera_launch)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            Log.d(TAG, "Camera capture successful: $photoUri")
            onImageCaptured(photoUri!!)
        } else {
            Log.e(TAG, "Camera capture failed. Success: $success, URI: $photoUri")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(failedCaptureMsg)
            }
        }
    }

    fun createImageUri(): Uri? {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "img_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PHMS")
                }
                return context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            } else {

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFile = File(
                    context.getExternalFilesDir("Pictures"),
                    "JPEG_${timeStamp}_.jpg"
                ).apply {
                    parentFile?.mkdirs()
                }

                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image URI", e)
            return null
        }
    }

    fun launchCameraInternal(): Boolean {
        try {

            photoUri = createImageUri()

            if (photoUri != null) {
                Log.d(TAG, "Launching camera with URI: $photoUri")
                cameraLauncher.launch(photoUri!!)
                return true
            } else {
                Log.e(TAG, "Failed to create photo URI")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching camera", e)
            return false
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Camera permission granted: $isGranted")
        hasCameraPermission = isGranted
        if (isGranted) {
            launchCameraInternal()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(permissionDeniedMsg)
            }
        }
    }

    fun captureImage() {
        if (hasCameraPermission) {
            if (!launchCameraInternal()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(cameraLaunchErrorMsg)
                }
            }
        } else {

            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return Triple({ captureImage() }, hasCameraPermission, { launchCameraInternal() })
}