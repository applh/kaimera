package com.example.kaimera

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.kaimera.managers.StorageManager
import java.io.File
import android.widget.TextView

class GalleryAdapter(
    private var files: MutableList<File>,
    private val onFileDeleted: () -> Unit,
    private val onFileShared: (File) -> Unit,
    private val onMediaViewerClosed: () -> Unit = {}
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val videoIndicator: ImageView = view.findViewById(R.id.videoIndicator)
        val deleteButton: android.widget.ImageButton = view.findViewById(R.id.btnDelete)
        val btnInfo: android.widget.ImageButton = view.findViewById(R.id.btnInfo)
        val fileSizeText: TextView = view.findViewById(R.id.fileSizeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val extension = file.extension.lowercase()
        val isVideo = extension == "mp4"
        val isAudio = extension == "m4a"

        when {
            isVideo -> {
                // Load video thumbnail
                holder.videoIndicator.visibility = View.VISIBLE
                holder.btnInfo.visibility = View.GONE
                loadVideoThumbnail(file, holder.imageView)
            }
            isAudio -> {
                // Show audio icon
                holder.videoIndicator.visibility = View.VISIBLE
                holder.btnInfo.visibility = View.GONE
                holder.imageView.setImageResource(android.R.drawable.ic_btn_speak_now)
            }
            else -> {
                // Load image respecting EXIF orientation
                holder.videoIndicator.visibility = View.GONE
                holder.btnInfo.visibility = View.VISIBLE
                loadImageWithOrientation(file, holder.imageView)
            }
        }

        // Display file size
        // Display file size
        holder.fileSizeText.text = StorageManager.formatStorageSize(file.length())
        
        holder.btnInfo.setOnClickListener {
            ExifUtils.showExifEditorDialog(holder.itemView.context, file)
        }
        
        // Set up item click listener
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            when {
                isVideo -> {
                    val intent = android.content.Intent(context, MediaViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    intent.putExtra("file_type", "video")
                    if (context is android.app.Activity) {
                        context.startActivityForResult(intent, 1001)
                    } else {
                        context.startActivity(intent)
                    }
                }
                isAudio -> {
                    val intent = android.content.Intent(context, MediaViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    intent.putExtra("file_type", "audio")
                    if (context is android.app.Activity) {
                        context.startActivityForResult(intent, 1001)
                    } else {
                        context.startActivity(intent)
                    }
                }
                else -> {
                    // Photo - launch MediaViewerActivity
                    val intent = android.content.Intent(context, MediaViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    intent.putExtra("file_type", "photo")
                    if (context is android.app.Activity) {
                        context.startActivityForResult(intent, 1001)
                    } else {
                        context.startActivity(intent)
                    }
                }
            }
        }
        
        // Set up long click listener for sharing
        holder.itemView.setOnLongClickListener {
            // Show options dialog (Share / Delete)
            val context = holder.itemView.context
            val options = arrayOf("Share", "Delete")
            AlertDialog.Builder(context)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onFileShared(file) // Share
                        1 -> holder.deleteButton.performClick() // Trigger delete logic
                    }
                }
                .show()
            true
        }

        // Set up delete button
        holder.deleteButton.setOnClickListener {
            val context = holder.itemView.context
            val fileType = when {
                isVideo -> "Video"
                isAudio -> "Audio"
                else -> "Photo"
            }
            AlertDialog.Builder(context)
                .setTitle("Delete $fileType?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    if (file.delete()) {
                        val removedPosition = holder.adapterPosition
                        files.removeAt(removedPosition)
                        notifyItemRemoved(removedPosition)
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        onFileDeleted()
                    } else {
                        Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadVideoThumbnail(file: File, imageView: ImageView) {
        // Use Coil to load video - it will automatically extract a frame
        imageView.load(file) {
            crossfade(true)
            placeholder(android.R.drawable.ic_media_play)
            error(android.R.drawable.ic_media_play)
        }
    }

    // Helper to load image respecting EXIF orientation
    private fun loadImageWithOrientation(file: File, imageView: ImageView) {
        // Use Coil which automatically handles EXIF orientation
        imageView.load(file) {
            crossfade(true)
        }
    }

    override fun getItemCount() = files.size
}
