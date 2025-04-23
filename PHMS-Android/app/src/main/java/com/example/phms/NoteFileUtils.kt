package com.example.phms

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object NoteFileUtils {

    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.externalCacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            parentFile?.mkdirs()
        }
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
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

            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
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
                    } else {
                        persistedUris.add(uriString)
                    }
                } else {
                    persistedUris.add(uriString)
                }
            } catch (e: Exception) {
                persistedUris.add(uriString)
            }
        }

        return persistedUris
    }
}