package com.example.kaimera

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

class MediaViewerActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var photoView: ImageView
    private lateinit var audioIcon: ImageView
    private lateinit var audioControls: LinearLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnClose: ImageButton
    
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_viewer)
        
        videoView = findViewById(R.id.videoView)
        photoView = findViewById(R.id.photoView)
        audioIcon = findViewById(R.id.audioIcon)
        audioControls = findViewById(R.id.audioControls)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnClose = findViewById(R.id.btnClose)
        
        val filePath = intent.getStringExtra("file_path") ?: run {
            Toast.makeText(this, "Error: No file path provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
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
        
        btnClose.setOnClickListener {
            finish()
        }
    }
    
    private fun setupVideoPlayback(filePath: String) {
        val videoContainer = findViewById<View>(R.id.videoContainer)
        videoContainer.visibility = View.VISIBLE
        videoView.visibility = View.VISIBLE
        audioIcon.visibility = View.GONE
        audioControls.visibility = View.GONE
        
        val uri = Uri.parse(filePath)
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener {
            it.start()
        }
        videoView.setOnCompletionListener {
            finish()
        }
        videoView.setOnErrorListener { _, what, extra ->
            Toast.makeText(this, "Error playing video: $what, $extra", Toast.LENGTH_SHORT).show()
            finish()
            true
        }
    }
    
    private fun setupAudioPlayback(filePath: String) {
        findViewById<View>(R.id.videoContainer).visibility = View.GONE
        videoView.visibility = View.GONE
        audioIcon.visibility = View.VISIBLE
        audioControls.visibility = View.VISIBLE
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }
            
            btnPlayPause.setOnClickListener {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    mediaPlayer?.start()
                    isPlaying = true
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                }
            }
            
            mediaPlayer?.setOnCompletionListener {
                isPlaying = false
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading audio: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
    }
    
    private fun setupPhotoViewing(filePath: String) {
        findViewById<View>(R.id.videoContainer).visibility = View.GONE
        videoView.visibility = View.GONE
        audioIcon.visibility = View.GONE
        audioControls.visibility = View.GONE
        photoView.visibility = View.VISIBLE
        
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
