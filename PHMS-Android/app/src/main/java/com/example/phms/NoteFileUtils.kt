package com.example.phms

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object NoteFileUtils {
    private const val TAG = "NoteFileUtils"

    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.externalCacheDir

        storageDir?.mkdirs()

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            Log.d(TAG, "Created temp file: $absolutePath")
        }
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        ).also {
            Log.d(TAG, "Created URI: $it for file: ${file.absolutePath}")
        }
    }

    fun saveImageToStorage(context: Context, imageUri: Uri): Uri? {
        try {
            val imagesDir = File(context.filesDir, "note_images").apply {
                if (!exists()) mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = getExtension(context, imageUri) ?: "jpg"
            val filename = "Image_${timeStamp}.$extension"
            val destFile = File(imagesDir, filename)

            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Saved image to: ${destFile.absolutePath}")
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image", e)
            return null
        }
    }

    private fun getExtension(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    fun persistImageUris(context: Context, uris: List<String>): List<String> {
        val persistedUris = mutableListOf<String>()

        for (uriString in uris) {
            try {
                val uri = Uri.parse(uriString)

                if (uri.scheme == "content") {
                    val savedUri = saveImageToStorage(context, uri)
                    if (savedUri != null) {
                        persistedUris.add(savedUri.toString())
                        Log.d(TAG, "Persisted URI: $savedUri")
                    } else {
                        persistedUris.add(uriString)
                        Log.w(TAG, "Failed to persist URI, using original: $uriString")
                    }
                } else {
                    persistedUris.add(uriString)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting URI: $uriString", e)
                persistedUris.add(uriString)
            }
        }

        return persistedUris
    }
}