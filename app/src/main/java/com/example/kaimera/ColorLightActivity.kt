package com.example.kaimera

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ColorLightActivity : AppCompatActivity() {

    private lateinit var colorContainer: View
    private lateinit var colorPad: View
    private lateinit var colorInfoText: TextView
    private lateinit var homeButton: FloatingActionButton

    private var currentHue = 0f
    private var currentBrightness = 1f
    private val handler = Handler(Looper.getMainLooper())
    private var hideLabelsRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_light)

        colorContainer = findViewById(R.id.colorContainer)
        colorPad = findViewById(R.id.colorPad)
        colorInfoText = findViewById(R.id.colorInfoText)
        homeButton = findViewById(R.id.homeButton)

        // Set up home button
        homeButton.setOnClickListener {
            finish()
        }

        // Set up touch listener for the entire container
        colorContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    handleTouch(event.x, event.y)
                    true
                }
                else -> false
            }
        }

        // Initialize with default color
        updateColor()
        
        // Hide labels initially
        colorInfoText.visibility = View.GONE
        
        // Set up square pad dimensions after layout
        colorContainer.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                colorContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                setupSquarePad()
            }
        })
    }

    private fun setupSquarePad() {
        val containerWidth = colorContainer.width
        val containerHeight = colorContainer.height
        val margin = 48 * resources.displayMetrics.density.toInt() // 48dp in pixels
        
        // Calculate square size as min(width, height) - 2 * margin
        val availableWidth = containerWidth - (2 * margin)
        val availableHeight = containerHeight - (2 * margin)
        val squareSize = minOf(availableWidth, availableHeight)
        
        // Set pad dimensions
        val layoutParams = colorPad.layoutParams as RelativeLayout.LayoutParams
        layoutParams.width = squareSize
        layoutParams.height = squareSize
        colorPad.layoutParams = layoutParams
    }

    private fun handleTouch(x: Float, y: Float) {
        val containerHeight = colorContainer.height.toFloat()
        val padTop = colorPad.top.toFloat()
        val padBottom = colorPad.bottom.toFloat()
        val padLeft = colorPad.left.toFloat()
        val padRight = colorPad.right.toFloat()

        // Check if touch is above the pad (brightness = 100%)
        if (y < padTop) {
            currentBrightness = 1f
        }
        // Check if touch is below the pad (brightness = 0%)
        else if (y > padBottom) {
            currentBrightness = 0f
        }
        // Touch is within the pad area
        else if (x >= padLeft && x <= padRight && y >= padTop && y <= padBottom) {
            val padWidth = padRight - padLeft
            val padHeight = padBottom - padTop
            
            // Calculate hue from X position within pad (0-360 degrees)
            val relativeX = x - padLeft
            currentHue = (relativeX / padWidth) * 360f
            
            // Calculate brightness from Y position within pad (inverted: top = bright, bottom = dark)
            val relativeY = y - padTop
            currentBrightness = 1f - (relativeY / padHeight)
        }

        updateColor()
        showLabelsTemporarily()
    }

    private fun updateColor() {
        // Convert HSV to RGB
        val hsv = floatArrayOf(currentHue, 1f, currentBrightness)
        val color = Color.HSVToColor(hsv)

        // Update background color
        colorContainer.setBackgroundColor(color)

        // Update info text
        val hueInt = currentHue.toInt()
        val brightnessPercent = (currentBrightness * 100).toInt()
        colorInfoText.text = "Hue: ${hueInt}° | Brightness: ${brightnessPercent}%"

        // Set text color based on brightness for readability
        val textColor = if (currentBrightness > 0.5f) Color.BLACK else Color.WHITE
        colorInfoText.setTextColor(textColor)
        
        // Update home button tint for visibility
        homeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (currentBrightness > 0.5f) Color.DKGRAY else Color.LTGRAY
        )
    }

    private fun showLabelsTemporarily() {
        // Show labels
        colorInfoText.visibility = View.VISIBLE
        
        // Cancel any pending hide action
        hideLabelsRunnable?.let { handler.removeCallbacks(it) }
        
        // Schedule hide after 5 seconds
        hideLabelsRunnable = Runnable {
            colorInfoText.visibility = View.GONE
        }
        handler.postDelayed(hideLabelsRunnable!!, 5000)
    }

    override fun onResume() {
        super.onResume()
        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler callbacks
        hideLabelsRunnable?.let { handler.removeCallbacks(it) }
    }
}

