package com.example.kaimera.camera.fragments
import com.example.kaimera.debug.ui.DebugLogActivity

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import com.example.kaimera.R
import com.example.kaimera.BuildConfig

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

        val returnToLauncherPref = findPreference<androidx.preference.Preference>("return_to_launcher")
        returnToLauncherPref?.setOnPreferenceClickListener {
            val intent = android.content.Intent(requireContext(), com.example.kaimera.LauncherActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            true
        }
    }
}
