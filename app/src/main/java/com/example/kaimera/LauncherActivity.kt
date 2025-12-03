package com.example.kaimera

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import com.example.kaimera.camera.ui.MainActivity
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.kaimera.managers.LauncherPreferencesManager
import com.example.kaimera.core.managers.PreferencesManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class LauncherActivity : AppCompatActivity() {

    private lateinit var appsGrid: GridLayout
    private lateinit var settingsFab: FloatingActionButton
    private lateinit var launcherPreferencesManager: LauncherPreferencesManager

    // Launcher for settings activity
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload launcher when settings are saved
            loadLauncherApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesManager = PreferencesManager(this)

        if (!preferencesManager.isLaunchScreenEnabled()) {
            // Launch screen disabled, go directly to MainActivity
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_launcher)

        // Initialize views
        appsGrid = findViewById(R.id.appsGrid)
        settingsFab = findViewById(R.id.settingsFab)
        
        // Initialize preferences manager
        launcherPreferencesManager = LauncherPreferencesManager(this)

        // Load launcher apps
        loadLauncherApps()

        // Settings FAB click listener
        settingsFab.setOnClickListener {
            showSettingsDialog()
        }

        // Quick Notes FAB click listener
        findViewById<FloatingActionButton>(R.id.quickNotesFab).setOnClickListener {
            showQuickAccessPanel()
        }
    }

    private fun loadLauncherApps() {
        // Clear existing views
        appsGrid.removeAllViews()

        // Get visible apps
        val visibleApps = launcherPreferencesManager.getVisibleApps()

        // Create icon for each visible app
        visibleApps.forEach { app ->
            val appView = createAppIcon(app)
            appsGrid.addView(appView)
        }
    }

    private fun createAppIcon(app: LauncherApp): View {
        val appView = LayoutInflater.from(this)
            .inflate(R.layout.item_launcher_icon, appsGrid, false)

        val iconView = appView.findViewById<ImageView>(R.id.appIcon)
        val nameView = appView.findViewById<TextView>(R.id.appName)

        iconView.setImageResource(app.iconRes)
        nameView.text = app.name

        // Set click listener
        appView.setOnClickListener {
            if (app.id == "settings") {
                // Special case: open settings dialog instead of activity
                showSettingsDialog()
            } else {
                val intent = Intent(this, app.activityClass)
                startActivity(intent)
            }
        }

        return appView
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_launcher_settings, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val themeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val preferencesManager = PreferencesManager(this)

        // Set current theme selection
        when (preferencesManager.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeRadioGroup.check(R.id.rbLight)
            AppCompatDelegate.MODE_NIGHT_YES -> themeRadioGroup.check(R.id.rbDark)
            else -> themeRadioGroup.check(R.id.rbSystem)
        }

        // Handle theme changes
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            preferencesManager.setThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Customize Launcher button
        dialogView.findViewById<View>(R.id.customizeLauncherButton)?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, LauncherSettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }

        // Close button
        dialogView.findViewById<View>(R.id.closeButton)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showQuickAccessPanel() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_quick_access_panel, null)
        dialog.setContentView(view)

        val notesContainer = view.findViewById<LinearLayout>(R.id.notesContainer)
        val createNoteButton = view.findViewById<View>(R.id.createNoteButton)

        // Load recent notes asynchronously
        val database = com.example.kaimera.notes.data.NoteDatabase.getDatabase(this)
        val repository = com.example.kaimera.notes.data.NoteRepository(database.noteDao())
        
        // Use a simple coroutine to fetch data (in a real app, use ViewModel/LifecycleScope)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            repository.allNotes.collect { notes ->
                notesContainer.removeAllViews()
                notes.take(3).forEach { note ->
                    val noteView = layoutInflater.inflate(R.layout.item_quick_note, notesContainer, false)
                    noteView.findViewById<TextView>(R.id.noteTitle).text = note.title
                    noteView.findViewById<TextView>(R.id.noteContent).text = note.content
                    noteView.setOnClickListener {
                        dialog.dismiss()
                        val intent = Intent(this@LauncherActivity, com.example.kaimera.notes.ui.NoteActivity::class.java)
                        // Pass note ID to open specific note (requires modification to NoteActivity to handle intent extras)
                         intent.putExtra("noteId", note.id) // Assuming NoteActivity handles this
                        startActivity(intent)
                    }
                    notesContainer.addView(noteView)
                }
            }
        }

        createNoteButton.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, com.example.kaimera.notes.ui.NoteActivity::class.java)
            startActivity(intent)
        }

        dialog.show()
    }
}
