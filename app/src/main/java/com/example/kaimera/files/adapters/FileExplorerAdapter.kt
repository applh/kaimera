package com.example.kaimera.files.adapters

import com.example.kaimera.R
import com.example.kaimera.core.utils.FileUtils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileExplorerAdapter(
    private var files: List<File>,
    private val onFileClick: (File) -> Unit,
    private val onFileAction: (File, View) -> Unit
) : RecyclerView.Adapter<FileExplorerAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFileIcon: ImageView = view.findViewById(R.id.ivFileIcon)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileInfo: TextView = view.findViewById(R.id.tvFileInfo)
        val btnFileAction: ImageButton = view.findViewById(R.id.btnFileAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        
        holder.tvFileName.text = file.name
        
        // Set icon based on file type
        val iconRes = when {
            file.isDirectory -> android.R.drawable.ic_menu_view
            file.name.endsWith(".jpg", true) || file.name.endsWith(".jpeg", true) || 
            file.name.endsWith(".png", true) || file.name.endsWith(".webp", true) -> 
                android.R.drawable.ic_menu_gallery
            file.name.endsWith(".mp4", true) || file.name.endsWith(".3gp", true) -> 
                android.R.drawable.ic_menu_slideshow
            file.name.endsWith(".txt", true) || file.name.endsWith(".xml", true) || 
            file.name.endsWith(".json", true) -> 
                android.R.drawable.ic_menu_edit
            file.name.endsWith(".db", true) -> 
                android.R.drawable.ic_menu_save
            else -> android.R.drawable.ic_menu_info_details
        }
        holder.ivFileIcon.setImageResource(iconRes)
        
        // Set file info
        val info = if (file.isDirectory) {
            val itemCount = file.listFiles()?.size ?: 0
            "$itemCount items"
        } else {
            val size = formatFileSize(file.length())
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(file.lastModified()))
            "$size • $date"
        }
        holder.tvFileInfo.text = info
        
        // Click listeners
        holder.itemView.setOnClickListener {
            onFileClick(file)
        }
        
        holder.btnFileAction.setOnClickListener {
            onFileAction(file, it)
        }
    }

    override fun getItemCount() = files.size

    fun updateFiles(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }

    private fun formatFileSize(bytes: Long): String {
        return FileUtils.formatFileSize(bytes)
    }
}
