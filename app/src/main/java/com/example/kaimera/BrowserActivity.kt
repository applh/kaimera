package com.example.kaimera

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
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.clearFormData()
                    android.webkit.CookieManager.getInstance().removeAllCookies { success ->
                        if (success) {
                            Toast.makeText(this, "All browsing data cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Clear WebView storage
                    webView.clearSslPreferences()
                    android.webkit.WebStorage.getInstance().deleteAllData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Handle close button
        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            // Save home URL
            val homeUrl = etHomeUrl.text.toString().trim()
            if (homeUrl.isNotEmpty()) {
                setHomeUrl(homeUrl)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
