package com.example.kaimera

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

import java.io.File

class MediaViewerActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var photoView: ImageView
    private lateinit var audioIcon: ImageView
    private lateinit var mediaControls: View
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnExportFrame: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnInfo: ImageButton
    private lateinit var seekBar: android.widget.SeekBar
    private lateinit var tvCurrentTime: android.widget.TextView
    private lateinit var tvTotalTime: android.widget.TextView
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isTrackingTouch = false
    private var currentFilePath: String? = null
    
    private val updateProgressAction = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_viewer)
        
        videoView = findViewById(R.id.videoView)
        photoView = findViewById(R.id.photoView)
        audioIcon = findViewById(R.id.audioIcon)
        mediaControls = findViewById(R.id.mediaControls)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnExportFrame = findViewById(R.id.btnExportFrame)
        btnClose = findViewById(R.id.btnClose)
        btnInfo = findViewById(R.id.btnInfo)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        
        val filePath = intent.getStringExtra("file_path") ?: run {
            Toast.makeText(this, "Error: No file path provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentFilePath = filePath
        
        val fileType = intent.getStringExtra("file_type") ?: "unknown"
        
        when (fileType) {
            "video" -> setupVideoPlayback(filePath)
            "audio" -> setupAudioPlayback(filePath)
            "photo" -> setupPhotoViewing(filePath)
            else -> {
                Toast.makeText(this, "Unsupported file type", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        setupSeekBar()
        applyMaxZoom()
        setupMediaControls()
    }
    
    private fun setupMediaControls() {
        btnClose.setOnClickListener {
            finish()
        }
        
        btnInfo.setOnClickListener {
            showExifEditorDialog()
        }
    }
    
    private fun showExifEditorDialog() {
        val filePath = currentFilePath ?: return
        val currentFile = File(filePath)
        ExifUtils.showExifEditorDialog(this, currentFile)
    }    
    
    private fun applyMaxZoom() {
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val maxZoom = sharedPreferences.getInt("gallery_max_zoom", 10).toFloat()
        
        val videoContainer = findViewById<ZoomableVideoLayout>(R.id.videoContainer)
        val photoView = findViewById<ZoomableImageView>(R.id.photoView)
        
        videoContainer.setMaxZoom(maxZoom)
        photoView.setMaxZoom(maxZoom)
    }
    
    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                isTrackingTouch = true
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                isTrackingTouch = false
                seekBar?.let {
                    if (videoView.visibility == View.VISIBLE) {
                        videoView.seekTo(it.progress)
                    } else if (mediaPlayer != null) {
                        mediaPlayer?.seekTo(it.progress)
                    }
                }
            }
        })
    }
    
    private fun updateProgress() {
        if (isTrackingTouch) return
        
        var current = 0
        var total = 0
        
        if (videoView.visibility == View.VISIBLE) {
            current = videoView.currentPosition
            total = videoView.duration
        } else if (mediaPlayer != null) {
            current = mediaPlayer?.currentPosition ?: 0
            total = mediaPlayer?.duration ?: 0
        }
        
        if (total > 0) {
            seekBar.max = total
            seekBar.progress = current
            tvCurrentTime.text = formatTime(current)
            tvTotalTime.text = formatTime(total)
        }
    }
    
    private fun formatTime(millis: Int): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    private fun setupVideoPlayback(filePath: String) {
        val videoContainer = findViewById<View>(R.id.videoContainer)
        videoContainer.visibility = View.VISIBLE
        videoView.visibility = View.VISIBLE
        audioIcon.visibility = View.GONE
        mediaControls.visibility = View.VISIBLE
        btnInfo.visibility = View.GONE  // Hide EXIF button for videos
        
        val uri = Uri.parse(filePath)
        videoView.setVideoURI(uri)
        
        videoView.setOnPreparedListener { mp ->
            mp.start()
            isPlaying = true
            updatePlayPauseButton()
            handler.post(updateProgressAction)
            
            // Fix for video view not updating duration immediately
            tvTotalTime.text = formatTime(videoView.duration)
        }
        
        videoView.setOnCompletionListener {
            isPlaying = false
            updatePlayPauseButton()
            videoView.seekTo(0)
        }
        
        videoView.setOnErrorListener { _, what, extra ->
            Toast.makeText(this, "Error playing video: $what, $extra", Toast.LENGTH_SHORT).show()
            finish()
            true
        }
        
        btnPlayPause.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
                isPlaying = false
            } else {
                videoView.start()
                isPlaying = true
            }
            updatePlayPauseButton()
        }
        
        btnExportFrame.setOnClickListener {
            exportCurrentFrame()
        }
    }
    
    private fun setupAudioPlayback(filePath: String) {
        findViewById<View>(R.id.videoContainer).visibility = View.GONE
        videoView.visibility = View.GONE
        audioIcon.visibility = View.VISIBLE
        mediaControls.visibility = View.VISIBLE
        btnInfo.visibility = View.GONE  // Hide EXIF button for audio
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            isPlaying = true
            updatePlayPauseButton()
            handler.post(updateProgressAction)
            
            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                updatePlayPauseButton()
                mediaPlayer?.seekTo(0)
            }
            
            btnPlayPause.setOnClickListener {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    mediaPlayer?.start()
                    isPlaying = true
                }
                updatePlayPauseButton()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading audio: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun updatePlayPauseButton() {
        btnPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause 
            else android.R.drawable.ic_media_play
        )
        // Show export button only when video is paused
        if (videoView.visibility == View.VISIBLE) {
            btnExportFrame.visibility = if (isPlaying) View.GONE else View.VISIBLE
        }
    }
    
    private fun exportCurrentFrame() {
        val filePath = currentFilePath ?: return
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(filePath)
            val currentPosition = videoView.currentPosition * 1000L // Convert to microseconds
            val bitmap = retriever.getFrameAtTime(currentPosition, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            
            if (bitmap != null) {
                saveFrameAsImage(bitmap)
            } else {
                Toast.makeText(this, "Failed to extract frame", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            retriever.release()
        }
    }
    
    private fun saveFrameAsImage(bitmap: android.graphics.Bitmap) {
        try {
            val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val preferencesManager = com.example.kaimera.managers.PreferencesManager(this)
            
            val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
            val namingPattern = sharedPreferences.getString("file_naming_pattern", "timestamp")
            val customPrefix = sharedPreferences.getString("custom_file_prefix", "FRAME")
            
            // Get image format and quality from photo settings
            val imageFormat = preferencesManager.getImageFormat()
            val quality = preferencesManager.getPhotoQualityInt()
            val extension = if (imageFormat == "webp") "webp" else "jpg"
            
            val outputDirectory = com.example.kaimera.managers.StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
            val fileName = if (namingPattern == "sequential") {
                com.example.kaimera.managers.StorageManager.generateSequentialFileName(outputDirectory, customPrefix ?: "FRAME", extension)
            } else {
                com.example.kaimera.managers.StorageManager.generateFileName(namingPattern ?: "timestamp", customPrefix ?: "FRAME", extension)
            }
            
            val imageFile = java.io.File(outputDirectory, fileName)
            val out = java.io.FileOutputStream(imageFile)
            
            // Use format and quality from settings
            val compressFormat = if (imageFormat == "webp") {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    android.graphics.Bitmap.CompressFormat.WEBP
                }
            } else {
                android.graphics.Bitmap.CompressFormat.JPEG
            }
            
            bitmap.compress(compressFormat, quality, out)
            out.flush()
            out.close()
            
            Toast.makeText(this, "Frame saved: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save frame: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (videoView.visibility == View.VISIBLE && videoView.isPlaying) {
            videoView.pause()
        }
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
        isPlaying = false
        updatePlayPauseButton()
        handler.removeCallbacks(updateProgressAction)
    }
    
    override fun onResume() {
        super.onResume()
        if (videoView.visibility == View.VISIBLE || mediaPlayer != null) {
            handler.post(updateProgressAction)
        }
    }
    
    private fun setupPhotoViewing(filePath: String) {
        findViewById<View>(R.id.videoContainer).visibility = View.GONE
        videoView.visibility = View.GONE
        audioIcon.visibility = View.GONE
        mediaControls.visibility = View.GONE
        photoView.visibility = View.VISIBLE
        btnInfo.visibility = View.VISIBLE  // Show EXIF button for photos
        
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(filePath)
            if (bitmap != null) {
                // Apply EXIF orientation
                val exif = androidx.exifinterface.media.ExifInterface(filePath)
                val orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )
                val matrix = android.graphics.Matrix()
                when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                val rotatedBitmap = if (matrix.isIdentity) {
                    bitmap
                } else {
                    android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                photoView.setImageBitmap(rotatedBitmap)
            } else {
                Toast.makeText(this, "Error loading photo", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressAction)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
