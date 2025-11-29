package com.example.kaimera.managers

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.example.kaimera.managers.StorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Manages burst mode photography functionality.
 * 
 * Handles rapid sequential photo capture with configurable count and interval.
 */
class BurstModeManager(
    private val context: Context,
    private val getImageCapture: () -> ImageCapture?,
    private val handler: Handler,
    private val maxCount: Int = 20,
    private val interval: Long = 200 // milliseconds between shots
) {
    companion object {
        private const val TAG = "BurstModeManager"
    }

    /**
     * Callback interface for UI updates
     */
    interface Callback {
        fun onBurstCounterUpdate(current: Int, max: Int)
        fun onBurstComplete(totalCount: Int)
    }

    private var isRunning = false
    private var photoCount = 0
    private var callback: Callback? = null

    /**
     * Start burst mode capture
     */
    fun start(callback: Callback) {
        if (isRunning) {
            Log.w(TAG, "Burst mode already running")
            return
        }

        this.callback = callback
        this.isRunning = true
        this.photoCount = 0

        // Notify initial counter
        callback.onBurstCounterUpdate(0, maxCount)

        // Start capturing
        capturePhoto()
    }

    /**
     * Stop burst mode capture
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false
        
        // Cancel pending captures
        handler.removeCallbacksAndMessages(null)

        // Notify completion
        callback?.onBurstComplete(photoCount)
        callback = null
    }

    /**
     * Check if burst mode is currently running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Capture a single photo in the burst sequence
     */
    private fun capturePhoto() {
        if (!isRunning || photoCount >= maxCount) {
            if (photoCount >= maxCount) {
                stop()
            }
            return
        }

        val imageCapture = getImageCapture()
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null, stopping burst mode")
            stop()
            return
        }

        // Get preferences
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "IMG")

        // Create output file
        val outputDirectory = StorageManager.getStorageLocation(context, saveLocationPref ?: "app_storage")
        val baseFileName = if (namingPattern == "sequential") {
            "${customPrefix ?: "IMG"}_BURST"
        } else {
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
            "${customPrefix ?: "IMG"}_${timestamp}"
        }

        val fileName = "${baseFileName}-${String.format("%03d", photoCount + 1)}.jpg"
        val photoFile = File(outputDirectory, fileName)

        val outputOptions = StorageManager.createOutputFileOptions(
            context,
            photoFile,
            saveLocationPref ?: "app_storage",
            fileName
        )

        // Capture image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photoCount++
                    callback?.onBurstCounterUpdate(photoCount, maxCount)

                    // Schedule next capture if still running
                    if (isRunning && photoCount < maxCount) {
                        handler.postDelayed({
                            capturePhoto()
                        }, interval)
                    } else if (photoCount >= maxCount) {
                        stop()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Burst photo capture failed", exception)
                    
                    // Retry if still running
                    if (isRunning && photoCount < maxCount) {
                        handler.postDelayed({
                            capturePhoto()
                        }, interval)
                    }
                }
            }
        )
    }
}
