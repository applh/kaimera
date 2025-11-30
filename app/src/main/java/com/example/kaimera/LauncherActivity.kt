package com.example.kaimera

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
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

        findViewById<ImageButton>(R.id.btnLaunchCamera).setOnClickListener {
            startMainActivity()
        }

        findViewById<ImageButton>(R.id.btnLaunchBrowser).setOnClickListener {
            val intent = Intent(this, BrowserActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnLaunchFileExplorer).setOnClickListener {
            val intent = Intent(this, FileExplorerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
