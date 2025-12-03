package com.example.kaimera.camera.managers
import com.example.kaimera.core.managers.PreferencesManager
import com.example.kaimera.camera.utils.ImageCaptureHelper

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.kaimera.camera.managers.StorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Manages intervalometer (time-lapse) photography functionality.
 * 
 * Handles scheduled photo capture with configurable start delay, interval, and count.
 * Supports low-power sleep mode that unbinds the camera between shots to conserve battery.
 */
class IntervalometerManager(
    private val context: Context,
    private val getCameraProvider: () -> ProcessCameraProvider?,
    private val getImageCapture: () -> ImageCapture?,
    private val restartCamera: () -> Unit,
    private val preferencesManager: PreferencesManager,
    private val handler: Handler
) {
    companion object {
        private const val TAG = "IntervalometerManager"
        private const val WAKE_UP_LEAD_TIME_MS = 3000L
    }

    /**
     * Configuration for intervalometer session
     */
    data class Config(
        val startDelaySeconds: Int,
        val intervalSeconds: Double,
        val totalCount: Int, // 0 for infinite
        val lowPowerMode: Boolean
    )

    /**
     * Callback interface for UI updates
     */
    interface Callback {
        fun onCounterUpdate(current: Int, total: Int)
        fun onCountdownUpdate(secondsRemaining: Int)
        fun onSleepOverlayVisibility(visible: Boolean)
        fun onSleepCountdownUpdate(hours: Int, minutes: Int, seconds: Int)
        fun onComplete(photoCount: Int)
    }

    private var isRunning = false
    private var isSleeping = false
    private var config: Config? = null
    private var photoCount = 0
    private var runnable: Runnable? = null
    private var callback: Callback? = null

    /**
     * Start intervalometer session
     */
    fun start(config: Config, callback: Callback) {
        if (isRunning) {
            Log.w(TAG, "Intervalometer already running")
            return
        }

        this.config = config
        this.callback = callback
        this.isRunning = true
        this.photoCount = 0

        val startDelayMs = config.startDelaySeconds * 1000L

        // Check for initial sleep mode (long start delay)
        if (config.lowPowerMode && config.startDelaySeconds > 5) {
            enterSleepMode(System.currentTimeMillis() + startDelayMs)
            return
        }

        // Handle countdown or immediate start
        if (startDelayMs > 0) {
            startCountdown(startDelayMs)
        } else {
            capturePhoto()
        }
    }

    /**
     * Stop intervalometer session
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false
        isSleeping = false
        config = null
        
        // Cancel all pending runnables
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null

        // Hide sleep overlay if visible
        callback?.onSleepOverlayVisibility(false)

        // Rebind camera if we were sleeping
        if (isSleeping) {
            restartCamera()
        }

        // Notify completion
        callback?.onComplete(photoCount)
        callback = null
    }

    /**
     * Check if intervalometer is currently running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Start countdown before first capture
     */
    private fun startCountdown(delayMs: Long) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + delayMs

        runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    // Clear the runnable before capturing to prevent re-posting
                    runnable = null
                    capturePhoto()
                } else {
                    val seconds = (remaining / 1000) + 1
                    callback?.onCountdownUpdate(seconds.toInt())
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.post(runnable!!)
    }

    /**
     * Enter low-power sleep mode
     */
    private fun enterSleepMode(wakeUpTime: Long) {
        if (!isRunning) return
        isSleeping = true

        // Unbind camera to save power
        getCameraProvider()?.unbindAll()

        // Show sleep overlay
        callback?.onSleepOverlayVisibility(true)

        // Start sleep countdown
        runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                val now = System.currentTimeMillis()
                val remaining = wakeUpTime - now

                if (remaining <= WAKE_UP_LEAD_TIME_MS) {
                    wakeUpCamera(wakeUpTime)
                } else {
                    // Update countdown display
                    val seconds = (remaining / 1000)
                    val hours = (seconds / 3600).toInt()
                    val minutes = ((seconds % 3600) / 60).toInt()
                    val secs = (seconds % 60).toInt()

                    callback?.onSleepCountdownUpdate(hours, minutes, secs)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable!!)
    }

    /**
     * Wake up camera before next capture
     */
    private fun wakeUpCamera(captureTime: Long) {
        isSleeping = false
        callback?.onSleepOverlayVisibility(false)

        // Restart camera
        restartCamera()

        // Wait for remaining lead time then capture
        val now = System.currentTimeMillis()
        val remaining = captureTime - now

        if (remaining > 0) {
            handler.postDelayed({
                capturePhoto()
            }, remaining)
        } else {
            capturePhoto()
        }
    }

    /**
     * Capture a single photo in the intervalometer sequence
     */
    private fun capturePhoto() {
        if (!isRunning) return

        val currentConfig = config ?: return
        val imageCapture = getImageCapture()

        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is null, cannot capture photo")
            stop()
            return
        }

        // Create output file
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val saveLocationPref = sharedPreferences.getString("camera_save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("camera_file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("camera_custom_file_prefix", "IMG")

        // Get image format
        val imageFormat = preferencesManager.getImageFormat()
        val fileExtension = com.example.kaimera.camera.utils.ImageCaptureHelper.getFileExtension(imageFormat)

        val outputDirectory = StorageManager.getStorageLocation(context, saveLocationPref ?: "app_storage")
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val fileName = "${customPrefix ?: "IMG"}_INT_${timestamp}.$fileExtension"
        val photoFile = File(outputDirectory, fileName)

        // Capture image using ImageCaptureHelper
        com.example.kaimera.camera.utils.ImageCaptureHelper.captureImage(
            imageCapture = imageCapture,
            outputFile = photoFile,
            format = imageFormat,
            quality = preferencesManager.getPhotoQualityInt(),
            executor = ContextCompat.getMainExecutor(context),
            onSuccess = {
                photoCount++
                callback?.onCounterUpdate(photoCount, currentConfig.totalCount)

                // Check if we reached the limit
                if (currentConfig.totalCount > 0 && photoCount >= currentConfig.totalCount) {
                    // Wait one more interval then stop
                    val intervalMs = (currentConfig.intervalSeconds * 1000).roundToLong()
                    handler.postDelayed({
                        stop()
                    }, intervalMs)
                    return@captureImage
                }

                // Schedule next shot
                if (isRunning) {
                    scheduleNextCapture(currentConfig)
                }
            },
            onError = { exception ->
                Log.e(TAG, "Interval photo capture failed", exception)
                
                // Retry after interval
                if (isRunning) {
                    val intervalMs = (currentConfig.intervalSeconds * 1000).roundToLong()
                    handler.postDelayed({
                        capturePhoto()
                    }, intervalMs)
                }
            }
        )
    }

    /**
     * Schedule the next photo capture
     */
    private fun scheduleNextCapture(currentConfig: Config) {
        val intervalMs = (currentConfig.intervalSeconds * 1000).roundToLong()
        val nextShotTime = System.currentTimeMillis() + intervalMs

        // Check for sleep mode
        if (currentConfig.lowPowerMode && currentConfig.intervalSeconds > 5) {
            enterSleepMode(nextShotTime)
        } else {
            // Show countdown for intervals > 1 second
            if (intervalMs > 1000) {
                startIntervalCountdown(intervalMs)
            } else {
                handler.postDelayed({
                    capturePhoto()
                }, intervalMs)
            }
        }
    }

    /**
     * Start countdown between interval shots
     */
    private fun startIntervalCountdown(delayMs: Long) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + delayMs

        runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return

                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    // Clear the runnable before capturing to prevent re-posting
                    runnable = null
                    capturePhoto()
                } else {
                    val seconds = (remaining / 1000) + 1
                    callback?.onCountdownUpdate(seconds.toInt())
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.post(runnable!!)
    }
}
