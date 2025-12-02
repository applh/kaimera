package com.example.kaimera.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val color: Int, // Color resource ID or ARGB value
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val labels: String = "" // Comma-separated labels for simplicity
)
