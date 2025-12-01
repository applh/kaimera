package com.example.kaimera.managers

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.example.kaimera.managers.StorageManager
import java.io.File

/**
 * Manages video recording functionality.
 * 
 * Handles video recording lifecycle, timer, and 120fps support.
 */
class VideoRecordingManager(
    private val context: Context,
    private val getVideoCapture: () -> VideoCapture<Recorder>?,
    private val handler: Handler
) {
    companion object {
        private const val TAG = "VideoRecordingManager"
    }

    /**
     * Callback interface for UI updates
     */
    interface Callback {
        fun onRecordingStarted()
        fun onRecordingStopped(fileName: String, success: Boolean)
        fun onTimerUpdate(minutes: Int, seconds: Int)
        fun onError(message: String)
    }

    private var recording: Recording? = null
    private var recordingSeconds = 0
    private var recordingTimerRunnable: Runnable? = null
    private var callback: Callback? = null

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = recording != null

    /**
     * Start video recording
     */
    fun startRecording(callback: Callback) {
        if (recording != null) {
            Log.w(TAG, "Already recording")
            return
        }

        val videoCapture = getVideoCapture()
        if (videoCapture == null) {
            callback.onError("Video capture not initialized")
            return
        }

        this.callback = callback

        // Get preferences
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val saveLocationPref = sharedPreferences.getString("camera_save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "VID")

        val outputDirectory = StorageManager.getStorageLocation(context, saveLocationPref ?: "app_storage")
        val fileName = if (namingPattern == "sequential") {
            StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "VID", "mp4")
        } else {
            StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "VID", "mp4")
        }

        val videoFile = File(outputDirectory, fileName)

        val outputOptions = StorageManager.createVideoOutputOptions(
            context,
            videoFile,
            saveLocationPref ?: "app_storage",
            fileName
        )

        val pendingRecording = if (outputOptions is FileOutputOptions) {
            videoCapture.output.prepareRecording(context, outputOptions)
        } else {
            videoCapture.output.prepareRecording(context, outputOptions as MediaStoreOutputOptions)
        }

        recording = pendingRecording
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // Start recording timer
                        recordingSeconds = 0
                        recordingTimerRunnable = object : Runnable {
                            override fun run() {
                                recordingSeconds++
                                val minutes = recordingSeconds / 60
                                val seconds = recordingSeconds % 60
                                callback.onTimerUpdate(minutes, seconds)
                                handler.postDelayed(this, 1000)
                            }
                        }
                        handler.post(recordingTimerRunnable!!)

                        callback.onRecordingStarted()
                        Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        // Stop recording timer
                        recordingTimerRunnable?.let { handler.removeCallbacks(it) }
                        recordingSeconds = 0

                        if (!recordEvent.hasError()) {
                            val savedUri = Uri.fromFile(videoFile)
                            callback.onRecordingStopped(videoFile.name, true)
                            Toast.makeText(context, "Video saved: ${videoFile.name}", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Video saved: $savedUri")
                        } else {
                            callback.onRecordingStopped(videoFile.name, false)
                            callback.onError("Video capture failed: ${recordEvent.error}")
                            Toast.makeText(context, "Video capture failed: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Video capture failed", recordEvent.cause)
                        }

                        recording = null
                    }
                }
            }
    }

    /**
     * Stop video recording
     */
    fun stopRecording() {
        if (recording == null) {
            Log.w(TAG, "Not recording")
            return
        }

        recording?.stop()
        recording = null

        // Stop recording timer
        recordingTimerRunnable?.let { handler.removeCallbacks(it) }
        recordingSeconds = 0

        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
    }

    /**
     * Get current recording duration in seconds
     */
    fun getRecordingSeconds(): Int = recordingSeconds

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopRecording()
    }
}
