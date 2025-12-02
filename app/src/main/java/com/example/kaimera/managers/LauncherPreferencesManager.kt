package com.example.kaimera.managers

import android.content.Context
import android.content.SharedPreferences
import com.example.kaimera.LauncherApp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages launcher app preferences (visibility and order)
 */
class LauncherPreferencesManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "launcher_preferences",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_LAUNCHER_APPS = "launcher_apps"
    }
    
    /**
     * Get the list of launcher apps with saved preferences
     */
    fun getLauncherApps(): List<LauncherApp> {
        val savedJson = prefs.getString(KEY_LAUNCHER_APPS, null)
        
        return if (savedJson != null) {
            // Load from saved preferences
            parseLauncherApps(savedJson)
        } else {
            // Return default apps
            LauncherApp.getDefaultApps()
        }
    }
    
    /**
     * Save the launcher apps list
     */
    fun saveLauncherApps(apps: List<LauncherApp>) {
        val json = serializeLauncherApps(apps)
        prefs.edit().putString(KEY_LAUNCHER_APPS, json).apply()
    }
    
    /**
     * Get only visible apps, sorted by order
     */
    fun getVisibleApps(): List<LauncherApp> {
        return getLauncherApps()
            .filter { it.isVisible }
            .sortedBy { it.order }
    }
    
    /**
     * Reset to default apps
     */
    fun resetToDefaults() {
        prefs.edit().remove(KEY_LAUNCHER_APPS).apply()
    }
    
    /**
     * Serialize launcher apps to JSON string
     */
    private fun serializeLauncherApps(apps: List<LauncherApp>): String {
        val jsonArray = JSONArray()
        
        apps.forEach { app ->
            val jsonObject = JSONObject().apply {
                put("id", app.id)
                put("name", app.name)
                put("isVisible", app.isVisible)
                put("order", app.order)
            }
            jsonArray.put(jsonObject)
        }
        
        return jsonArray.toString()
    }
    
    /**
     * Parse launcher apps from JSON string
     */
    private fun parseLauncherApps(json: String): List<LauncherApp> {
        val jsonArray = JSONArray(json)
        val defaultApps = LauncherApp.getDefaultApps()
        val appsMap = defaultApps.associateBy { it.id }.toMutableMap()
        
        // Update apps with saved preferences
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getString("id")
            
            appsMap[id]?.let { app ->
                app.isVisible = jsonObject.getBoolean("isVisible")
                app.order = jsonObject.getInt("order")
            }
        }
        
        return appsMap.values.toList().sortedBy { it.order }
    }
}
