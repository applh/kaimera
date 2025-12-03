package com.example.kaimera.camera.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.kaimera.R

class OverlaysSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_overlays, rootKey)
    }
}
