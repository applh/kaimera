package com.example.kaimera.mms.ui

import com.example.kaimera.R
import com.example.kaimera.LauncherActivity
import com.example.kaimera.core.utils.FileUtils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.InputStream

class MmsActivity : AppCompatActivity() {

    private lateinit var phoneInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var pickFileButton: MaterialButton
    private lateinit var sendButton: MaterialButton
    private lateinit var fileInfoCard: MaterialCardView
    private lateinit var fileNameText: TextView
    private lateinit var fileInfoText: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    private var selectedFileSize: Long = 0
    private var selectedFileType: String? = null

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFileSelection(it) }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Permissions required to send MMS", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mms)

        // Initialize views
        phoneInput = findViewById(R.id.phoneInput)
        messageInput = findViewById(R.id.messageInput)
        pickFileButton = findViewById(R.id.pickFileButton)
        sendButton = findViewById(R.id.sendButton)
        fileInfoCard = findViewById(R.id.fileInfoCard)
        fileNameText = findViewById(R.id.fileNameText)
        fileInfoText = findViewById(R.id.fileInfoText)
        progressBar = findViewById(R.id.progressBar)

        // Home button
        findViewById<FloatingActionButton>(R.id.homeButton).setOnClickListener {
            val intent = Intent(this, LauncherActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Set up listeners
        setupInputValidation()
        pickFileButton.setOnClickListener { openFilePicker() }
        sendButton.setOnClickListener { sendMms() }

        // Request permissions on start
        requestPermissions()
    }

    private fun setupInputValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }
        phoneInput.addTextChangedListener(textWatcher)
    }

    private fun validateForm() {
        val phoneNumber = phoneInput.text?.toString()?.trim() ?: ""
        val hasFile = selectedFileUri != null

        // Enable send button if phone number is not empty and file is selected
        sendButton.isEnabled = phoneNumber.isNotEmpty() && hasFile
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS
        )

        // Add storage permissions based on API level
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun openFilePicker() {
        // Accept images, videos, and audio
        filePickerLauncher.launch("*/*")
    }

    private fun handleFileSelection(uri: Uri) {
        try {
            // Get file info
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    
                    selectedFileName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    selectedFileSize = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0
                }
            }

            // Get MIME type
            selectedFileType = contentResolver.getType(uri)
            
            // Validate file type
            if (!isValidFileType(selectedFileType)) {
                Toast.makeText(this, "Please select an image, video, or audio file", Toast.LENGTH_SHORT).show()
                return
            }

            // Validate file size (warn if > 1MB)
            val fileSizeMB = selectedFileSize / (1024.0 * 1024.0)
            if (fileSizeMB > 1.0) {
                Toast.makeText(
                    this,
                    String.format("Warning: File is %.1f MB. MMS may fail with large files.", fileSizeMB),
                    Toast.LENGTH_LONG
                ).show()
            }

            // Store URI and update UI
            selectedFileUri = uri
            updateFileInfo()
            validateForm()

        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidFileType(mimeType: String?): Boolean {
        return FileUtils.isValidMmsFileType(mimeType)
    }

    private fun updateFileInfo() {
        if (selectedFileUri != null) {
            fileNameText.text = selectedFileName ?: "Unknown file"
            
            val typeStr = when {
                selectedFileType?.startsWith("image/") == true -> "Image"
                selectedFileType?.startsWith("video/") == true -> "Video"
                selectedFileType?.startsWith("audio/") == true -> "Audio"
                else -> "File"
            }
            
            val sizeStr = formatFileSize(selectedFileSize)
            fileInfoText.text = "$typeStr • $sizeStr"
            
            fileInfoCard.visibility = View.VISIBLE
        } else {
            fileInfoCard.visibility = View.GONE
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return FileUtils.formatFileSize(bytes)
    }

    private fun sendMms() {
        val phoneNumber = phoneInput.text?.toString()?.trim() ?: ""
        val messageBody = messageInput.text?.toString()?.trim() ?: ""
        val fileUri = selectedFileUri

        if (phoneNumber.isEmpty() || fileUri == null) {
            Toast.makeText(this, "Please enter phone number and select a file", Toast.LENGTH_SHORT).show()
            return
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
            requestPermissions()
            return
        }

        // Show progress
        progressBar.visibility = View.VISIBLE
        sendButton.isEnabled = false

        try {
            val smsManager = SmsManager.getDefault()
            
            // Read file data
            val inputStream: InputStream? = contentResolver.openInputStream(fileUri)
            val fileData = inputStream?.readBytes()
            inputStream?.close()

            if (fileData == null) {
                throw Exception("Could not read file data")
            }

            // Send MMS
            smsManager.sendMultimediaMessage(
                this,
                fileUri,
                null, // location URL
                null, // config overrides
                null  // sent intent
            )

            Toast.makeText(this, "MMS sent successfully!", Toast.LENGTH_LONG).show()
            
            // Clear form
            phoneInput.text?.clear()
            messageInput.text?.clear()
            selectedFileUri = null
            updateFileInfo()
            validateForm()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send MMS: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            progressBar.visibility = View.GONE
            sendButton.isEnabled = true
        }
    }
}
