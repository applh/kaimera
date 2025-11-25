package com.example.kamerai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
import java.util.Date
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
    // Data class representing a filter
    data class CameraFilter(val name: String, val matrix: ColorMatrix?)

    private var isBurstMode: Boolean = false
    private var burstCount: Int = 0
    private val burstInterval: Long = 200 // milliseconds between burst shots
    private val maxBurstCount: Int = 20
    private var currentFilter: CameraFilter? = null
    private val handler = Handler(Looper.getMainLooper())

    // Chronometer state
    private var chronometerRunning = false
    private var chronometerSeconds = 0
    private var chronometerRunnable: Runnable? = null
    private var audioRecorder: android.media.MediaRecorder? = null
    private var isRecordingAudio = false
    private var audioFilePath: String? = null
    private var orientationEventListener: OrientationEventListener? = null

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

    override fun onResume() {
        super.onResume()
        applyPreferences()
    }

    private fun applyPreferences() {
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        
        // Apply Grid Overlay
        val showGrid = sharedPreferences.getBoolean("grid_overlay", false)
        val gridOverlay = findViewById<GridOverlayView>(R.id.gridOverlay)
        gridOverlay.visibility = if (showGrid) android.view.View.VISIBLE else android.view.View.GONE
        
        // Apply Photo Quality
        val quality = sharedPreferences.getString("photo_quality", "high")
        photoQuality = when (quality) {
            "high" -> PhotoQuality.HIGH
            "medium" -> PhotoQuality.MEDIUM
            "low" -> PhotoQuality.LOW
            else -> PhotoQuality.HIGH
        }
        
        // Apply Flash Mode
        val flashModePref = sharedPreferences.getString("flash_mode", "auto")
        flashMode = when (flashModePref) {
            "auto" -> ImageCapture.FLASH_MODE_AUTO
            "on" -> ImageCapture.FLASH_MODE_ON
            "off" -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        
        // Apply Chronometer visibility
        val showChronometer = sharedPreferences.getBoolean("enable_chronometer", false)
        val chronometerPanel = findViewById<android.widget.LinearLayout>(R.id.chronometerPanel)
        chronometerPanel?.visibility = if (showChronometer) android.view.View.VISIBLE else android.view.View.GONE
        
        // Shutter sound is handled during capture
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        val galleryButton = findViewById<FloatingActionButton>(R.id.galleryButton)
        val switchButton = findViewById<FloatingActionButton>(R.id.switchButton)
        val timerButton = findViewById<FloatingActionButton>(R.id.timerButton)
        val countdownText = findViewById<TextView>(R.id.countdownText)
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

        // Set up settings button click listener
        val settingsButton = findViewById<FloatingActionButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Set up switch button click listener
        switchButton.setOnClickListener {
            toggleCamera()
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


     // Set up filter button and selector
        val filterButton = findViewById<FloatingActionButton>(R.id.filterButton)
        val filterSelector = findViewById<RecyclerView>(R.id.filterSelector)
        
        val filters = createFilters()
        val filterAdapter = FilterAdapter(filters) { filter ->
            currentFilter = filter
            filterButton.contentDescription = filter.name
            filterSelector.visibility = android.view.View.GONE
            Toast.makeText(this, "Filter: ${filter.name}", Toast.LENGTH_SHORT).show()
        }
        
        filterSelector.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filterSelector.adapter = filterAdapter
        
        // Initialize OrientationEventListener
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageCapture?.targetRotation = rotation
                videoCapture?.targetRotation = rotation
            }
        }
    
        filterButton.setOnClickListener {
            filterSelector.visibility = if (filterSelector.visibility == android.view.View.VISIBLE) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }

        // Set up chronometer controls
        val chronometerDisplay = findViewById<TextView>(R.id.chronometerDisplay)
        val chronoStartButton = findViewById<MaterialButton>(R.id.chronoStartButton)
        val chronoStartWithAudioButton = findViewById<MaterialButton>(R.id.chronoStartWithAudioButton)
        val chronoStopButton = findViewById<MaterialButton>(R.id.chronoStopButton)
        val chronoResetButton = findViewById<MaterialButton>(R.id.chronoResetButton)

        chronoStartButton.setOnClickListener {
            startChronometer(chronometerDisplay, chronoStartButton, chronoStartWithAudioButton, chronoStopButton, false)
        }

        chronoStartWithAudioButton.setOnClickListener {
            startChronometer(chronometerDisplay, chronoStartButton, chronoStartWithAudioButton, chronoStopButton, true)
        }

        chronoStopButton.setOnClickListener {
            stopChronometer(chronoStartButton, chronoStartWithAudioButton, chronoStopButton)
        }

        chronoResetButton.setOnClickListener {
            resetChronometer(chronometerDisplay, chronoStartButton, chronoStartWithAudioButton, chronoStopButton)
        }
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
                        .setTargetRotation(windowManager.defaultDisplay.rotation)
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
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val playSound = sharedPreferences.getBoolean("shutter_sound", true)
        
        if (playSound) {
            android.media.MediaActionSound().play(android.media.MediaActionSound.SHUTTER_CLICK)
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Apply filter to saved image if any
                    val filter = currentFilter
                    val matrix = filter?.matrix
                    val savedUri = Uri.fromFile(photoFile)
                    if (matrix != null) {
                        try {
                            val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            val filteredBitmap = Bitmap.createBitmap(
                                originalBitmap.width,
                                originalBitmap.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(filteredBitmap)
                            val paint = android.graphics.Paint()
                            paint.colorFilter = ColorMatrixColorFilter(matrix)
                            canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
                            // Overwrite file
                            val out = java.io.FileOutputStream(photoFile)
                            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, photoQuality.jpegQuality, out)
                            out.flush()
                            out.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to apply filter", e)
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(baseContext, "Photo captured: ${photoFile.name}", Toast.LENGTH_SHORT).show()
                        
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

    private fun startChronometer(
        display: TextView,
        startButton: MaterialButton,
        startWithAudioButton: MaterialButton,
        stopButton: MaterialButton,
        withAudio: Boolean
    ) {
        chronometerRunning = true
        
        // Hide start buttons, show stop button
        startButton.visibility = android.view.View.GONE
        startWithAudioButton.visibility = android.view.View.GONE
        stopButton.visibility = android.view.View.VISIBLE
        
        // Start audio recording if requested
        if (withAudio) {
            startAudioRecording()
        }
        
        chronometerRunnable = object : Runnable {
            override fun run() {
                chronometerSeconds++
                val minutes = chronometerSeconds / 60
                val seconds = chronometerSeconds % 60
                display.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(chronometerRunnable!!)
    }

    private fun stopChronometer(
        startButton: MaterialButton,
        startWithAudioButton: MaterialButton,
        stopButton: MaterialButton
    ) {
        chronometerRunning = false
        
        // Show start buttons, hide stop button
        startButton.visibility = android.view.View.VISIBLE
        startWithAudioButton.visibility = android.view.View.VISIBLE
        stopButton.visibility = android.view.View.GONE
        
        chronometerRunnable?.let {
            handler.removeCallbacks(it)
        }
        
        // Stop audio recording if active
        if (isRecordingAudio) {
            stopAudioRecording()
        }
    }

    private fun resetChronometer(
        display: TextView,
        startButton: MaterialButton,
        startWithAudioButton: MaterialButton,
        stopButton: MaterialButton
    ) {
        stopChronometer(startButton, startWithAudioButton, stopButton)
        chronometerSeconds = 0
        display.text = "00:00"
    }

    private fun startAudioRecording() {
        try {
            val outputDir = getExternalFilesDir(null)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            audioFilePath = "${outputDir}/AUDIO_$timestamp.m4a"
            
            audioRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(this)
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
            Toast.makeText(this, "Audio recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        try {
            audioRecorder?.apply {
                stop()
                release()
            }
            audioRecorder = null
            isRecordingAudio = false
            Toast.makeText(this, "Audio saved: ${audioFilePath?.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to stop audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener?.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener?.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up chronometer
        chronometerRunnable?.let {
            handler.removeCallbacks(it)
        }
        // Clean up audio recorder
        if (isRecordingAudio) {
            stopAudioRecording()
        }
        cameraExecutor.shutdown()
    }
    // Helper to create list of available filters
    private fun createFilters(): List<CameraFilter> {
        return listOf(
            CameraFilter(getString(R.string.filter_none), null),
            CameraFilter(getString(R.string.filter_grayscale), ColorMatrix().apply { setSaturation(0f) }),
            CameraFilter(getString(R.string.filter_sepia), ColorMatrix().apply {
                setScale(1f, 0.95f, 0.82f, 1f)
                val sepia = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                setConcat(ColorMatrix(sepia), this)
            }),
            CameraFilter(getString(R.string.filter_vivid), ColorMatrix().apply { setScale(1.2f, 1.2f, 1.2f, 1f) }),
            CameraFilter(getString(R.string.filter_cool), ColorMatrix().apply {
                setRotate(0, -10f)
                setRotate(1, -10f)
            })
        )
    }
}
