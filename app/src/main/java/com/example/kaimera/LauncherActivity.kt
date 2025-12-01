package com.example.kaimera

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.kaimera.managers.PreferencesManager

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesManager = PreferencesManager(this)

        if (!preferencesManager.isLaunchScreenEnabled()) {
            // Launch screen disabled, go directly to MainActivity
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_launcher)

        // Set up icon click listeners
        findViewById<LinearLayout>(R.id.cameraIcon).setOnClickListener {
            startMainActivity()
        }

        findViewById<LinearLayout>(R.id.browserIcon).setOnClickListener {
            val intent = Intent(this, BrowserActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.fileExplorerIcon).setOnClickListener {
            val intent = Intent(this, FileExplorerActivity::class.java)
            startActivity(intent)
        }

        // Set up theme switcher
        setupThemeSwitcher()
    }

    private fun setupThemeSwitcher() {
        val themeRadioGroup = findViewById<RadioGroup>(R.id.themeRadioGroup)
        
        // Get current theme preference
        val prefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Set current selection
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeRadioGroup.check(R.id.rbLight)
            AppCompatDelegate.MODE_NIGHT_YES -> themeRadioGroup.check(R.id.rbDark)
            else -> themeRadioGroup.check(R.id.rbSystem)
        }
        
        // Handle theme changes
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                R.id.rbLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbDark -> AppCompatDelegate.MODE_NIGHT_YES
                R.id.rbSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            
            // Save preference
            prefs.edit().putInt("theme_mode", newTheme).apply()
            
            // Apply theme
            AppCompatDelegate.setDefaultNightMode(newTheme)
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
