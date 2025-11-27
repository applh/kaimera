package com.example.kaimera

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File

object ExifUtils {

    fun showExifEditorDialog(context: Context, file: File) {
        // Only allow editing for JPEG images
        if (!file.extension.equals("jpg", ignoreCase = true) && !file.extension.equals("jpeg", ignoreCase = true)) {
            Toast.makeText(context, "EXIF editing is only supported for JPEG images", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val currentDesc = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION) ?: ""
            val currentComment = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT) ?: ""

            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_exif_editor, null)
            val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
            val etUserComment = dialogView.findViewById<EditText>(R.id.etUserComment)
            val tvFileInfo = dialogView.findViewById<TextView>(R.id.tvFileInfo)

            etDescription.setText(currentDesc)
            etUserComment.setText(currentComment)
            tvFileInfo.text = "File: ${file.name}\nSize: ${StorageManager.formatStorageSize(file.length())}"

            AlertDialog.Builder(context)
                .setTitle("Edit Image Info")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val newDesc = etDescription.text.toString()
                    val newComment = etUserComment.text.toString()

                    try {
                        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, newDesc)
                        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, newComment)
                        exif.saveAttributes()
                        Toast.makeText(context, "Info saved successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to save info: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read EXIF data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
