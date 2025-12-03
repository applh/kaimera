package com.example.kaimera.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Centralized formatting utilities for consistent display across the app.
 */
object FormatUtils {
    
    /**
     * Format duration in milliseconds to HH:MM:SS format.
     */
    fun formatDuration(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Format timestamp to readable date/time string.
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Format phone number for display (basic formatting).
     */
    fun formatPhoneNumber(number: String): String {
        // Remove all non-digit characters
        val digits = number.filter { it.isDigit() }
        
        // Basic formatting for common lengths
        return when {
            digits.length == 10 -> {
                // Format as (XXX) XXX-XXXX
                "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            digits.length == 11 && digits.startsWith("1") -> {
                // Format as +1 (XXX) XXX-XXXX
                "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
            }
            else -> number // Return original if not a standard format
        }
    }
    
    /**
     * Format file size in bytes to human-readable format with decimal precision.
     */
    fun formatFileSizeDetailed(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
