package com.example.kaimera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.ExperimentalHighSpeedVideo
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {


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
    private var cameraProvider: ProcessCameraProvider? = null
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
    
    // Recording timer state
    private var recordingSeconds = 0
    private var recordingTimerRunnable: Runnable? = null
    
    // Level indicator
    private lateinit var levelIndicator: LevelIndicatorView
    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null
    private lateinit var hdrButton: FloatingActionButton
    
    // Intervalometer state
    data class IntervalometerConfig(
        val startDelaySeconds: Int,
        val intervalSeconds: Double,
        val totalCount: Int, // 0 for infinite
        val lowPowerMode: Boolean
    )
    
    private var isIntervalometerRunning = false
    private var isSleeping = false
    private var intervalometerConfig: IntervalometerConfig? = null
    private var intervalometerCount = 0
    private var intervalometerRunnable: Runnable? = null
    
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = it.values[0]
                val y = it.values[1]

                
                // Calculate orientation angle
                val angle = Math.toDegrees(kotlin.math.atan2(x.toDouble(), y.toDouble())).toFloat()
                
                levelIndicator.updateAngle(angle)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not needed
        }
    }

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
        checkAutoDelete()
        
        // Register sensor listener if level indicator is enabled
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val levelIndicatorEnabled = sharedPreferences.getBoolean("enable_level_indicator", true)
        if (levelIndicatorEnabled) {
            gravitySensor?.let {
                sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
            
            // Apply sensitivity preference
            val sensitivity = sharedPreferences.getInt("level_indicator_sensitivity", 5)
            levelIndicator.setThreshold(sensitivity.toFloat())
            
            // Apply crosshair size preference
            val crosshairSize = sharedPreferences.getInt("level_indicator_crosshair_size", 20)
            levelIndicator.setCrosshairSizePercentage(crosshairSize)
            
            // Apply circle size preference
            val circleSize = sharedPreferences.getInt("level_indicator_circle_size", 10)
            levelIndicator.setCircleSizePercentage(circleSize)
            
            levelIndicator.visibility = android.view.View.VISIBLE
        } else {
            levelIndicator.visibility = android.view.View.GONE
        }
        
        if (allPermissionsGranted()) {
            startCamera()
        }
    }
    
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "Kaimera"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private fun checkAutoDelete() {
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val autoDeleteEnabled = sharedPreferences.getBoolean("auto_delete_enabled", false)
        
        if (autoDeleteEnabled) {
            val keepFilesFor = sharedPreferences.getString("keep_files_for", "30")?.toIntOrNull() ?: 30
            if (keepFilesFor > 0) {
                val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
                val directory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
                
                cameraExecutor.execute {
                    val deletedCount = StorageManager.deleteOldFiles(directory, keepFilesFor)
                    if (deletedCount > 0) {
                        runOnUiThread {
                            Toast.makeText(this, "Cleaned up $deletedCount old files", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun applyPreferences() {
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        
        // Apply Grid Overlay
        val showGrid = sharedPreferences.getBoolean("grid_overlay", false)
        val gridOverlay = findViewById<GridOverlayView>(R.id.gridOverlay)
        gridOverlay.visibility = if (showGrid) android.view.View.VISIBLE else android.view.View.GONE
        
        // Apply Grid nb of rows and columns
        val rows = sharedPreferences.getInt("grid_rows", 3)
        val columns = sharedPreferences.getInt("grid_columns", 3)
        gridOverlay.setGridSize(rows, columns)
        
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
        
        // Apply HDR button state
        val hdrEnabled = sharedPreferences.getBoolean("hdr_enabled", false)
        if (captureMode == CaptureMode.PHOTO) {
            hdrButton.visibility = android.view.View.VISIBLE
            hdrButton.backgroundTintList = if (hdrEnabled) {
                android.content.res.ColorStateList.valueOf(getColor(android.R.color.holo_orange_light))
            } else {
                android.content.res.ColorStateList.valueOf(getColor(android.R.color.darker_gray))
            }
        } else {
            hdrButton.visibility = android.view.View.GONE
        }
        
        // Shutter sound is handled during capture
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        levelIndicator = findViewById(R.id.levelIndicator)
        hdrButton = findViewById(R.id.hdrButton)
        
        // Initialize sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
        
        if (permissionsNeeded.isNotEmpty()) {
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
        val filterContainer = findViewById<android.widget.LinearLayout>(R.id.filterContainer)
        
        val filters = createFilters()
        val filterAdapter = FilterAdapter(filters) { filter ->
            currentFilter = filter
            filterButton.contentDescription = filter.name
            filterContainer.visibility = android.view.View.GONE
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
            filterContainer.visibility = if (filterContainer.visibility == android.view.View.VISIBLE) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }

        // Set up intervalometer button
        val intervalometerButton = findViewById<FloatingActionButton>(R.id.intervalometerButton)
        intervalometerButton.setOnClickListener {
            showIntervalometerDialog()
        }

        // Set up HDR button click listener
        hdrButton.setOnClickListener {
            val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val currentHdrEnabled = sharedPreferences.getBoolean("hdr_enabled", false)
            val newHdrEnabled = !currentHdrEnabled
            
            // Save the new HDR setting
            sharedPreferences.edit().putBoolean("hdr_enabled", newHdrEnabled).apply()
            
            // Update button appearance
            hdrButton.backgroundTintList = if (newHdrEnabled) {
                android.content.res.ColorStateList.valueOf(getColor(android.R.color.holo_orange_light))
            } else {
                android.content.res.ColorStateList.valueOf(getColor(android.R.color.darker_gray))
            }
            
            // Show toast
            val message = if (newHdrEnabled) "HDR Enabled" else "HDR Disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
            // Restart camera to apply HDR setting
            startCamera()
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

    @kotlin.OptIn(androidx.camera.video.ExperimentalHighSpeedVideo::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

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

                if (captureMode == CaptureMode.PHOTO) {
                    // ImageCapture use case for photos
                    val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                    
                    // Capture Mode
                    val captureModePref = sharedPreferences.getString("capture_mode_preference", "latency")
                    val captureModeValue = if (captureModePref == "quality") {
                        ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    } else {
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    }
                    
                    // Target Resolution
                    val resolutionPref = sharedPreferences.getString("target_resolution", "max")
                    val targetResolution = when (resolutionPref) {
                        "12mp" -> android.util.Size(4000, 3000)
                        "fhd" -> android.util.Size(1920, 1080)
                        "hd" -> android.util.Size(1280, 720)
                        else -> null // Max/Default
                    }

                    val imageCaptureBuilder = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setJpegQuality(photoQuality.jpegQuality)
                        .setCaptureMode(captureModeValue)
                        .setTargetRotation(windowManager.defaultDisplay.rotation)
                        
                    if (targetResolution != null) {
                        imageCaptureBuilder.setTargetResolution(targetResolution)
                    }
                    
                    imageCapture = imageCaptureBuilder.build()

                    // Bind use cases to camera
                    val camera = provider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )

                    // Set up pinch-to-zoom
                    setupPinchToZoom(camera)
                } else {
                    // VideoCapture use case for videos
                    val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                    val enable120fps = sharedPreferences.getBoolean("enable_120fps", false)
                    
                    if (enable120fps) {
                        // Try to setup high-speed recording
                        val highSpeedSupported = setupHighSpeedRecording(provider, cameraSelector, preview)
                        if (!highSpeedSupported) {
                            // Fallback to standard recording
                            Toast.makeText(this, "120fps not supported on this device, using standard recording", Toast.LENGTH_SHORT).show()
                            setupStandardRecording(provider, cameraSelector, preview)
                        }
                    } else {
                        // Standard recording
                        setupStandardRecording(provider, cameraSelector, preview)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getVideoQuality(): androidx.camera.video.Quality {
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val qualityString = sharedPreferences.getString("video_quality", "fhd")
        return when (qualityString) {
            "uhd" -> androidx.camera.video.Quality.UHD
            "hd" -> androidx.camera.video.Quality.HD
            else -> androidx.camera.video.Quality.FHD
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
        val camera = cameraProvider.bindToLifecycle(
            this, cameraSelector, preview, videoCapture
        )
        // Set up pinch-to-zoom
        setupPinchToZoom(camera)
    }

    @androidx.camera.video.ExperimentalHighSpeedVideo
    private fun setupHighSpeedRecording(
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        preview: Preview
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
            
            // We can't easily use HighSpeedVideoSessionConfig due to API complexity/changes.
            // Fallback: Use Camera2Interop to force the FPS range on the Preview.
            // This often forces the sensor into high-speed mode for the whole session.
            
            Log.d(TAG, "Attempting to force 120fps via Camera2Interop")
            
            // Note: This requires the Preview to be built with Camera2Interop
            // We need to modify the Preview passed in, but it's already built.
            // We should ideally pass the builder or rebuild it.
            // Since we can't easily rebuild it here without changing the call site,
            // we'll assume the caller (startCamera) can be modified or we create a new Preview.
            
            // However, startCamera passes a built Preview.
            // Let's create a NEW Preview with the intercept.
            
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
            val camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, newPreview, videoCapture
            )
            
            setupPinchToZoom(camera)
            
            Toast.makeText(this, "120fps Mode Active (Forced)", Toast.LENGTH_SHORT).show()
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup high-speed recording", e)
            return false
        }
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
        // Create output file
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "IMG")
        
        val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
        val fileName = if (namingPattern == "sequential") {
            StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "IMG", "jpg")
        } else {
            StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "IMG", "jpg")
        }
        
        val photoFile = File(outputDirectory, fileName)

        val outputOptions = StorageManager.createOutputFileOptions(
            this,
            photoFile,
            saveLocationPref ?: "app_storage",
            fileName
        )

        // Capture image
        val playSound = sharedPreferences.getBoolean("shutter_sound", true)
        
        if (playSound) {
            android.media.MediaActionSound().play(android.media.MediaActionSound.SHUTTER_CLICK)
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Get the saved URI (works for both file and MediaStore)
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    
                    // Apply filter to saved image if any (only for file-based storage)
                    val filter = currentFilter
                    val matrix = filter?.matrix
                    if (matrix != null && saveLocationPref != "dcim") {
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
                        val fileName = if (saveLocationPref == "dcim") {
                            "Photo saved to gallery"
                        } else {
                            "Photo captured: ${photoFile.name}"
                        }
                        Toast.makeText(baseContext, fileName, Toast.LENGTH_SHORT).show()
                        
                        // Launch PreviewActivity
                        val intent = android.content.Intent(this@MainActivity, PreviewActivity::class.java)
                        intent.putExtra("image_uri", savedUri.toString())
                        intent.putExtra("is_dcim", saveLocationPref == "dcim")
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
        handler.removeCallbacksAndMessages(null)
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

        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "IMG")
        
        val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
        val baseFileName = if (namingPattern == "sequential") {
            "${customPrefix ?: "IMG"}_BURST"
        } else {
            val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
            "${customPrefix ?: "IMG"}_${timestamp}"
        }
        
        val fileName = "${baseFileName}-${String.format("%03d", burstCount + 1)}.jpg"
        val photoFile = File(outputDirectory, fileName)

        val outputOptions = StorageManager.createOutputFileOptions(
            this,
            photoFile,
            saveLocationPref ?: "app_storage",
            fileName
        )

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    burstCount++
                    runOnUiThread {
                        burstCounter.text = getString(R.string.burst_counter, burstCount)
                    }
                    
                    if (isBurstMode && burstCount < maxBurstCount) {
                        handler.postDelayed({
                            captureBurstPhoto(burstCounter)
                        }, burstInterval)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Burst photo capture failed", exception)
                    if (isBurstMode && burstCount < maxBurstCount) {
                        handler.postDelayed({
                            captureBurstPhoto(burstCounter)
                        }, burstInterval)
                    }
                }
            }
        )
    }

    private fun showIntervalometerDialog() {
        if (isIntervalometerRunning) {
            // If running, ask to stop
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Intervalometer Running")
                .setMessage("Stop current intervalometer session?")
                .setPositiveButton("Stop") { _, _ -> stopIntervalometer() }
                .setNegativeButton("Continue", null)
                .show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_intervalometer, null)
        val radioGroupStart = dialogView.findViewById<RadioGroup>(R.id.radioGroupStart)
        val layoutDelayInput = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutDelayInput)
        val layoutAlarmInput = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutAlarmInput)
        val inputDelaySeconds = dialogView.findViewById<EditText>(R.id.inputDelaySeconds)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)
        val textAlarmTime = dialogView.findViewById<TextView>(R.id.textAlarmTime)
        val inputInterval = dialogView.findViewById<EditText>(R.id.inputInterval)
        val inputCount = dialogView.findViewById<EditText>(R.id.inputCount)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnStart = dialogView.findViewById<Button>(R.id.btnStart)

        // Alarm time state
        var alarmHour = -1
        var alarmMinute = -1

        val checkLowPower = dialogView.findViewById<CheckBox>(R.id.checkLowPower)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        radioGroupStart.setOnCheckedChangeListener { _, checkedId ->
            layoutDelayInput.visibility = if (checkedId == R.id.radioStartDelay) android.view.View.VISIBLE else android.view.View.GONE
            layoutAlarmInput.visibility = if (checkedId == R.id.radioStartAlarm) android.view.View.VISIBLE else android.view.View.GONE
        }
        
        btnSetTime.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            
            android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
                alarmHour = hourOfDay
                alarmMinute = minute
                textAlarmTime.text = String.format("%02d:%02d", hourOfDay, minute)
            }, currentHour, currentMinute, true).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnStart.setOnClickListener {
            try {
                var startDelay = 0
                
                if (radioGroupStart.checkedRadioButtonId == R.id.radioStartDelay) {
                    startDelay = inputDelaySeconds.text.toString().toIntOrNull() ?: 0
                } else if (radioGroupStart.checkedRadioButtonId == R.id.radioStartAlarm) {
                    if (alarmHour == -1) {
                        Toast.makeText(this, "Please set an alarm time", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    val now = java.util.Calendar.getInstance()
                    val target = java.util.Calendar.getInstance()
                    target.set(java.util.Calendar.HOUR_OF_DAY, alarmHour)
                    target.set(java.util.Calendar.MINUTE, alarmMinute)
                    target.set(java.util.Calendar.SECOND, 0)
                    
                    if (target.before(now)) {
                        // If time is in past, assume tomorrow
                        target.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    val diffMs = target.timeInMillis - now.timeInMillis
                    startDelay = (diffMs / 1000).toInt()
                }
                
                val interval = inputInterval.text.toString().toDoubleOrNull() ?: 5.0
                val count = inputCount.text.toString().toIntOrNull() ?: 0

                if (interval < 0.5) {
                    Toast.makeText(this, "Minimum interval is 0.5 seconds", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val lowPower = checkLowPower.isChecked
                val config = IntervalometerConfig(startDelay, interval, count, lowPower)
                startIntervalometer(config)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun startIntervalometer(config: IntervalometerConfig) {
        isIntervalometerRunning = true
        intervalometerConfig = config
        intervalometerCount = 0
        
        // Keep screen on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val startDelayMs = config.startDelaySeconds * 1000L
        
        // Initial sleep check for long start delay
        if (config.lowPowerMode && config.startDelaySeconds > 5) {
             enterSleepMode(System.currentTimeMillis() + startDelayMs)
             return
        }
        
        if (startDelayMs > 0) {
            val countdownText = findViewById<TextView>(R.id.countdownText)
            countdownText.visibility = android.view.View.VISIBLE
            countdownText.text = "Starting in ${config.startDelaySeconds}s..."
            
            // Simple countdown update
            val startTime = System.currentTimeMillis()
            val endTime = startTime + startDelayMs
            
            intervalometerRunnable = object : Runnable {
                override fun run() {
                    if (!isIntervalometerRunning) return
                    
                    val remaining = endTime - System.currentTimeMillis()
                    if (remaining <= 0) {
                        countdownText.visibility = android.view.View.GONE
                        captureIntervalPhoto()
                    } else {
                        val seconds = (remaining / 1000) + 1
                        countdownText.text = "Starting in ${seconds}s..."
                        handler.postDelayed(this, 200)
                    }
                }
            }
            handler.post(intervalometerRunnable!!)
        } else {
            captureIntervalPhoto()
        }
        
        Toast.makeText(this, "Intervalometer Started", Toast.LENGTH_SHORT).show()
    }

    private fun enterSleepMode(wakeUpTime: Long) {
        if (!isIntervalometerRunning) return
        isSleeping = true
        
        cameraProvider?.unbindAll()
        
        val sleepOverlay = findViewById<RelativeLayout>(R.id.sleepOverlay)
        val sleepCountdownText = findViewById<TextView>(R.id.sleepCountdownText)
        val btnWakeUp = findViewById<Button>(R.id.btnWakeUp)
        
        sleepOverlay.visibility = android.view.View.VISIBLE
        
        btnWakeUp.setOnClickListener {
            stopIntervalometer()
        }
        
        val wakeUpLeadTime = 3000L
        
        intervalometerRunnable = object : Runnable {
            override fun run() {
                if (!isIntervalometerRunning) return
                
                val now = System.currentTimeMillis()
                val remaining = wakeUpTime - now
                
                if (remaining <= wakeUpLeadTime) {
                    wakeUpCamera(wakeUpTime)
                } else {
                    val seconds = (remaining / 1000)
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val secs = seconds % 60
                    
                    sleepCountdownText.text = String.format("%02d:%02d:%02d", hours, minutes, secs)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(intervalometerRunnable!!)
    }
    
    private fun wakeUpCamera(captureTime: Long) {
        isSleeping = false
        val sleepOverlay = findViewById<RelativeLayout>(R.id.sleepOverlay)
        sleepOverlay.visibility = android.view.View.GONE
        
        startCamera()
        
        val now = System.currentTimeMillis()
        val remaining = captureTime - now
        
        if (remaining > 0) {
            handler.postDelayed({
                captureIntervalPhoto()
            }, remaining)
        } else {
            captureIntervalPhoto()
        }
    }

    private fun stopIntervalometer() {
        isIntervalometerRunning = false
        isSleeping = false
        intervalometerConfig = null
        handler.removeCallbacksAndMessages(null) // Be careful, this removes ALL callbacks including burst/timer
        
        // Clear screen on flag
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val countdownText = findViewById<TextView>(R.id.countdownText)
        countdownText.visibility = android.view.View.GONE
        
        val burstCounter = findViewById<TextView>(R.id.burstCounter)
        burstCounter.visibility = android.view.View.GONE
        
        findViewById<android.widget.RelativeLayout>(R.id.sleepOverlay).visibility = android.view.View.GONE
        
        // Ensure camera is bound if we stopped during sleep
        if (cameraProvider != null) {
             // We might need to re-bind if we were sleeping
             // Simple way: just call startCamera()
             startCamera()
        }
        
        Toast.makeText(this, "Intervalometer Stopped. Captured $intervalometerCount photos.", Toast.LENGTH_SHORT).show()
    }

    private fun captureIntervalPhoto() {
        if (!isIntervalometerRunning) return
        
        val config = intervalometerConfig ?: return
        
        // Check stop condition (Moved to after capture to allow the last photo to count)
        // Actually, we check before capture usually, but if we want to stop AFTER the count is reached:
        // If count is 5, we take 5th photo, increment to 5.
        // Then we check.
        
        val imageCapture = imageCapture ?: return

        // Create output file
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "IMG")
        
        val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val fileName = "${customPrefix ?: "IMG"}_INT_${timestamp}.jpg"
        val photoFile = File(outputDirectory, fileName)

        val outputOptions = StorageManager.createOutputFileOptions(
            this,
            photoFile,
            saveLocationPref ?: "app_storage",
            fileName
        )

        // Capture image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    intervalometerCount++
                    
                    val counterView = findViewById<TextView>(R.id.burstCounter)
                    counterView.visibility = android.view.View.VISIBLE
                    counterView.text = "INT: $intervalometerCount / ${if(config.totalCount > 0) config.totalCount else "∞"}"
                    
                    // Check if we reached the limit
                    if (config.totalCount > 0 && intervalometerCount >= config.totalCount) {
                        // Wait one more interval then hide/stop
                        val intervalMs = (config.intervalSeconds * 1000).toLong()
                        handler.postDelayed({
                            stopIntervalometer()
                        }, intervalMs)
                        return
                    }
                    
                    // Schedule next shot
                    if (isIntervalometerRunning) {
                        val intervalMs = (config.intervalSeconds * 1000).toLong()
                        val nextShotTime = System.currentTimeMillis() + intervalMs
                        
                        // Check for sleep mode
                        if (config.lowPowerMode && config.intervalSeconds > 5) {
                            enterSleepMode(nextShotTime)
                        } else {
                            handler.postDelayed({
                                captureIntervalPhoto()
                            }, intervalMs)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Interval photo capture failed", exception)
                    // Retry or skip?
                    if (isIntervalometerRunning) {
                        val intervalMs = (config.intervalSeconds * 1000).toLong()
                        handler.postDelayed({
                            captureIntervalPhoto()
                        }, intervalMs)
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
            
            // Stop recording timer
            recordingTimerRunnable?.let { handler.removeCallbacks(it) }
            recordingSeconds = 0
            
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            return
        }

        // Check audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission required for video recording", Toast.LENGTH_SHORT).show()
            return
        }

        // Start recording
        // Start recording
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "VID")
        
        val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
        val fileName = if (namingPattern == "sequential") {
            StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "VID", "mp4")
        } else {
            StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "VID", "mp4")
        }
        
        val videoFile = File(outputDirectory, fileName)

        val outputOptions = StorageManager.createVideoOutputOptions(
            this,
            videoFile,
            saveLocationPref ?: "app_storage",
            fileName
        )

        val pendingRecording = if (outputOptions is FileOutputOptions) {
            videoCapture.output.prepareRecording(this, outputOptions)
        } else {
            videoCapture.output.prepareRecording(this, outputOptions as MediaStoreOutputOptions)
        }

        recording = pendingRecording
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        runOnUiThread {
                            recordingIndicator.visibility = android.view.View.VISIBLE
                            
                            // Start recording timer
                            recordingSeconds = 0
                            recordingTimerRunnable = object : Runnable {
                                override fun run() {
                                    recordingSeconds++
                                    val minutes = recordingSeconds / 60
                                    val seconds = recordingSeconds % 60
                                    recordingIndicator.text = String.format("⏺ %02d:%02d", minutes, seconds)
                                    handler.postDelayed(this, 1000)
                                }
                            }
                            handler.post(recordingTimerRunnable!!)
                            
                            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        runOnUiThread {
                            recordingIndicator.visibility = android.view.View.GONE
                            
                            // Stop recording timer
                            recordingTimerRunnable?.let { handler.removeCallbacks(it) }
                            recordingSeconds = 0
                            
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
            val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
            val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
            val customPrefix = sharedPreferences.getString("custom_file_prefix", "AUDIO")
            
            val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
            val fileName = if (namingPattern == "sequential") {
                StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "AUDIO", "m4a")
            } else {
                StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "AUDIO", "m4a")
            }
            
            audioFilePath = File(outputDirectory, fileName).absolutePath
            
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
