package com.example.kamerai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "Kamerai"
    }

    enum class TimerDelay(val seconds: Int) {
        OFF(0), THREE_SEC(3), TEN_SEC(10)
    }

    enum class PhotoQuality(val jpegQuality: Int) {
        HIGH(95), MEDIUM(75), LOW(50)
    }

    enum class CaptureMode {
        PHOTO, VIDEO
    }

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: FloatingActionButton
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var timerDelay: TimerDelay = TimerDelay.OFF
    private var photoQuality: PhotoQuality = PhotoQuality.HIGH
    private var captureMode: CaptureMode = CaptureMode.PHOTO
    private var isBurstMode: Boolean = false
    private var burstCount: Int = 0
    private val burstInterval: Long = 200 // milliseconds between burst shots
    private val maxBurstCount: Int = 20
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        
        if (cameraGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        if (!audioGranted) {
            Toast.makeText(this, "Audio permission required for video recording", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        val galleryButton = findViewById<FloatingActionButton>(R.id.galleryButton)
        val switchButton = findViewById<FloatingActionButton>(R.id.switchButton)
        val flashButton = findViewById<FloatingActionButton>(R.id.flashButton)
        val gridButton = findViewById<FloatingActionButton>(R.id.gridButton)
        val gridOverlay = findViewById<GridOverlayView>(R.id.gridOverlay)
        val timerButton = findViewById<FloatingActionButton>(R.id.timerButton)
        val countdownText = findViewById<TextView>(R.id.countdownText)
        val qualityButton = findViewById<FloatingActionButton>(R.id.qualityButton)
        val modeButton = findViewById<FloatingActionButton>(R.id.modeButton)
        val recordingIndicator = findViewById<TextView>(R.id.recordingIndicator)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera and audio permissions
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (permissionsNeeded.isEmpty()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }

        // Set up capture button click and long-press listeners
        val burstCounter = findViewById<TextView>(R.id.burstCounter)
        
        captureButton.setOnClickListener {
            if (captureMode == CaptureMode.PHOTO) {
                takePhoto(countdownText)
            } else {
                toggleVideoRecording(recordingIndicator)
            }
        }

        // Long-press for burst mode (Photo mode only)
        captureButton.setOnLongClickListener {
            if (captureMode == CaptureMode.PHOTO && !isBurstMode) {
                startBurstMode(burstCounter)
                true
            } else {
                false
            }
        }

        // Stop burst on release
        captureButton.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP && isBurstMode) {
                stopBurstMode(burstCounter)
            }
            false // Return false to allow other listeners to process
        }

        // Set up gallery button click listener
        galleryButton.setOnClickListener {
            val intent = android.content.Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        // Set up switch button click listener
        switchButton.setOnClickListener {
            toggleCamera()
        }

        // Set up flash button click listener
        flashButton.setOnClickListener {
            toggleFlash(flashButton)
        }

        // Set up grid button click listener
        gridButton.setOnClickListener {
            if (gridOverlay.visibility == android.view.View.VISIBLE) {
                gridOverlay.visibility = android.view.View.GONE
                Toast.makeText(this, "Grid Off", Toast.LENGTH_SHORT).show()
            } else {
                gridOverlay.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Grid On", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up timer button click listener
        timerButton.setOnClickListener {
            timerDelay = when (timerDelay) {
                TimerDelay.OFF -> TimerDelay.THREE_SEC
                TimerDelay.THREE_SEC -> TimerDelay.TEN_SEC
                TimerDelay.TEN_SEC -> TimerDelay.OFF
            }
            
            val (stringRes, toastMsg) = when (timerDelay) {
                TimerDelay.OFF -> Pair(R.string.timer_off, "Timer Off")
                TimerDelay.THREE_SEC -> Pair(R.string.timer_3s, "Timer: 3s")
                TimerDelay.TEN_SEC -> Pair(R.string.timer_10s, "Timer: 10s")
            }
            
            timerButton.contentDescription = getString(stringRes)
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }

        // Set up quality button click listener
        qualityButton.setOnClickListener {
            photoQuality = when (photoQuality) {
                PhotoQuality.HIGH -> PhotoQuality.MEDIUM
                PhotoQuality.MEDIUM -> PhotoQuality.LOW
                PhotoQuality.LOW -> PhotoQuality.HIGH
            }
            
            val (stringRes, toastMsg) = when (photoQuality) {
                PhotoQuality.HIGH -> Pair(R.string.quality_high, "Quality: High (95%)")
                PhotoQuality.MEDIUM -> Pair(R.string.quality_medium, "Quality: Medium (75%)")
                PhotoQuality.LOW -> Pair(R.string.quality_low, "Quality: Low (50%)")
            }
            
            qualityButton.contentDescription = getString(stringRes)
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            
            // Restart camera to apply new quality setting
            startCamera()
        }

        // Set up mode button click listener
        modeButton.setOnClickListener {
            captureMode = if (captureMode == CaptureMode.PHOTO) CaptureMode.VIDEO else CaptureMode.PHOTO
            
            val (stringRes, icon, toastMsg) = when (captureMode) {
                CaptureMode.PHOTO -> Triple(R.string.mode_photo, android.R.drawable.ic_menu_camera, "Photo Mode")
                CaptureMode.VIDEO -> Triple(R.string.mode_video, android.R.drawable.ic_menu_gallery, "Video Mode")
            }
            
            modeButton.contentDescription = getString(stringRes)
            modeButton.setImageResource(icon)
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            
            // Restart camera to switch use cases
            startCamera()
        }
    }

    private fun toggleFlash(flashButton: FloatingActionButton) {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }

        imageCapture?.flashMode = flashMode

        val (iconRes, stringRes) = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> Pair(android.R.drawable.ic_menu_always_landscape_portrait, R.string.flash_on)
            ImageCapture.FLASH_MODE_AUTO -> Pair(android.R.drawable.ic_menu_compass, R.string.flash_auto)
            else -> Pair(android.R.drawable.ic_menu_close_clear_cancel, R.string.flash_off)
        }

        flashButton.setImageResource(iconRes)
        flashButton.contentDescription = getString(stringRes)
        Toast.makeText(this, stringRes, Toast.LENGTH_SHORT).show()
    }

    private fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                if (captureMode == CaptureMode.PHOTO) {
                    // ImageCapture use case for photos
                    imageCapture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setJpegQuality(photoQuality.jpegQuality)
                        .build()

                    // Bind use cases to camera
                    val camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )

                    // Set up pinch-to-zoom
                    setupPinchToZoom(camera)
                } else {
                    // VideoCapture use case for videos
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                    videoCapture = VideoCapture.withOutput(recorder)

                    // Bind use cases to camera
                    val camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, videoCapture
                    )

                    // Set up pinch-to-zoom
                    setupPinchToZoom(camera)
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupPinchToZoom(camera: androidx.camera.core.Camera) {
        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
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

    private fun takePhoto(countdownText: TextView) {
        if (timerDelay == TimerDelay.OFF) {
            capturePhoto()
        } else {
            startCountdown(countdownText)
        }
    }

    private fun startCountdown(countdownText: TextView) {
        var remainingSeconds = timerDelay.seconds
        countdownText.visibility = android.view.View.VISIBLE
        countdownText.text = remainingSeconds.toString()

        val countdownRunnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                if (remainingSeconds > 0) {
                    countdownText.text = remainingSeconds.toString()
                    handler.postDelayed(this, 1000)
                } else {
                    countdownText.visibility = android.view.View.GONE
                    capturePhoto()
                }
            }
        }
        handler.postDelayed(countdownRunnable, 1000)
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        // Create output file
        val photoFile = File(
            getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capture image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)

                    // Show toast and launch preview on main thread
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Capture Success", Toast.LENGTH_SHORT).show()
                        
                        // Launch PreviewActivity
                        val intent = android.content.Intent(this@MainActivity, PreviewActivity::class.java)
                        intent.putExtra("image_uri", savedUri.toString())
                        startActivity(intent)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Photo capture failed: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun startBurstMode(burstCounter: TextView) {
        isBurstMode = true
        burstCount = 0
        burstCounter.visibility = android.view.View.VISIBLE
        burstCounter.text = getString(R.string.burst_counter, burstCount)
        
        // Start capturing burst photos
        captureBurstPhoto(burstCounter)
    }

    private fun stopBurstMode(burstCounter: TextView) {
        isBurstMode = false
        burstCounter.visibility = android.view.View.GONE
        handler.removeCallbacksAndMessages(null) // Cancel any pending burst captures
        Toast.makeText(this, "Burst complete: $burstCount photos", Toast.LENGTH_SHORT).show()
    }

    private fun captureBurstPhoto(burstCounter: TextView) {
        if (!isBurstMode || burstCount >= maxBurstCount) {
            if (burstCount >= maxBurstCount) {
                stopBurstMode(burstCounter)
            }
            return
        }

        val imageCapture = imageCapture ?: return

        // Create output file with burst sequence number
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val photoFile = File(
            getExternalFilesDir(null),
            "${timestamp}-${String.format("%03d", burstCount + 1)}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capture image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    burstCount++
                    runOnUiThread {
                        burstCounter.text = getString(R.string.burst_counter, burstCount)
                    }
                    
                    // Schedule next burst photo if still in burst mode
                    if (isBurstMode && burstCount < maxBurstCount) {
                        handler.postDelayed({
                            captureBurstPhoto(burstCounter)
                        }, burstInterval)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Burst photo capture failed", exception)
                    // Continue burst even if one photo fails
                    if (isBurstMode && burstCount < maxBurstCount) {
                        handler.postDelayed({
                            captureBurstPhoto(burstCounter)
                        }, burstInterval)
                    }
                }
            }
        )
    }

    private fun toggleVideoRecording(recordingIndicator: TextView) {
        val videoCapture = this.videoCapture ?: return

        if (recording != null) {
            // Stop recording
            recording?.stop()
            recording = null
            recordingIndicator.visibility = android.view.View.GONE
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            return
        }

        // Check audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission required for video recording", Toast.LENGTH_SHORT).show()
            return
        }

        // Start recording
        val videoFile = File(
            getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        runOnUiThread {
                            recordingIndicator.visibility = android.view.View.VISIBLE
                            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        runOnUiThread {
                            recordingIndicator.visibility = android.view.View.GONE
                            if (!recordEvent.hasError()) {
                                val savedUri = Uri.fromFile(videoFile)
                                Toast.makeText(this, "Video saved: ${videoFile.name}", Toast.LENGTH_SHORT).show()
                                Log.d(TAG, "Video saved: $savedUri")
                            } else {
                                Toast.makeText(this, "Video capture failed: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "Video capture failed", recordEvent.cause)
                            }
                        }
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
