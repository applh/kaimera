package com.example.kaimera

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kaimera.managers.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var loadingIndicator: android.widget.ProgressBar

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
            loadingIndicator = findViewById(R.id.loadingIndicator)
            android.util.Log.d(TAG, "onCreate: Views initialized")

            // Set up system gallery button
            val openSystemGalleryButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.openSystemGalleryButton)
            openSystemGalleryButton.setOnClickListener {
                openSystemGallery()
            }
            android.util.Log.d(TAG, "onCreate: Button listeners set")

            loadGallery()
            android.util.Log.d(TAG, "onCreate: Gallery loading started")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "onCreate: Error during initialization", e)
            android.widget.Toast.makeText(this, "Error initializing gallery: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // MediaViewerActivity returned with success (frame was exported)
            loadGallery()
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
        lifecycleScope.launch {
            try {
                // Show loading indicator
                loadingIndicator.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.GONE

                // Load files in background
                val galleryData = withContext(Dispatchers.IO) {
                    val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@GalleryActivity)
                    val saveLocationPref = sharedPreferences.getString("camera_save_location", "app_storage")
                    val directory = StorageManager.getStorageLocation(this@GalleryActivity, saveLocationPref ?: "app_storage")
                    
                    val files = directory.listFiles { file ->
                        val ext = file.extension.lowercase()
                        ext == "jpg" || ext == "webp" || ext == "mp4" || ext == "m4a"
                    }?.sortedByDescending { it.lastModified() }?.toMutableList() ?: mutableListOf()

                    val totalSize = StorageManager.calculateStorageUsage(directory)
                    val formattedSize = StorageManager.formatStorageSize(totalSize)
                    
                    val locationName = when (saveLocationPref) {
                        "sd_card" -> "SD Card"
                        "dcim" -> "DCIM (Public Gallery)"
                        else -> "App Storage"
                    }

                    // Return data as a bundle
                    GalleryData(
                        files = files,
                        locationName = locationName,
                        totalSize = formattedSize,
                        directoryPath = directory.absolutePath,
                        isDcim = saveLocationPref == "dcim"
                    )
                }

                // Update UI on main thread
                loadingIndicator.visibility = View.GONE
                
                val storageLocationText = findViewById<TextView>(R.id.storageLocationText)
                val storageStatsText = findViewById<TextView>(R.id.storageStatsText)
                val storagePathText = findViewById<TextView>(R.id.storagePathText)
                val dcimWarningCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.dcimWarningCard)
                
                storageLocationText.text = "Storage: ${galleryData.locationName}"
                storageStatsText.text = "${galleryData.files.size} files • ${galleryData.totalSize}"
                storagePathText.text = galleryData.directoryPath
                dcimWarningCard.visibility = if (galleryData.isDcim) View.VISIBLE else View.GONE

                if (galleryData.files.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE

                    recyclerView.layoutManager = GridLayoutManager(this@GalleryActivity, 3)
                    recyclerView.adapter = GalleryAdapter(galleryData.files, 
                        onFileDeleted = {
                            loadGallery()
                        },
                        onFileShared = { file ->
                            shareFile(file)
                        },
                        onMediaViewerClosed = {
                            // Reload gallery when returning from MediaViewerActivity
                            loadGallery()
                        }
                    )
                }
            } catch (e: Exception) {
                loadingIndicator.visibility = View.GONE
                android.util.Log.e("GalleryActivity", "Error loading gallery", e)
                android.widget.Toast.makeText(this@GalleryActivity, "Error loading gallery: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private data class GalleryData(
        val files: MutableList<File>,
        val locationName: String,
        val totalSize: String,
        val directoryPath: String,
        val isDcim: Boolean
    )

    private fun shareFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            
            val mimeType = when (file.extension.lowercase()) {
                "mp4" -> "video/mp4"
                "m4a" -> "audio/mp4"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Could not share file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            android.util.Log.e("GalleryActivity", "Share failed", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
