package com.example.kaimera

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d(TAG, "onCreate: Starting GalleryActivity")
        
        try {
            setContentView(R.layout.activity_gallery)
            android.util.Log.d(TAG, "onCreate: Layout inflated successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "onCreate: Failed to inflate layout", e)
            android.widget.Toast.makeText(this, "Failed to load gallery: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            recyclerView = findViewById(R.id.recyclerView)
            emptyView = findViewById(R.id.emptyView)
            android.util.Log.d(TAG, "onCreate: Views initialized")

            // Set up system gallery button
            val openSystemGalleryButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.openSystemGalleryButton)
            openSystemGalleryButton.setOnClickListener {
                openSystemGallery()
            }
            android.util.Log.d(TAG, "onCreate: Button listeners set")

            loadGallery()
            android.util.Log.d(TAG, "onCreate: Gallery loaded successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "onCreate: Error during initialization", e)
            android.widget.Toast.makeText(this, "Error initializing gallery: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    companion object {
        private const val TAG = "GalleryActivity"
    }

    private fun openSystemGallery() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.type = "image/*"
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(android.content.Intent.createChooser(intent, "Open Gallery"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "No gallery app found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGallery() {
        try {
            // Load files from the active storage location
            val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val saveLocationPref = sharedPreferences.getString("save_location", "app_storage")
            val directory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
            
            val files = directory.listFiles { file ->
                val ext = file.extension.lowercase()
                ext == "jpg" || ext == "mp4" || ext == "m4a"
            }?.sortedByDescending { it.lastModified() }?.toMutableList() ?: mutableListOf()

            // Update info panel
            val storageLocationText = findViewById<TextView>(R.id.storageLocationText)
            val storageStatsText = findViewById<TextView>(R.id.storageStatsText)
            val storagePathText = findViewById<TextView>(R.id.storagePathText)
            
            val locationName = when (saveLocationPref) {
                "sd_card" -> "SD Card"
                "dcim" -> "DCIM (Public Gallery)"
                else -> "App Storage"
            }
            storageLocationText.text = "Storage: $locationName"
            
            // Show/hide DCIM warning card
            val dcimWarningCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.dcimWarningCard)
            if (saveLocationPref == "dcim") {
                dcimWarningCard.visibility = View.VISIBLE
            } else {
                dcimWarningCard.visibility = View.GONE
            }
            
            val totalSize = StorageManager.calculateStorageUsage(directory)
            val formattedSize = StorageManager.formatStorageSize(totalSize)
            storageStatsText.text = "${files.size} files • $formattedSize"
            
            storagePathText.text = directory.absolutePath

            if (files.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE

                // Set up RecyclerView
                recyclerView.layoutManager = GridLayoutManager(this, 3)
                recyclerView.adapter = GalleryAdapter(files) {
                    // Refresh gallery when file is deleted
                    loadGallery()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GalleryActivity", "Error loading gallery", e)
            android.widget.Toast.makeText(this, "Error loading gallery: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
