package com.example.kaimera

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.io.File

class GalleryAdapter(
    private var files: MutableList<File>,
    private val onFileDeleted: () -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
        val videoIndicator: ImageView = view.findViewById(R.id.videoIndicator)
        val fileSizeText: android.widget.TextView = view.findViewById(R.id.fileSizeText)
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
                loadVideoThumbnail(file, holder.imageView)
            }
            isAudio -> {
                // Show audio icon
                holder.videoIndicator.visibility = View.VISIBLE
                holder.imageView.setImageResource(android.R.drawable.ic_btn_speak_now)
            }
            else -> {
                // Load image respecting EXIF orientation
                holder.videoIndicator.visibility = View.GONE
                loadImageWithOrientation(file, holder.imageView)
            }
        }

        // Display file size
        val fileSize = file.length()
        holder.fileSizeText.text = StorageManager.formatStorageSize(fileSize)

        // Set up item click listener
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            when {
                isVideo -> {
                    val intent = android.content.Intent(context, MediaViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    intent.putExtra("file_type", "video")
                    context.startActivity(intent)
                }
                isAudio -> {
                    val intent = android.content.Intent(context, MediaViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    intent.putExtra("file_type", "audio")
                    context.startActivity(intent)
                }
                else -> {
                    // Photo - launch MediaViewerActivity
                    val intent = android.content.Intent(context, MediaViewerActivity::class.java)
                    intent.putExtra("file_path", file.absolutePath)
                    intent.putExtra("file_type", "photo")
                    context.startActivity(intent)
                }
            }
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
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(android.R.drawable.ic_media_play)
            }
        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    // Helper to load image respecting EXIF orientation
    private fun loadImageWithOrientation(file: File, imageView: ImageView) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> {}
            }
            val rotatedBitmap = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            imageView.setImageBitmap(rotatedBitmap)
        } catch (e: Exception) {
            // Fallback to Coil loading
            imageView.load(file) {
                crossfade(true)
            }
        }
    }

    override fun getItemCount() = files.size
}
