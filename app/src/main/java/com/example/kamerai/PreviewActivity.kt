package com.example.kamerai

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
        android.util.Log.d("PreviewActivity", "Received URI string: $imageUriString")

        if (imageUriString == null) {
            android.util.Log.e("PreviewActivity", "URI is null, finishing")
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        previewImageView.load(imageUri)

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
