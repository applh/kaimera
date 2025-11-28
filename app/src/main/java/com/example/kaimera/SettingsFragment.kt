package com.example.kaimera

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        // Set version summary dynamically
        val versionPref = findPreference<androidx.preference.Preference>("version")
        versionPref?.summary = BuildConfig.VERSION_NAME
        
        // Handle Debug Logs button
        val debugLogsPref = findPreference<androidx.preference.Preference>("debug_logs")
        
        // Check Android version - logcat reading restricted on Android 4.1+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // Keep button visible but update summary to explain limitation
            debugLogsPref?.summary = "Limited on Android 4.1+. Use Test button or ADB instead."
        }
        
        debugLogsPref?.setOnPreferenceClickListener {
            val intent = android.content.Intent(requireContext(), DebugLogActivity::class.java)
            startActivity(intent)
            true
        }
    }
}
