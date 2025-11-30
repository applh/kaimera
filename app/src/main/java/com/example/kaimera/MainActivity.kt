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
import android.os.Build
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
import com.example.kaimera.managers.IntervalometerManager
import com.example.kaimera.managers.BurstModeManager
import com.example.kaimera.managers.PreferencesManager
import com.example.kaimera.managers.ChronometerManager
import com.example.kaimera.managers.VideoRecordingManager
import com.example.kaimera.managers.StorageManager
import com.example.kaimera.managers.CameraManager
import com.example.kaimera.managers.PermissionManager
import com.example.kaimera.managers.OrientationManager

class MainActivity : AppCompatActivity(), IntervalometerManager.Callback, BurstModeManager.Callback, ChronometerManager.Callback, VideoRecordingManager.Callback {


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
    private lateinit var cameraExecutor: ExecutorService
    private var timerDelay: TimerDelay = TimerDelay.OFF
    private var captureMode: CaptureMode = CaptureMode.PHOTO
    // Data class representing a filter
    data class CameraFilter(val name: String, val matrix: ColorMatrix?)

    // Burst mode manager
    private lateinit var burstModeManager: BurstModeManager
    private var currentFilter: CameraFilter? = null
    private val handler = Handler(Looper.getMainLooper())

    // Chronometer manager
    private lateinit var chronometerManager: ChronometerManager

    
    // Video recording manager
    private lateinit var videoRecordingManager: VideoRecordingManager
    
    // Camera manager
    private lateinit var cameraManager: CameraManager
    
    // Permission manager
    private lateinit var permissionManager: PermissionManager
    
    // Orientation manager
    private lateinit var orientationManager: OrientationManager
    
    // Level indicator
    private lateinit var levelIndicator: LevelIndicatorView
    private lateinit var hdrButton: FloatingActionButton
    
    // Intervalometer manager
    private lateinit var intervalometerManager: IntervalometerManager
    
    // Preferences manager
    private lateinit var preferencesManager: PreferencesManager
    



    override fun onResume() {
        super.onResume()
        applyPreferences()
        checkAutoDelete()
        
        orientationManager.start()
        
        if (permissionManager.allPermissionsGranted()) {
            cameraManager.startCamera()
        }
    }
    
    override fun onPause() {
        super.onPause()
        orientationManager.stop()
    }

    companion object {
        private const val TAG = "Kaimera"
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
        
        // Apply Level Indicator settings
        val showLevelIndicator = preferencesManager.isLevelIndicatorEnabled()
        if (showLevelIndicator) {
            val sensitivity = preferencesManager.getLevelIndicatorSensitivity()
            levelIndicator.setThreshold(sensitivity.toFloat())
            
            val crosshairSize = preferencesManager.getLevelIndicatorCrosshairSize()
            levelIndicator.setCrosshairSizePercentage(crosshairSize)
            
            val circleSize = preferencesManager.getLevelIndicatorCircleSize()
            levelIndicator.setCircleSizePercentage(circleSize)
        }
        orientationManager.refreshLevelIndicatorVisibility()
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
        
        

        val galleryButton = findViewById<FloatingActionButton>(R.id.galleryButton)
        val switchButton = findViewById<FloatingActionButton>(R.id.switchButton)
        val timerButton = findViewById<FloatingActionButton>(R.id.timerButton)
        val countdownText = findViewById<TextView>(R.id.countdownText)
        val modeButton = findViewById<FloatingActionButton>(R.id.modeButton)
        val recordingIndicator = findViewById<TextView>(R.id.recordingIndicator)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize preferences manager
        preferencesManager = PreferencesManager(this)

        // Initialize managers
        burstModeManager = BurstModeManager(
            context = this,
            getImageCapture = { cameraManager.getImageCapture() },
            preferencesManager = preferencesManager,
            handler = android.os.Handler(android.os.Looper.getMainLooper())
        )

        intervalometerManager = IntervalometerManager(
            context = this,
            getCameraProvider = { cameraManager.getCameraProvider() },
            getImageCapture = { cameraManager.getImageCapture() },
            restartCamera = { cameraManager.startCamera() },
            preferencesManager = preferencesManager,
            handler = handler
        )
        
        // Initialize preferences manager
        preferencesManager = PreferencesManager(this)
        
        // Initialize chronometer manager
        chronometerManager = ChronometerManager(
            context = this,
            handler = handler
        )
        
        // Initialize video recording manager
        videoRecordingManager = VideoRecordingManager(
            context = this,
            getVideoCapture = { cameraManager.getVideoCapture() },
            handler = handler
        )
        
        // Initialize camera manager
        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            previewView = previewView,
            preferencesManager = preferencesManager
        )

        // Initialize permission manager
        permissionManager = PermissionManager(this) {
            cameraManager.startCamera()
        }
        
        // Initialize orientation manager
        orientationManager = OrientationManager(
            context = this,
            levelIndicator = levelIndicator,
            preferencesManager = preferencesManager
        ) { rotation ->
            cameraManager.getImageCapture()?.targetRotation = rotation
            cameraManager.getVideoCapture()?.targetRotation = rotation
        }
        
        // Request permissions
        permissionManager.checkAndRequestPermissions()

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
            if (captureMode == CaptureMode.PHOTO && !burstModeManager.isRunning()) {
                burstModeManager.start(this)
                true
            } else {
                false
            }
        }

        // Stop burst on release
        captureButton.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP && burstModeManager.isRunning()) {
                burstModeManager.stop()
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
            cameraManager.toggleCamera()
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
            cameraManager.setCaptureMode(captureMode)
            
            val (stringRes, icon, toastMsg) = when (captureMode) {
                CaptureMode.PHOTO -> Triple(R.string.mode_photo, android.R.drawable.ic_menu_camera, "Photo Mode")
                CaptureMode.VIDEO -> Triple(R.string.mode_video, android.R.drawable.ic_menu_gallery, "Video Mode")
            }
            
            modeButton.contentDescription = getString(stringRes)
            modeButton.setImageResource(icon)
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            
            // Restart camera to switch use cases
            cameraManager.startCamera()
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
            cameraManager.startCamera()
        }

        // Set up chronometer controls
        val chronometerDisplay = findViewById<TextView>(R.id.chronometerDisplay)
        val chronoStartButton = findViewById<MaterialButton>(R.id.chronoStartButton)
        val chronoStartWithAudioButton = findViewById<MaterialButton>(R.id.chronoStartWithAudioButton)
        val chronoStopButton = findViewById<MaterialButton>(R.id.chronoStopButton)
        val chronoResetButton = findViewById<MaterialButton>(R.id.chronoResetButton)

        chronoStartButton.setOnClickListener {
            chronoStartButton.visibility = android.view.View.GONE
            chronoStartWithAudioButton.visibility = android.view.View.GONE
            chronoStopButton.visibility = android.view.View.VISIBLE
            chronometerManager.start(false, this)
        }

        chronoStartWithAudioButton.setOnClickListener {
            chronoStartButton.visibility = android.view.View.GONE
            chronoStartWithAudioButton.visibility = android.view.View.GONE
            chronoStopButton.visibility = android.view.View.VISIBLE
            chronometerManager.start(true, this)
        }

        chronoStopButton.setOnClickListener {
            chronoStartButton.visibility = android.view.View.VISIBLE
            chronoStartWithAudioButton.visibility = android.view.View.VISIBLE
            chronoStopButton.visibility = android.view.View.GONE
            chronometerManager.stop()
        }

        chronoResetButton.setOnClickListener {
            chronoStartButton.visibility = android.view.View.VISIBLE
            chronoStartWithAudioButton.visibility = android.view.View.VISIBLE
            chronoStopButton.visibility = android.view.View.GONE
            chronometerManager.reset()
            chronometerDisplay.text = "00:00"
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
        val imageCapture = cameraManager.getImageCapture() ?: return

        // Get image format preference
        val imageFormat = preferencesManager.getImageFormat()
        val fileExtension = com.example.kaimera.utils.ImageCaptureHelper.getFileExtension(imageFormat)

        // Create output file
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
        val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
        val customPrefix = sharedPreferences.getString("custom_file_prefix", "IMG")
        
        val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
        val fileName = if (namingPattern == "sequential") {
            StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "IMG", fileExtension)
        } else {
            StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "IMG", fileExtension)
        }
        
        val photoFile = File(outputDirectory, fileName)

        // Play shutter sound
        val playSound = sharedPreferences.getBoolean("shutter_sound", true)
        if (playSound) {
            android.media.MediaActionSound().play(android.media.MediaActionSound.SHUTTER_CLICK)
        }

        // Capture image using ImageCaptureHelper
        com.example.kaimera.utils.ImageCaptureHelper.captureImage(
            imageCapture = imageCapture,
            outputFile = photoFile,
            format = imageFormat,
            quality = cameraManager.photoQuality.jpegQuality,
            executor = ContextCompat.getMainExecutor(this),
            onSuccess = { savedFile ->
                // Get the saved URI
                val savedUri = Uri.fromFile(savedFile)
                
                // Apply filter to saved image if any (only for file-based storage)
                val filter = currentFilter
                val matrix = filter?.matrix
                if (matrix != null && saveLocationPref != "dcim") {
                    try {
                        val originalBitmap = BitmapFactory.decodeFile(savedFile.absolutePath)
                        val filteredBitmap = Bitmap.createBitmap(
                            originalBitmap.width,
                            originalBitmap.height,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(filteredBitmap)
                        val paint = android.graphics.Paint()
                        paint.colorFilter = ColorMatrixColorFilter(matrix)
                        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
                        
                        // Overwrite file with filtered version
                        val out = java.io.FileOutputStream(savedFile)
                        val compressFormat = if (imageFormat == "webp") {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Bitmap.CompressFormat.WEBP_LOSSY
                            } else {
                                @Suppress("DEPRECATION")
                                Bitmap.CompressFormat.WEBP
                            }
                        } else {
                            Bitmap.CompressFormat.JPEG
                        }
                        filteredBitmap.compress(compressFormat, cameraManager.photoQuality.jpegQuality, out)
                        out.flush()
                        out.close()
                        originalBitmap.recycle()
                        filteredBitmap.recycle()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply filter", e)
                    }
                }
                
                runOnUiThread {
                    val message = if (saveLocationPref == "dcim") {
                        "Photo saved to gallery"
                    } else {
                        "Photo captured: ${savedFile.name}"
                    }
                    Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
                    
                    // Launch PreviewActivity
                    val intent = android.content.Intent(this@MainActivity, PreviewActivity::class.java)
                    intent.putExtra("image_uri", savedUri.toString())
                    intent.putExtra("is_dcim", saveLocationPref == "dcim")
                    startActivity(intent)
                }
            },
            onError = { exception ->
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Photo capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showIntervalometerDialog() {
        if (intervalometerManager.isRunning()) {
            // If running, ask to stop
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Intervalometer Running")
                .setMessage("Stop current intervalometer session?")
                .setPositiveButton("Stop") { _, _ -> intervalometerManager.stop() }
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
                val config = IntervalometerManager.Config(startDelay, interval, count, lowPower)
                
                // Keep screen on during intervalometer
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                
                intervalometerManager.start(config, this)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
    
    private fun toggleVideoRecording(recordingIndicator: TextView) {
        if (videoRecordingManager.isRecording()) {
            // Stop recording
            videoRecordingManager.stopRecording()
            recordingIndicator.visibility = android.view.View.GONE
            return
        }

        // Check audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission required for video recording", Toast.LENGTH_SHORT).show()
            return
        }

        // Start recording
        recordingIndicator.visibility = android.view.View.VISIBLE
        videoRecordingManager.startRecording(this)
    }



    override fun onDestroy() {
        super.onDestroy()
        // Clean up chronometer
        chronometerManager.cleanup()
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
    
    // IntervalometerManager.Callback implementations
    override fun onCounterUpdate(current: Int, total: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.burstCounter).apply {
                visibility = android.view.View.VISIBLE
                text = "INT: $current / ${if(total > 0) total else "∞"}"
            }
        }
    }
    
    override fun onCountdownUpdate(secondsRemaining: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.countdownText).apply {
                visibility = android.view.View.VISIBLE
                text = "Starting in ${secondsRemaining}s..."
            }
        }
    }
    
    override fun onSleepOverlayVisibility(visible: Boolean) {
        runOnUiThread {
            findViewById<RelativeLayout>(R.id.sleepOverlay).visibility = 
                if (visible) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    override fun onSleepCountdownUpdate(hours: Int, minutes: Int, seconds: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.sleepCountdownText).text = 
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
    
    override fun onComplete(photoCount: Int) {
        runOnUiThread {
            // Hide all overlays
            findViewById<TextView>(R.id.countdownText).visibility = android.view.View.GONE
            findViewById<TextView>(R.id.burstCounter).visibility = android.view.View.GONE
            findViewById<RelativeLayout>(R.id.sleepOverlay).visibility = android.view.View.GONE
            
            Toast.makeText(this, 
                "Intervalometer Stopped. Captured $photoCount photos.", 
                Toast.LENGTH_LONG).show()
        }
    }
    
    // BurstModeManager.Callback implementations
    override fun onBurstCounterUpdate(current: Int, max: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.burstCounter).apply {
                visibility = android.view.View.VISIBLE
                text = getString(R.string.burst_counter, current)
            }
        }
    }
    
    override fun onBurstComplete(totalCount: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.burstCounter).visibility = android.view.View.GONE
            Toast.makeText(this, "Burst complete: $totalCount photos", Toast.LENGTH_SHORT).show()
        }
    }
    
    // ChronometerManager.Callback implementations
    override fun onTimeUpdate(minutes: Int, seconds: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.chronometerDisplay).text = 
                String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    override fun onStateChanged(running: Boolean) {
        // State changes are handled by button visibility in click listeners
    }
    
    override fun onAudioRecordingStarted() {
        // Already handled by Toast in manager
    }
    
    override fun onAudioRecordingStopped(fileName: String) {
        // Already handled by Toast in manager
    }
    
    // VideoRecordingManager.Callback implementations
    override fun onRecordingStarted() {
        // Visibility already handled in toggleVideoRecording
    }
    
    override fun onRecordingStopped(fileName: String, success: Boolean) {
        runOnUiThread {
            findViewById<TextView>(R.id.recordingIndicator).visibility = android.view.View.GONE
        }
    }
    
    override fun onTimerUpdate(minutes: Int, seconds: Int) {
        runOnUiThread {
            findViewById<TextView>(R.id.recordingIndicator).text = 
                String.format("⏺ %02d:%02d", minutes, seconds)
        }
    }
    
    override fun onError(message: String) {
        // Already handled by Toast in manager
    }
}
