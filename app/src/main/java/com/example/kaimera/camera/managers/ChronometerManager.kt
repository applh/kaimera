package com.example.kaimera.camera.managers

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.example.kaimera.camera.managers.StorageManager
import java.io.File

/**
 * Manages chronometer (stopwatch) functionality with optional audio recording.
 * 
 * Handles time tracking and synchronized audio recording.
 */
class ChronometerManager(
    private val context: Context,
    private val handler: Handler
) {
    companion object {
        private const val TAG = "ChronometerManager"
    }

    /**
     * Callback interface for UI updates
     */
    interface Callback {
        fun onTimeUpdate(minutes: Int, seconds: Int)
        fun onStateChanged(running: Boolean)
        fun onAudioRecordingStarted()
        fun onAudioRecordingStopped(fileName: String)
    }

    private var isRunning = false
    private var seconds = 0
    private var runnable: Runnable? = null
    private var callback: Callback? = null
    
    // Audio recording
    private var audioRecorder: android.media.MediaRecorder? = null
    private var isRecordingAudio = false
    private var audioFilePath: String? = null

    /**
     * Start chronometer with optional audio recording
     */
    fun start(withAudio: Boolean, callback: Callback) {
        if (isRunning) {
            Log.w(TAG, "Chronometer already running")
            return
        }

        this.callback = callback
        this.isRunning = true

        // Start audio recording if requested
        if (withAudio) {
            startAudioRecording()
        }

        // Start time tracking
        runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                
                seconds++
                val minutes = seconds / 60
                val secs = seconds % 60
                callback.onTimeUpdate(minutes, secs)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable!!)
        
        callback.onStateChanged(true)
    }

    /**
     * Stop chronometer and audio recording
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false

        // Stop time tracking
        runnable?.let {
            handler.removeCallbacks(it)
        }
        runnable = null

        // Stop audio recording if active
        if (isRecordingAudio) {
            stopAudioRecording()
        }

        callback?.onStateChanged(false)
    }

    /**
     * Reset chronometer to zero
     */
    fun reset() {
        stop()
        seconds = 0
        callback?.onTimeUpdate(0, 0)
    }

    /**
     * Get current elapsed time in seconds
     */
    fun getElapsedSeconds(): Int = seconds

    /**
     * Check if chronometer is currently running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Check if audio is being recorded
     */
    fun isRecordingAudio(): Boolean = isRecordingAudio

    /**
     * Start audio recording
     */
    private fun startAudioRecording() {
        try {
            val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            val saveLocationPref = sharedPreferences.getString("camera_save_location", "app_storage")
            val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
            val customPrefix = sharedPreferences.getString("custom_file_prefix", "AUDIO")

            val outputDirectory = StorageManager.getStorageLocation(context, saveLocationPref ?: "app_storage")
            val fileName = if (namingPattern == "sequential") {
                StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "AUDIO", "m4a")
            } else {
                StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "AUDIO", "m4a")
            }

            audioFilePath = File(outputDirectory, fileName).absolutePath

            audioRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }

            audioRecorder?.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecordingAudio = true
            callback?.onAudioRecordingStarted()
            Toast.makeText(context, "Audio recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            Toast.makeText(context, "Failed to start audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Stop audio recording
     */
    private fun stopAudioRecording() {
        try {
            audioRecorder?.apply {
                stop()
                release()
            }
            audioRecorder = null
            isRecordingAudio = false

            val fileName = audioFilePath?.substringAfterLast("/") ?: "unknown"
            callback?.onAudioRecordingStopped(fileName)
            Toast.makeText(context, "Audio saved: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio recording", e)
            Toast.makeText(context, "Failed to stop audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stop()
        audioRecorder?.release()
        audioRecorder = null
    }
}
