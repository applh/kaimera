package com.example.kamerai

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.button.MaterialButton
import java.io.File

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        android.util.Log.d("PreviewActivity", "onCreate called")

        val previewImageView = findViewById<ImageView>(R.id.previewImageView)
        val retakeButton = findViewById<MaterialButton>(R.id.retakeButton)
        val keepButton = findViewById<MaterialButton>(R.id.keepButton)

        val imageUriString = intent.getStringExtra("image_uri")
        android.util.Log.d("PreviewActivity", "Received URI string: $imageUriString")

        if (imageUriString == null) {
            android.util.Log.e("PreviewActivity", "URI is null, finishing")
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        previewImageView.load(imageUri)

        retakeButton.setOnClickListener {
            // Delete the file and finish
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
            // Just finish, file is already saved
            Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
