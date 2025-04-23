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

/**
 * A hook that provides camera functionality for the notes screen.
 * Returns:
 *   1) captureImage() – call this to open the camera
 *   2) hasCameraPermission – state value
 *   3) launchCameraInternal() – low-level launcher (rarely needed)
 */
@Composable
fun useNotesCamera(
    snackbarHostState: SnackbarHostState,
    onImageCaptured: (Uri) -> Unit
): Triple<() -> Unit, Boolean, () -> Unit> {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ─── permission state ────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // ─── ActivityResult launchers ────────────────────────────────────────
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onImageCaptured(photoUri!!)
        }
    }

    fun launchCameraInternal(): Boolean = try {
        val photoFile = NoteFileUtils.createTempImageFile(context)
        photoUri = NoteFileUtils.getUriForFile(context, photoFile)
        photoUri?.let { cameraLauncher.launch(it) }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) launchCameraInternal()
    }

    // ─── public function: captureImage() ─────────────────────────────────
    fun captureImage() {
        val activity = context as? Activity
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false

        if (!hasCameraPermission && !shouldShowRationale) {
            // permanently denied
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Camera permission permanently denied. Enable it in App Settings."
                )
            }
            return
        }

        if (hasCameraPermission) {
            if (!launchCameraInternal()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Could not open camera – see logs or check storage path."
                    )
                }
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    return Triple({ captureImage() }, hasCameraPermission, { launchCameraInternal() })
}
