package com.example.kamerai

import android.app.AlertDialog
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val isVideo = file.extension.lowercase() == "mp4"

        if (isVideo) {
            // Load video thumbnail
            holder.videoIndicator.visibility = View.VISIBLE
            loadVideoThumbnail(file, holder.imageView)
        } else {
            // Load image
            holder.videoIndicator.visibility = View.GONE
            holder.imageView.load(file) {
                crossfade(true)
            }
        }

        // Set up delete button
        holder.deleteButton.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Delete ${if (isVideo) "Video" else "Photo"}?")
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

    override fun getItemCount() = files.size
}
