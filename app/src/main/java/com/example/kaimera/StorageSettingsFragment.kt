package com.example.kaimera

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.kaimera.managers.StorageManager

class StorageSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_storage, rootKey)
        setupStoragePreferences()
    }

    private fun setupStoragePreferences() {
        val saveLocationPref = findPreference<androidx.preference.ListPreference>("save_location")
        val storageUsagePref = findPreference<androidx.preference.Preference>("storage_usage")
        val autoDeletePref = findPreference<androidx.preference.SwitchPreferenceCompat>("auto_delete_enabled")
        
        // Initial state check
        val isDcim = saveLocationPref?.value == "dcim"
        autoDeletePref?.isEnabled = !isDcim
        if (isDcim) {
            autoDeletePref?.isChecked = false
            autoDeletePref?.summary = "Not available for Public Gallery (DCIM)"
        } else {
            autoDeletePref?.summary = "Automatically delete files after a specified period"
        }
        
        // Update storage usage
        updateStorageUsage(storageUsagePref, saveLocationPref?.value)
        
        saveLocationPref?.setOnPreferenceChangeListener { _, newValue ->
            val newLocation = newValue as String
            updateStorageUsage(storageUsagePref, newLocation)
            
            val isNowDcim = newLocation == "dcim"
            autoDeletePref?.isEnabled = !isNowDcim
            if (isNowDcim) {
                autoDeletePref?.isChecked = false
                autoDeletePref?.summary = "Not available for Public Gallery (DCIM)"
            } else {
                autoDeletePref?.summary = "Automatically delete files after a specified period"
            }
            true
        }
    }
    
    private fun updateStorageUsage(preference: androidx.preference.Preference?, locationValue: String?) {
        val context = context ?: return
        val location = StorageManager.getStorageLocation(context, locationValue ?: "app_storage")
        
        val totalSize = StorageManager.calculateStorageUsage(location)
        val formattedSize = StorageManager.formatStorageSize(totalSize)
        val fileCount = StorageManager.getFileCount(location)
        
        preference?.summary = "Used: $formattedSize ($fileCount files)\nLocation: ${location.absolutePath}"
    }
}
