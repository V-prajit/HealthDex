package com.example.phms

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilities for handling image files in notes
 */
object NoteFileUtils {

    /**
     * Creates a temporary file for storing an image from the camera
     */
    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir("NoteImages")
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Create the directory if it doesn't exist
            parentFile?.mkdirs()
        }
    }

    /**
     * Gets a content URI for a file using FileProvider
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Saves a bitmap to a file in the app's private directory and returns the Uri
     */
    fun saveImageToStorage(context: Context, imageUri: Uri): Uri? {
        try {
            // Create a directory for stored images if it doesn't exist
            val imagesDir = File(context.filesDir, "note_images").apply {
                if (!exists()) mkdirs()
            }

            // Generate a unique filename
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = getExtension(context, imageUri) ?: "jpg"
            val filename = "Image_${timeStamp}.$extension"
            val destFile = File(imagesDir, filename)

            // Copy the content from the source URI to our destination file
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Return a URI for the saved file
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Get the file extension from a URI
     */
    private fun getExtension(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    /**
     * Persists the permanent URI references when saving a note
     * Converts any temporary URIs to permanent storage
     */
    fun persistImageUris(context: Context, uris: List<String>): List<String> {
        val persistedUris = mutableListOf<String>()

        for (uriString in uris) {
            try {
                val uri = Uri.parse(uriString)

                // Check if this is a content URI that needs to be persisted
                if (uri.scheme == "content") {
                    // Save to permanent storage and get new URI
                    val savedUri = saveImageToStorage(context, uri)
                    if (savedUri != null) {
                        persistedUris.add(savedUri.toString())
                    } else {
                        // If saving failed, keep the original URI
                        persistedUris.add(uriString)
                    }
                } else {
                    // Not a content URI or already persisted, keep as is
                    persistedUris.add(uriString)
                }
            } catch (e: Exception) {
                // If any error occurs, keep the original string
                persistedUris.add(uriString)
            }
        }

        return persistedUris
    }
}