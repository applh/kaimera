package com.example.kaimera

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object StorageManager {
    
    enum class SaveLocation {
        APP_STORAGE,
        SD_CARD,
        DCIM
    }
    
    enum class FileNamingPattern {
        TIMESTAMP,
        SEQUENTIAL,
        CUSTOM_PREFIX
    }
    
    /**
     * Get the storage directory based on user preference
     * Note: For DCIM, this returns app storage as fallback for file-based operations
     */
    fun getStorageLocation(context: Context, preference: String): File {
        return when (preference) {
            "sd_card" -> {
                // Try to get SD card path, fallback to app storage
                val externalDirs = context.getExternalFilesDirs(null)
                if (externalDirs.size > 1 && externalDirs[1] != null) {
                    externalDirs[1]!!
                } else {
                    context.getExternalFilesDir(null) ?: context.filesDir
                }
            }
            // For DCIM, we don't return a File object for direct access usually,
            // but we return app storage as a safe fallback for operations that require a File
            "dcim" -> context.getExternalFilesDir(null) ?: context.filesDir
            else -> context.getExternalFilesDir(null) ?: context.filesDir // Default: app_storage
        }
    }
    
    /**
     * Create OutputFileOptions for ImageCapture
     */
    fun createOutputFileOptions(
        context: Context,
        photoFile: File,
        saveLocationPref: String,
        fileName: String
    ): androidx.camera.core.ImageCapture.OutputFileOptions {
        return if (saveLocationPref == "dcim") {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kaimera")
                }
            }
            
            androidx.camera.core.ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()
        } else {
            androidx.camera.core.ImageCapture.OutputFileOptions.Builder(photoFile).build()
        }
    }
    
    /**
     * Create OutputOptions for VideoCapture
     */
    fun createVideoOutputOptions(
        context: Context,
        videoFile: File,
        saveLocationPref: String,
        fileName: String
    ): androidx.camera.video.OutputOptions {
        return if (saveLocationPref == "dcim") {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                    put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/Kaimera")
                }
            }
            
            androidx.camera.video.MediaStoreOutputOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()
        } else {
            androidx.camera.video.FileOutputOptions.Builder(videoFile).build()
        }
    }

    /**
     * Generate filename based on selected pattern
     */
    fun generateFileName(pattern: String, prefix: String, extension: String): String {
        return when (pattern) {
            "timestamp" -> {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                "${prefix}_${timestamp}.${extension}"
            }
            "sequential" -> {
                // Will be handled by caller to get next sequence number
                "${prefix}_SEQ.${extension}"
            }
            "custom_prefix" -> {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                "${prefix}_${timestamp}.${extension}"
            }
            else -> {
                // Default: timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                "${prefix}_${timestamp}.${extension}"
            }
        }
    }
    
    /**
     * Generate sequential filename
     */
    fun generateSequentialFileName(directory: File, prefix: String, extension: String): String {
        val existingFiles = directory.listFiles { file ->
            file.name.startsWith(prefix) && file.extension == extension
        } ?: emptyArray()
        
        val nextNumber = existingFiles.size + 1
        return "${prefix}_${String.format("%04d", nextNumber)}.${extension}"
    }
    
    /**
     * Calculate total storage usage in bytes
     */
    fun calculateStorageUsage(directory: File): Long {
        if (!directory.exists() || !directory.isDirectory) return 0L
        
        var totalSize = 0L
        directory.listFiles()?.forEach { file ->
            totalSize += if (file.isDirectory) {
                calculateStorageUsage(file)
            } else {
                file.length()
            }
        }
        return totalSize
    }
    
    /**
     * Format bytes to human-readable format
     */
    fun formatStorageSize(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            bytes >= gb -> String.format("%.2f GB", bytes / gb)
            bytes >= mb -> String.format("%.2f MB", bytes / mb)
            bytes >= kb -> String.format("%.2f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
    
    /**
     * Delete files older than specified days
     */
    fun deleteOldFiles(directory: File, daysToKeep: Int): Int {
        if (!directory.exists() || !directory.isDirectory) return 0
        
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        var deletedCount = 0
        
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        
        return deletedCount
    }
    
    /**
     * Get count of files in directory
     */
    fun getFileCount(directory: File): Int {
        if (!directory.exists() || !directory.isDirectory) return 0
        return directory.listFiles()?.count { it.isFile } ?: 0
    }
    
    /**
     * Check if SD card is available
     */
    fun isSDCardAvailable(context: Context): Boolean {
        val externalDirs = context.getExternalFilesDirs(null)
        return externalDirs.size > 1 && externalDirs[1] != null
    }
}
