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
        setContentView(R.layout.activity_gallery)

        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)

        loadGallery()
        
        // Add back button support
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.gallery)
    }

    private fun loadGallery() {
        // Load files from external files directory (photos, videos, and audio)
        val directory = getExternalFilesDir(null)
        val files = directory?.listFiles { file ->
            val ext = file.extension.lowercase()
            ext == "jpg" || ext == "mp4" || ext == "m4a"
        }?.sortedByDescending { it.lastModified() }?.toMutableList() ?: mutableListOf()

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
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
