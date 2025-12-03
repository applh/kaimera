package com.example.kaimera.core.utils

import android.webkit.MimeTypeMap
import java.io.File

/**
 * Centralized file utilities to avoid duplication across the app.
 * Consolidates functions from FileExplorerActivity, FileExplorerAdapter, and MmsActivity.
 */
object FileUtils {
    
    /**
     * Format file size in bytes to human-readable format (B, KB, MB, GB).
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Get MIME type for a file based on its extension.
     */
    fun getMimeType(file: File): String? {
        val extension = getFileExtension(file)
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            null
        }
    }
    
    /**
     * Get file extension without the dot.
     */
    fun getFileExtension(file: File): String {
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < name.length - 1) {
            name.substring(lastDot + 1)
        } else {
            ""
        }
    }
    
    /**
     * Check if file is an image based on MIME type.
     */
    fun isImageFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        return mimeType?.startsWith("image/") == true
    }
    
    /**
     * Check if file is a video based on MIME type.
     */
    fun isVideoFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        return mimeType?.startsWith("video/") == true
    }
    
    /**
     * Check if file is an audio file based on MIME type.
     */
    fun isAudioFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        return mimeType?.startsWith("audio/") == true
    }
    
    /**
     * Check if file is a text file based on MIME type.
     */
    fun isTextFile(file: File): Boolean {
        val mimeType = getMimeType(file)
        return mimeType?.startsWith("text/") == true
    }
    
    /**
     * Check if MIME type is valid for MMS (image, video, or audio).
     */
    fun isValidMmsFileType(mimeType: String?): Boolean {
        return mimeType?.let {
            it.startsWith("image/") || it.startsWith("video/") || it.startsWith("audio/")
        } ?: false
    }
    
    /**
     * Recursively delete a file or directory.
     */
    fun deleteRecursive(fileOrDirectory: File): Boolean {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        return fileOrDirectory.delete()
    }
}
