package com.example.kamerai

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        // Set version summary dynamically
        val versionPref = findPreference<androidx.preference.Preference>("version")
        versionPref?.summary = BuildConfig.VERSION_NAME
    }
}
