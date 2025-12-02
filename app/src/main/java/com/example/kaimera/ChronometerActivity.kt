package com.example.kaimera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.kaimera.managers.ChronometerManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChronometerActivity : AppCompatActivity(), ChronometerManager.Callback {

    private lateinit var chronometerManager: ChronometerManager
    private lateinit var chronometerDisplay: TextView
    private lateinit var chronoStartButton: MaterialButton
    private lateinit var chronoStartWithAudioButton: MaterialButton
    private lateinit var chronoStopButton: MaterialButton
    private lateinit var chronoResetButton: MaterialButton
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chronometer)

        // Initialize Chronometer Manager
        chronometerManager = ChronometerManager(this, handler)

        // Initialize Views
        chronometerDisplay = findViewById(R.id.chronometerDisplay)
        chronoStartButton = findViewById(R.id.chronoStartButton)
        chronoStartWithAudioButton = findViewById(R.id.chronoStartWithAudioButton)
        chronoStopButton = findViewById(R.id.chronoStopButton)
        chronoResetButton = findViewById(R.id.chronoResetButton)
        val homeButton = findViewById<FloatingActionButton>(R.id.homeButton)

        // Set Listeners
        chronoStartButton.setOnClickListener {
            chronometerManager.start(false, this)
        }

        chronoStartWithAudioButton.setOnClickListener {
            chronometerManager.start(true, this)
        }

        chronoStopButton.setOnClickListener {
            chronometerManager.stop()
        }

        chronoResetButton.setOnClickListener {
            chronometerManager.reset()
            chronometerDisplay.text = "00:00"
            updateButtons(false)
        }

        homeButton.setOnClickListener {
            val intent = Intent(this, LauncherActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onTimeUpdate(minutes: Int, seconds: Int) {
        val timeString = String.format("%02d:%02d", minutes, seconds)
        chronometerDisplay.text = timeString
    }

    override fun onStateChanged(running: Boolean) {
        updateButtons(running)
    }

    override fun onAudioRecordingStarted() {
        // Optional: Show some indicator that audio is recording
    }

    override fun onAudioRecordingStopped(fileName: String) {
        // Optional: Show confirmation
    }

    private fun updateButtons(running: Boolean) {
        if (running) {
            chronoStartButton.visibility = View.GONE
            chronoStartWithAudioButton.visibility = View.GONE
            chronoStopButton.visibility = View.VISIBLE
        } else {
            chronoStartButton.visibility = View.VISIBLE
            chronoStartWithAudioButton.visibility = View.VISIBLE
            chronoStopButton.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chronometerManager.cleanup()
    }
}
