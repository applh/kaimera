package com.example.kaimera.camera.ui

import com.example.kaimera.R
import com.example.kaimera.camera.managers.StorageManager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

import com.example.kaimera.camera.fragments.SettingsFragment
import com.example.kaimera.camera.fragments.StorageSettingsFragment
import com.example.kaimera.camera.fragments.PhotoSettingsFragment
import com.example.kaimera.camera.fragments.VideoSettingsFragment
import com.example.kaimera.camera.fragments.OverlaysSettingsFragment
import java.io.File

class SettingsActivity : AppCompatActivity(), androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private var chronometerRunning = false
    private var chronometerSeconds = 0
    private var chronometerRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var audioRecorder: android.media.MediaRecorder? = null
    private var isRecordingAudio = false
    private var audioFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        

        
        findViewById<android.view.View>(R.id.btn_home)?.setOnClickListener {
            finish()
        }
        
        setupChronometer()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh chronometer visibility in case it was changed in sub-screen
        setupChronometer()
    }

    override fun onPreferenceStartFragment(caller: androidx.preference.PreferenceFragmentCompat, pref: androidx.preference.Preference): Boolean {
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment!!
        )
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        
        return true
    }

    private fun setupChronometer() {
        val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val chronometerEnabled = sharedPreferences.getBoolean("enable_chronometer", false)
        
        val chronometerPanel = findViewById<View>(R.id.chronometerPanel)
        val chronometerDisplay = findViewById<TextView>(R.id.chronometerDisplay)
        val chronoStartButton = findViewById<MaterialButton>(R.id.chronoStartButton)
        val chronoStartWithAudioButton = findViewById<MaterialButton>(R.id.chronoStartWithAudioButton)
        val chronoStopButton = findViewById<MaterialButton>(R.id.chronoStopButton)
        val chronoResetButton = findViewById<MaterialButton>(R.id.chronoResetButton)
        
        chronometerPanel.visibility = if (chronometerEnabled) View.VISIBLE else View.GONE
        
        chronoStartButton.setOnClickListener {
            startChronometer(chronometerDisplay, chronoStartButton, chronoStartWithAudioButton, chronoStopButton, false)
        }
        
        chronoStartWithAudioButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
                return@setOnClickListener
            }
            startChronometer(chronometerDisplay, chronoStartButton, chronoStartWithAudioButton, chronoStopButton, true)
        }
        
        chronoStopButton.setOnClickListener {
            stopChronometer(chronoStartButton, chronoStartWithAudioButton, chronoStopButton)
        }
        
        chronoResetButton.setOnClickListener {
            resetChronometer(chronometerDisplay)
        }
    }
    
    private fun startChronometer(display: TextView, startBtn: MaterialButton, audioBtn: MaterialButton, stopBtn: MaterialButton, withAudio: Boolean) {
        if (chronometerRunning) return
        
        chronometerRunning = true
        startBtn.visibility = View.GONE
        audioBtn.visibility = View.GONE
        stopBtn.visibility = View.VISIBLE
        
        if (withAudio) {
            startAudioRecording()
        }
        
        chronometerRunnable = object : Runnable {
            override fun run() {
                chronometerSeconds++
                val minutes = chronometerSeconds / 60
                val seconds = chronometerSeconds % 60
                display.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(chronometerRunnable!!)
    }
    
    private fun stopChronometer(startBtn: MaterialButton, audioBtn: MaterialButton, stopBtn: MaterialButton) {
        if (!chronometerRunning) return
        
        chronometerRunning = false
        chronometerRunnable?.let { handler.removeCallbacks(it) }
        
        startBtn.visibility = View.VISIBLE
        audioBtn.visibility = View.VISIBLE
        stopBtn.visibility = View.GONE
        
        if (isRecordingAudio) {
            stopAudioRecording()
        }
    }
    
    private fun resetChronometer(display: TextView) {
        chronometerSeconds = 0
        display.text = "00:00"
    }
    
    private fun startAudioRecording() {
        try {
            val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
            val saveLocationPref = sharedPreferences.getString("camera_save_location", "app_storage")
            val outputDirectory = StorageManager.getStorageLocation(this, saveLocationPref ?: "app_storage")
            val fileName = StorageManager.generateFileName("timestamp", "AUDIO", "m4a")
            val audioFile = File(outputDirectory, fileName)
            audioFilePath = audioFile.absolutePath
            
            audioRecorder = android.media.MediaRecorder().apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            isRecordingAudio = true
            Toast.makeText(this, "Recording audio...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start audio recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopAudioRecording() {
        try {
            audioRecorder?.apply {
                stop()
                release()
            }
            audioRecorder = null
            isRecordingAudio = false
            Toast.makeText(this, "Audio saved: ${File(audioFilePath ?: "").name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop audio recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        chronometerRunnable?.let { handler.removeCallbacks(it) }
        if (isRecordingAudio) {
            stopAudioRecording()
        }
    }
}
