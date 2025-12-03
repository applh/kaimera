package com.example.kaimera.browser.ui

import com.example.kaimera.R
import com.example.kaimera.files.ui.FileExplorerActivity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnGo: Button
    private lateinit var btnSettings: ImageButton

    private val PREFS_NAME = "BrowserPrefs"
    private val KEY_HOME_URL = "home_url"
    private val DEFAULT_HOME_URL = "https://www.google.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        // Initialize views
        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        btnGo = findViewById(R.id.btnGo)
        btnSettings = findViewById(R.id.btnSettings)

        setupWebView()
        setupControls()

        // Load home page
        loadUrl(getHomeUrl())
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        // Register for context menu (long press)
        registerForContextMenu(webView)
        
        // Set up download listener
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            handleDownload(url, userAgent, contentDisposition, mimetype, contentLength)
        }
        
        // Handle page navigation within the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // Load in this WebView
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                etUrl.setText(url)
                updateNavigationButtons()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                updateNavigationButtons()
            }
        }

        // Handle progress and title
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
    
    override fun onCreateContextMenu(menu: android.view.ContextMenu?, v: View?, menuInfo: android.view.ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        
        val result = webView.hitTestResult
        when (result.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE, 
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                val url = result.extra ?: return
                menu?.setHeaderTitle("Link Options")
                menu?.add(0, 1, 0, "Open Link")?.setOnMenuItemClickListener {
                    webView.loadUrl(url)
                    true
                }
                menu?.add(0, 2, 0, "Download Link")?.setOnMenuItemClickListener {
                    handleDownload(url, webView.settings.userAgentString ?: "", "", "", 0)
                    true
                }
                menu?.add(0, 3, 0, "Copy Link")?.setOnMenuItemClickListener {
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("URL", url)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            WebView.HitTestResult.IMAGE_TYPE -> {
                val url = result.extra ?: return
                menu?.setHeaderTitle("Image Options")
                menu?.add(0, 4, 0, "Download Image")?.setOnMenuItemClickListener {
                    handleDownload(url, webView.settings.userAgentString ?: "", "", "image/*", 0)
                    true
                }
            }
        }
    }

    private fun setupControls() {
        btnGo.setOnClickListener {
            loadUrl(etUrl.text.toString())
        }

        etUrl.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loadUrl(etUrl.text.toString())
                true
            } else {
                false
            }
        }

        btnBack.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }

        btnForward.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }

        btnRefresh.setOnClickListener {
            webView.reload()
        }

        btnHome.setOnClickListener {
            loadUrl(getHomeUrl())
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun loadUrl(url: String) {
        var formattedUrl = url.trim()
        if (formattedUrl.isEmpty()) return

        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }

        webView.loadUrl(formattedUrl)
        // Hide keyboard
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etUrl.windowToken, 0)
    }

    private fun updateNavigationButtons() {
        btnBack.isEnabled = webView.canGoBack()
        btnBack.alpha = if (webView.canGoBack()) 1.0f else 0.5f
        
        btnForward.isEnabled = webView.canGoForward()
        btnForward.alpha = if (webView.canGoForward()) 1.0f else 0.5f
    }

    private fun getHomeUrl(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_HOME_URL, DEFAULT_HOME_URL) ?: DEFAULT_HOME_URL
    }

    private fun setHomeUrl(url: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(KEY_HOME_URL, url).apply()
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_browser_settings, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Initialize home URL
        val etHomeUrl = dialogView.findViewById<EditText>(R.id.etHomeUrl)
        etHomeUrl.setText(getHomeUrl())

        // Initialize download folder
        val etDownloadFolder = dialogView.findViewById<EditText>(R.id.etDownloadFolder)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentDownloadFolder = prefs.getString("download_folder", "downloads") ?: "downloads"
        etDownloadFolder.setText(currentDownloadFolder)

        // Initialize checkboxes with current settings
        val cbJavaScript = dialogView.findViewById<android.widget.CheckBox>(R.id.cbJavaScript)
        val cbDomStorage = dialogView.findViewById<android.widget.CheckBox>(R.id.cbDomStorage)
        val cbLoadImages = dialogView.findViewById<android.widget.CheckBox>(R.id.cbLoadImages)
        
        cbJavaScript.isChecked = webView.settings.javaScriptEnabled
        cbDomStorage.isChecked = webView.settings.domStorageEnabled
        cbLoadImages.isChecked = webView.settings.loadsImagesAutomatically

        // Initialize text size radio buttons
        val rgTextSize = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgTextSize)
        when (webView.settings.textZoom) {
            75 -> rgTextSize.check(R.id.rbTextSmall)
            100 -> rgTextSize.check(R.id.rbTextNormal)
            125 -> rgTextSize.check(R.id.rbTextLarge)
            150 -> rgTextSize.check(R.id.rbTextExtraLarge)
        }

        // Handle checkbox changes
        cbJavaScript.setOnCheckedChangeListener { _, isChecked ->
            webView.settings.javaScriptEnabled = isChecked
        }

        cbDomStorage.setOnCheckedChangeListener { _, isChecked ->
            webView.settings.domStorageEnabled = isChecked
        }

        cbLoadImages.setOnCheckedChangeListener { _, isChecked ->
            webView.settings.loadsImagesAutomatically = isChecked
        }

        // Handle text size changes
        rgTextSize.setOnCheckedChangeListener { _, checkedId ->
            webView.settings.textZoom = when (checkedId) {
                R.id.rbTextSmall -> 75
                R.id.rbTextNormal -> 100
                R.id.rbTextLarge -> 125
                R.id.rbTextExtraLarge -> 150
                else -> 100
            }
        }

        // Handle clear cache button
        dialogView.findViewById<Button>(R.id.btnClearCache).setOnClickListener {
            webView.clearCache(true)
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        }

        // Handle clear cookies button
        dialogView.findViewById<Button>(R.id.btnClearCookies).setOnClickListener {
            android.webkit.CookieManager.getInstance().removeAllCookies { success ->
                if (success) {
                    Toast.makeText(this, "Cookies cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle clear history button
        dialogView.findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            webView.clearHistory()
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
        }

        // Handle clear all data button
        dialogView.findViewById<Button>(R.id.btnClearAll).setOnClickListener {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will clear cache, cookies, history, and all stored data. Continue?")
                .setPositiveButton("Clear All") { _, _ ->
                    clearAllBrowsingData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Handle open downloads button
        dialogView.findViewById<Button>(R.id.btnOpenDownloads).setOnClickListener {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val downloadFolderName = prefs.getString("download_folder", "downloads") ?: "downloads"
            
            val intent = Intent(this, FileExplorerActivity::class.java).apply {
                putExtra("start_path", java.io.File(filesDir, downloadFolderName).absolutePath)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        // Handle close button
        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            // Save home URL
            val homeUrl = etHomeUrl.text.toString().trim()
            if (homeUrl.isNotEmpty()) {
                setHomeUrl(homeUrl)
            }
            
            // Save download folder
            val downloadFolder = etDownloadFolder.text.toString().trim()
            if (downloadFolder.isNotEmpty()) {
                prefs.edit().putString("download_folder", downloadFolder).apply()
            }
            
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearAllBrowsingData() {
        try {
            // Clear WebView in-memory data
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            webView.clearSslPreferences()
            
            // Clear cookies
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            
            // Clear WebStorage (localStorage, sessionStorage, WebSQL)
            android.webkit.WebStorage.getInstance().deleteAllData()
            
            // Clear WebView databases
            val databasePath = applicationContext.getDatabasePath("webview.db")?.parent
            databasePath?.let { path ->
                val dir = java.io.File(path)
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("webview")) {
                            file.delete()
                        }
                    }
                }
            }
            
            // Clear app_webview directory (main WebView data storage)
            val webviewDir = java.io.File(applicationContext.dataDir, "app_webview")
            if (webviewDir.exists()) {
                deleteRecursive(webviewDir)
            }
            
            // Clear cache directory
            cacheDir.deleteRecursively()
            
            Toast.makeText(this, "All browsing data cleared completely", Toast.LENGTH_LONG).show()
            
            // Reload home page
            loadUrl(getHomeUrl())
        } catch (e: Exception) {
            Toast.makeText(this, "Error clearing data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteRecursive(fileOrDirectory: java.io.File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    private fun handleDownload(url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) {
        try {
            // Get download folder name from preferences
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val downloadFolderName = prefs.getString("download_folder", "downloads") ?: "downloads"
            
            // Create downloads directory if it doesn't exist
            val downloadsDir = java.io.File(filesDir, downloadFolderName)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Extract filename from content disposition or URL
            var filename = extractFilename(contentDisposition, url)
            
            // Handle file naming conflicts
            filename = getUniqueFilename(downloadsDir, filename)
            
            val outputFile = java.io.File(downloadsDir, filename)
            
            Toast.makeText(this, "Downloading: $filename", Toast.LENGTH_SHORT).show()
            
            // Download file using OkHttp in background thread
            Thread {
                try {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", userAgent)
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            runOnUiThread {
                                Toast.makeText(this, "Download failed: HTTP ${response.code}", Toast.LENGTH_LONG).show()
                            }
                            return@Thread
                        }
                        
                        response.body?.let { body ->
                            val inputStream = body.byteStream()
                            val outputStream = java.io.FileOutputStream(outputFile)
                            
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                            }
                            
                            outputStream.flush()
                            outputStream.close()
                            inputStream.close()
                            
                            runOnUiThread {
                                Toast.makeText(this, "Download complete: $filename", Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            runOnUiThread {
                                Toast.makeText(this, "Download failed: Empty response", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun extractFilename(contentDisposition: String, url: String): String {
        // Try to extract filename from Content-Disposition header
        if (contentDisposition.isNotEmpty()) {
            val filenamePattern = "filename=\"?([^\"]+)\"?".toRegex()
            val match = filenamePattern.find(contentDisposition)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        // Fall back to extracting from URL
        val uri = android.net.Uri.parse(url)
        val lastSegment = uri.lastPathSegment
        if (!lastSegment.isNullOrEmpty()) {
            return lastSegment
        }
        
        // Default filename
        return "download_${System.currentTimeMillis()}"
    }
    
    private fun getUniqueFilename(directory: java.io.File, filename: String): String {
        var uniqueFilename = filename
        var counter = 1
        
        while (java.io.File(directory, uniqueFilename).exists()) {
            val dotIndex = filename.lastIndexOf('.')
            uniqueFilename = if (dotIndex > 0) {
                val name = filename.substring(0, dotIndex)
                val extension = filename.substring(dotIndex)
                "${name}_${counter}${extension}"
            } else {
                "${filename}_${counter}"
            }
            counter++
        }
        
        return uniqueFilename
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
