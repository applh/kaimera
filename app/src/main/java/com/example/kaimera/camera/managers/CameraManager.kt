package com.example.kaimera.camera.managers
import com.example.kaimera.core.managers.PreferencesManager

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import com.example.kaimera.camera.ui.MainActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

import java.util.concurrent.ExecutorService

/**
 * Manages camera initialization, lifecycle binding, and use case setup.
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    
    // State
    var captureMode: MainActivity.CaptureMode = MainActivity.CaptureMode.PHOTO
        private set
    var flashMode: Int = ImageCapture.FLASH_MODE_OFF

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            this.cameraProvider = provider

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            try {
                // Unbind all use cases before rebinding
                provider.unbindAll()

                if (captureMode == MainActivity.CaptureMode.PHOTO) {
                    setupPhotoCapture(provider, preview)
                } else {
                    setupVideoCapture(provider, preview)
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPhotoCapture(provider: ProcessCameraProvider, preview: Preview) {
        // Update settings from preferences
        val photoQuality = preferencesManager.getPhotoQualityInt()
        
        val flashModePref = preferencesManager.getFlashMode()
        flashMode = when (flashModePref) {
            "auto" -> ImageCapture.FLASH_MODE_AUTO
            "on" -> ImageCapture.FLASH_MODE_ON
            "off" -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }

        // Capture Mode Preference
        val captureModePref = preferencesManager.getCaptureMode()
        val captureModeValue = if (captureModePref == "quality") {
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        } else {
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
        
        // Target Resolution Preference
        val resolutionPref = preferencesManager.getTargetResolution()
        val targetResolution = when (resolutionPref) {
            "12mp" -> Size(4000, 3000)
            "fhd" -> Size(1920, 1080)
            "hd" -> Size(1280, 720)
            else -> null // Max/Default
        }

        val imageCaptureBuilder = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setJpegQuality(photoQuality)
            .setCaptureMode(captureModeValue)
            
        // Handle rotation if context is Activity
        if (context is android.app.Activity) {
             imageCaptureBuilder.setTargetRotation(context.windowManager.defaultDisplay.rotation)
        }
            
        if (targetResolution != null) {
            imageCaptureBuilder.setTargetResolution(targetResolution)
        }
        
        imageCapture = imageCaptureBuilder.build()

        // Bind use cases to camera
        camera = provider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, imageCapture
        )

        setupPinchToZoom(camera!!)
    }

    @kotlin.OptIn(androidx.camera.video.ExperimentalHighSpeedVideo::class)
    private fun setupVideoCapture(provider: ProcessCameraProvider, preview: Preview) {
        val enable120fps = preferencesManager.is120fpsEnabled()
        
        if (enable120fps) {
            // Try to setup high-speed recording
            val highSpeedSupported = setupHighSpeedRecording(provider, cameraSelector)
            if (!highSpeedSupported) {
                // Fallback to standard recording
                Toast.makeText(context, "120fps not supported on this device, using standard recording", Toast.LENGTH_SHORT).show()
                setupStandardRecording(provider, cameraSelector, preview)
            }
        } else {
            // Standard recording
            setupStandardRecording(provider, cameraSelector, preview)
        }
    }

    private fun setupStandardRecording(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        preview: Preview
    ) {
        val quality = getVideoQuality()
        val fallback = FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality, fallback))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        // Bind use cases to camera
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, videoCapture
        )
        setupPinchToZoom(camera!!)
    }

    @androidx.camera.video.ExperimentalHighSpeedVideo
    private fun setupHighSpeedRecording(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector
    ): Boolean {
        try {
            Log.d(TAG, "Setting up HighSpeedVideoSession for 120fps using Camera2Interop...")
            
            // Get camera info
            val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
            
            // Check high-speed capabilities
            val highSpeedCapabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo)
            
            if (highSpeedCapabilities == null) {
                Log.d(TAG, "No high-speed capabilities available on this device")
                return false
            }
            
            Log.d(TAG, "Attempting to force 120fps via Camera2Interop")
            
            val previewBuilder = Preview.Builder()
            val extender = androidx.camera.camera2.interop.Camera2Interop.Extender(previewBuilder)
            extender.setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                android.util.Range(120, 120)
            )
            
            val newPreview = previewBuilder.build()
            newPreview.setSurfaceProvider(previewView.surfaceProvider)
            
            // Use standard recorder
            val quality = getVideoQuality()
            val fallback = FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(quality, fallback))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Bind use cases
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, newPreview, videoCapture
            )
            
            setupPinchToZoom(camera!!)
            
            Toast.makeText(context, "120fps Mode Active (Forced)", Toast.LENGTH_SHORT).show()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup high speed recording", e)
            return false
        }
    }

    private fun getVideoQuality(): Quality {
        val qualityString = preferencesManager.getVideoQuality()
        return when (qualityString) {
            "uhd" -> Quality.UHD
            "hd" -> Quality.HD
            else -> Quality.FHD
        }
    }

    private fun setupPinchToZoom(camera: Camera) {
        val scaleGestureDetector = android.view.ScaleGestureDetector(context, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        })

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    fun setCaptureMode(mode: MainActivity.CaptureMode) {
        captureMode = mode
        // Note: Caller needs to call startCamera() to apply changes
    }

    // Getters for other managers
    fun getImageCapture(): ImageCapture? = imageCapture
    fun getVideoCapture(): VideoCapture<Recorder>? = videoCapture
    fun getCameraProvider(): ProcessCameraProvider? = cameraProvider
    fun getCamera(): Camera? = camera
}
