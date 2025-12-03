package com.example.kaimera.debug.ui

import com.example.kaimera.R

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Debug activity to view app logs on-device without ADB
 * Access via Settings → Debug Logs
 */
class DebugLogActivity : AppCompatActivity() {
    
    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var refreshButton: Button
    private lateinit var clearButton: Button
    private lateinit var testButton: Button
    
    companion object {
        private const val TAG = "DebugLogActivity"
        private const val MAX_LOG_LINES = 500 // Limit to last 500 lines
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "DebugLogActivity opened")
        
        // Create layout programmatically
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
        }
        
        // Button container
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        
        refreshButton = Button(this).apply {
            text = "🔄 Refresh"
            setOnClickListener { 
                Log.d(TAG, "Refresh button clicked")
                loadLogs() 
            }
        }
        
        clearButton = Button(this).apply {
            text = "🗑️ Clear"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
            setOnClickListener { 
                Log.d(TAG, "Clear button clicked")
                clearLogs() 
            }
        }
        
        testButton = Button(this).apply {
            text = "🧪 Test"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
            setOnClickListener { 
                generateTestLogs()
            }
        }
        
        buttonLayout.addView(refreshButton)
        buttonLayout.addView(clearButton)
        buttonLayout.addView(testButton)
        
        // ScrollView with TextView
        scrollView = ScrollView(this)
        logTextView = TextView(this).apply {
            textSize = 10f
            setPadding(16, 16, 16, 16)
            setTextIsSelectable(true)
        }
        scrollView.addView(logTextView)
        
        mainLayout.addView(buttonLayout)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)
        
        title = "Debug Logs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        loadLogs()
    }
    
    private fun generateTestLogs() {
        Log.d(TAG, "=== TEST LOG START ===")
        Log.d(TAG, "Test log entry 1: Debug message")
        Log.i(TAG, "Test log entry 2: Info message")
        Log.w(TAG, "Test log entry 3: Warning message")
        Log.e(TAG, "Test log entry 4: Error message")
        Log.d("MainActivity", "Test log from MainActivity")
        Log.d("GalleryActivity", "Test log from GalleryActivity")
        Log.d(TAG, "=== TEST LOG END ===")
        
        android.widget.Toast.makeText(this, "Test logs generated! Tap Refresh to see them.", android.widget.Toast.LENGTH_LONG).show()
    }
    
    private fun loadLogs() {
        logTextView.text = "Loading logs...\n\nNote: On Android 4.1+, apps need READ_LOGS permission.\nIf you see 'No logs found', try:\n1. Tap 'Test' to generate logs\n2. Use ADB instead\n3. Install a log viewer app"
        
        Thread {
            try {
                // Try to get logcat output
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", MAX_LOG_LINES.toString(), "-v", "time"))
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                
                val log = StringBuilder()
                var lineCount = 0
                var totalLines = 0
                var line: String?
                
                // Filter for our app package
                while (bufferedReader.readLine().also { line = it } != null) {
                    totalLines++
                    if (line?.contains("com.example.kaimera") == true ||
                        line?.contains("GalleryActivity") == true ||
                        line?.contains("MainActivity") == true ||
                        line?.contains("StorageManager") == true ||
                        line?.contains("PreviewActivity") == true ||
                        line?.contains("SettingsFragment") == true ||
                        line?.contains("DebugLogActivity") == true ||
                        line?.contains("Kaimera") == true) {
                        log.append(line).append("\n")
                        lineCount++
                    }
                }
                
                bufferedReader.close()
                process.waitFor()
                
                runOnUiThread {
                    logTextView.text = if (log.isEmpty()) {
                        "No Kaimera logs found (scanned $totalLines total lines).\n\n" +
                        "📋 Troubleshooting:\n" +
                        "1. Tap '🧪 Test' to generate sample logs\n" +
                        "2. Use the app (take photos, open gallery)\n" +
                        "3. Tap '🔄 Refresh' to reload\n\n" +
                        "⚠️ Note: On Android 4.1+, apps may not have permission to read logs.\n\n" +
                        "✅ Alternative solutions:\n" +
                        "• Install 'Logcat Reader' from Play Store\n" +
                        "• Use ADB: adb logcat | grep Kaimera\n" +
                        "• Enable USB Debugging and connect to computer\n\n" +
                        "Process exit code: ${process.exitValue()}"
                    } else {
                        "✅ Found $lineCount Kaimera log entries (scanned $totalLines total)\n" +
                        "Tap 🔄 Refresh to update • Tap 🗑️ Clear to reset\n" +
                        "─".repeat(50) + "\n\n" +
                        log.toString()
                    }
                    
                    // Scroll to bottom
                    scrollView.post {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading logs", e)
                runOnUiThread {
                    logTextView.text = "❌ Error reading logs: ${e.message}\n\n" +
                            "Exception: ${e.javaClass.simpleName}\n\n" +
                            "This usually means:\n" +
                            "• Android 4.1+ restricts log access\n" +
                            "• App doesn't have READ_LOGS permission\n\n" +
                            "✅ Recommended solutions:\n\n" +
                            "1. Install 'Logcat Reader' app:\n" +
                            "   - Free from Play Store\n" +
                            "   - Filter by: com.example.kaimera\n\n" +
                            "2. Use ADB (computer required):\n" +
                            "   adb logcat | grep Kaimera\n\n" +
                            "3. Enable Developer Options:\n" +
                            "   Settings → About → Tap Build Number 7x\n" +
                            "   Then enable USB Debugging"
                }
            }
        }.start()
    }
    
    private fun clearLogs() {
        try {
            // Clear the logcat buffer
            Runtime.getRuntime().exec("logcat -c")
            
            runOnUiThread {
                logTextView.text = "✅ Logs cleared!\n\nUse the app, then tap 🔄 Refresh to see new logs.\n\nOr tap 🧪 Test to generate sample logs."
                android.widget.Toast.makeText(this, "Logs cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
            runOnUiThread {
                android.widget.Toast.makeText(this, "Failed to clear logs: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
