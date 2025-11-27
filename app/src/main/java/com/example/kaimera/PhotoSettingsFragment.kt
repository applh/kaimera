package com.example.kaimera

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class PhotoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_photo, rootKey)
    }
}
