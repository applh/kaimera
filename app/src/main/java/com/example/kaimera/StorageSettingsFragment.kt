package com.example.kaimera

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class StorageSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_storage, rootKey)
    }
}
