package com.example.kaimera.core.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Centralized manager for all app preferences.
 * 
 * Provides type-safe access to SharedPreferences with default values.
 */
class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getString(key: String, defValue: String): String? = prefs.getString(key, defValue)
    fun getBoolean(key: String, defValue: Boolean): Boolean = prefs.getBoolean(key, defValue)
    
    // Photo Settings
    fun getPhotoQualityInt(): Int = prefs.getInt("photo_quality_int", 95)
    
    fun getFlashMode(): String = prefs.getString("flash_mode", "auto") ?: "auto"
    
    fun getImageFormat(): String = prefs.getString("image_format", "jpeg") ?: "jpeg"
    
    fun getCaptureMode(): String = prefs.getString("capture_mode_preference", "latency") ?: "latency"
    
    fun getTargetResolution(): String = prefs.getString("target_resolution", "max") ?: "max"
    
    // Video Settings
    fun getVideoQuality(): String = prefs.getString("video_quality", "fhd") ?: "fhd"
    
    fun getVideoFrameRate(): String = prefs.getString("video_frame_rate", "30") ?: "30"
    
    fun is120fpsEnabled(): Boolean = prefs.getBoolean("enable_120fps", false)
    
    // Storage Settings
    fun getSaveLocation(): String = prefs.getString("camera_save_location", "app_storage") ?: "app_storage"
    
    fun getFileNamingPattern(): String = prefs.getString("camera_file_naming_pattern", "timestamp") ?: "timestamp"
    
    fun getCustomFilePrefix(): String = prefs.getString("camera_custom_file_prefix", "IMG") ?: "IMG"
    
    fun getAutoDeleteDays(): Int = prefs.getString("auto_delete_days", "0")?.toIntOrNull() ?: 0
    
    // Overlay Settings
    fun isGridEnabled(): Boolean = prefs.getBoolean("enable_grid", false)
    
    fun isLevelIndicatorEnabled(): Boolean = prefs.getBoolean("enable_level_indicator", true)
    
    fun getLevelIndicatorSensitivity(): Int = prefs.getInt("level_indicator_sensitivity", 5)
    
    fun getLevelIndicatorCrosshairSize(): Int = prefs.getInt("level_indicator_crosshair_size", 20)
    
    fun getLevelIndicatorCircleSize(): Int = prefs.getInt("level_indicator_circle_size", 10)
    
    // Preview Settings
    fun getAutoSaveDelay(): Int = prefs.getString("auto_save_delay", "3")?.toIntOrNull() ?: 3
    
    // Helper method to get all preferences at once for a specific category
    data class PhotoSettings(
        val quality: Int,
        val captureMode: String,
        val targetResolution: String
    )
    
    data class VideoSettings(
        val quality: String,
        val frameRate: String,
        val enable120fps: Boolean
    )
    
    data class StorageSettings(
        val saveLocation: String,
        val namingPattern: String,
        val customPrefix: String,
        val autoDeleteDays: Int
    )
    
    data class OverlaySettings(
        val gridEnabled: Boolean,
        val levelIndicatorEnabled: Boolean,
        val levelSensitivity: Int,
        val crosshairSize: Int,
        val circleSize: Int
    )
    
    fun getPhotoSettings(): PhotoSettings = PhotoSettings(
        quality = getPhotoQualityInt(),
        captureMode = getCaptureMode(),
        targetResolution = getTargetResolution()
    )
    
    fun getVideoSettings(): VideoSettings = VideoSettings(
        quality = getVideoQuality(),
        frameRate = getVideoFrameRate(),
        enable120fps = is120fpsEnabled()
    )
    
    fun getStorageSettings(): StorageSettings = StorageSettings(
        saveLocation = getSaveLocation(),
        namingPattern = getFileNamingPattern(),
        customPrefix = getCustomFilePrefix(),
        autoDeleteDays = getAutoDeleteDays()
    )
    
    fun getOverlaySettings(): OverlaySettings = OverlaySettings(
        gridEnabled = isGridEnabled(),
        levelIndicatorEnabled = isLevelIndicatorEnabled(),
        levelSensitivity = getLevelIndicatorSensitivity(),
        crosshairSize = getLevelIndicatorCrosshairSize(),
        circleSize = getLevelIndicatorCircleSize()
    )
    
    fun isLaunchScreenEnabled(): Boolean {
        return prefs.getBoolean("launch_screen_enabled", true)
    }

    fun isLocationTaggingEnabled(): Boolean {
        return prefs.getBoolean("camera_save_gps_location", false)
    }

    fun getThemeMode(): Int {
        return prefs.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
    }
}
