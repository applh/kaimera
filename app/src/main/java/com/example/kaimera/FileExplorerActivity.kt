package com.example.kaimera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileExplorerActivity : AppCompatActivity() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var tvCurrentPath: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: FileExplorerAdapter
    
    private var currentDirectory: File? = null
    private val rootDirectory by lazy { applicationContext.dataDir }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explorer)

        rvFiles = findViewById(R.id.rvFiles)
        tvCurrentPath = findViewById(R.id.tvCurrentPath)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        setupRecyclerView()
        
        // Check if a specific path was provided
        val startPath = intent.getStringExtra("start_path")
        val startDir = if (startPath != null) {
            val dir = java.io.File(startPath)
            if (dir.exists() && dir.isDirectory) dir else rootDirectory
        } else {
            rootDirectory
        }
        
        // Start in specified directory or app data directory
        navigateToDirectory(startDir)
        
        // Handle home button
        findViewById<android.widget.ImageButton>(R.id.btnHome).setOnClickListener {
            val intent = Intent(this, LauncherActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = FileExplorerAdapter(
            files = emptyList(),
            onFileClick = { file -> handleFileClick(file) },
            onFileAction = { file, view -> showFileActionMenu(file, view) }
        )
        
        rvFiles.layoutManager = LinearLayoutManager(this)
        rvFiles.adapter = adapter
    }

    private fun navigateToDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            Toast.makeText(this, "Directory not accessible", Toast.LENGTH_SHORT).show()
            return
        }

        // Security: Prevent navigation outside app directory
        if (!directory.absolutePath.startsWith(rootDirectory.absolutePath)) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
            return
        }

        currentDirectory = directory
        tvCurrentPath.text = directory.absolutePath

        val files = directory.listFiles()?.toList()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()

        if (files.isEmpty()) {
            rvFiles.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvFiles.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            adapter.updateFiles(files)
        }
    }

    private fun handleFileClick(file: File) {
        when {
            file.isDirectory -> navigateToDirectory(file)
            isImageFile(file) || isVideoFile(file) -> openMediaFile(file)
            isTextFile(file) -> openTextFile(file)
            else -> showFileInfo(file)
        }
    }

    private fun showFileActionMenu(file: File, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add(0, 1, 0, "Share")
        popup.menu.add(0, 2, 0, "Rename")
        popup.menu.add(0, 3, 0, "Delete")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> shareFile(file)
                2 -> showRenameDialog(file)
                3 -> confirmDelete(file)
            }
            true
        }
        popup.show()
    }

    private fun openMediaFile(file: File) {
        val fileType = when {
            isImageFile(file) -> "photo"
            isVideoFile(file) -> "video"
            else -> "unknown"
        }
        
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra("file_path", file.absolutePath)
            putExtra("file_type", fileType)
        }
        startActivity(intent)
    }

    private fun openTextFile(file: File) {
        try {
            val content = file.readText()
            AlertDialog.Builder(this)
                .setTitle(file.name)
                .setMessage(content)
                .setPositiveButton("Close", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileInfo(file: File) {
        val size = formatFileSize(file.length())
        val info = """
            Name: ${file.name}
            Size: $size
            Path: ${file.absolutePath}
            Modified: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(file.lastModified()))}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("File Information")
            .setMessage(info)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Share file"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${if (file.isDirectory) "Folder" else "File"}")
            .setMessage("Are you sure you want to delete \"${file.name}\"?${if (file.isDirectory) "\n\nThis will delete all contents." else ""}")
            .setPositiveButton("Delete") { _, _ ->
                deleteFile(file)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFile(file: File) {
        try {
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            
            if (deleted) {
                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                currentDirectory?.let { navigateToDirectory(it) }
            } else {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showRenameDialog(file: File) {
        val input = EditText(this)
        input.setText(file.name)
        input.selectAll()
        
        AlertDialog.Builder(this)
            .setTitle("Rename ${if (file.isDirectory) "Folder" else "File"}")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newName == file.name) {
                    return@setPositiveButton
                }
                
                val newFile = File(file.parentFile, newName)
                if (newFile.exists()) {
                    Toast.makeText(this, "A file with that name already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                try {
                    if (file.renameTo(newFile)) {
                        Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show()
                        currentDirectory?.let { navigateToDirectory(it) }
                    } else {
                        Toast.makeText(this, "Failed to rename", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif")
    }

    private fun isVideoFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("mp4", "3gp", "mkv", "avi")
    }

    private fun isTextFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("txt", "xml", "json", "log")
    }

    private fun getMimeType(file: File): String {
        return when {
            isImageFile(file) -> "image/*"
            isVideoFile(file) -> "video/*"
            isTextFile(file) -> "text/plain"
            else -> "*/*"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    override fun onBackPressed() {
        val parent = currentDirectory?.parentFile
        if (parent != null && parent.absolutePath.startsWith(rootDirectory.absolutePath)) {
            navigateToDirectory(parent)
        } else {
            super.onBackPressed()
        }
    }
}
