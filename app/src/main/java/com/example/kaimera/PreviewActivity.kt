package com.example.kaimera

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.button.MaterialButton
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var remainingSeconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        android.util.Log.d("PreviewActivity", "onCreate called")

        val previewImageView = findViewById<ImageView>(R.id.previewImageView)
        val retakeButton = findViewById<MaterialButton>(R.id.retakeButton)
        val keepButton = findViewById<MaterialButton>(R.id.keepButton)
        val countdownText = findViewById<TextView>(R.id.previewCountdownText)

        val imageUriString = intent.getStringExtra("image_uri")
        val isDcim = intent.getBooleanExtra("is_dcim", false)
        android.util.Log.d("PreviewActivity", "Received URI string: $imageUriString, isDcim: $isDcim")

        if (imageUriString == null) {
            android.util.Log.e("PreviewActivity", "URI is null, finishing")
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        
        // Load image with EXIF orientation support
        try {
            if (isDcim || imageUri.scheme == "content") {
                // For MediaStore content URIs, use ContentResolver
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    // Try to read EXIF from content URI
                    try {
                        val exifInputStream = contentResolver.openInputStream(imageUri)
                        if (exifInputStream != null) {
                            val exif = androidx.exifinterface.media.ExifInterface(exifInputStream)
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
                            previewImageView.setImageBitmap(rotatedBitmap)
                            exifInputStream.close()
                        } else {
                            previewImageView.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("PreviewActivity", "Could not read EXIF from content URI", e)
                        previewImageView.setImageBitmap(bitmap)
                    }
                } else {
                    android.util.Log.e("PreviewActivity", "Failed to decode bitmap from content URI")
                    finish()
                    return
                }
            } else {
                // For file URIs
                val file = File(imageUri.path!!)
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        // Apply EXIF orientation
                        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
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
                        previewImageView.setImageBitmap(rotatedBitmap)
                    } else {
                        android.util.Log.e("PreviewActivity", "Failed to decode bitmap")
                        finish()
                        return
                    }
                } else {
                    android.util.Log.e("PreviewActivity", "File does not exist: ${file.absolutePath}")
                    finish()
                    return
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PreviewActivity", "Error loading image", e)
            // Fallback to Coil
            previewImageView.load(imageUri)
        }

        // Get auto-save delay from preferences
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val autoSaveDelay = sharedPreferences.getInt("preview_auto_save_delay", 2)

        // If delay is 0, save immediately
        if (autoSaveDelay == 0) {
            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Start countdown timer
        remainingSeconds = autoSaveDelay
        countdownText.visibility = android.view.View.VISIBLE
        startCountdown(countdownText)

        retakeButton.setOnClickListener {
            // Cancel timer and delete the file
            cancelCountdown()
            try {
                val file = File(imageUri.path!!)
                if (file.exists()) {
                    file.delete()
                    Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            finish()
        }

        keepButton.setOnClickListener {
            // Cancel timer and finish
            cancelCountdown()
            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCountdown(countdownText: TextView) {
        countdownRunnable = object : Runnable {
            override fun run() {
                if (remainingSeconds > 0) {
                    countdownText.text = "Auto-saving in ${remainingSeconds}s..."
                    remainingSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    // Auto-save
                    Toast.makeText(this@PreviewActivity, "Photo saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        handler.post(countdownRunnable!!)
    }

    private fun cancelCountdown() {
        countdownRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCountdown()
    }
}
