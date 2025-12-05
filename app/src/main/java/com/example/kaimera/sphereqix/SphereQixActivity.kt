package com.example.kaimera.sphereqix

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.kaimera.R

class SphereQixActivity : AndroidApplication() {
    
    private lateinit var game: SphereQixGame
    private var gameScreen: GameScreen? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sphere_qix)
        
        val config = AndroidApplicationConfiguration()
        config.useAccelerometer = false
        config.useCompass = false
        // config.useImmersiveMode = true // Handled by layout now
        
        game = SphereQixGame()
        val gameView = initializeForView(game, config)
        
        val container = findViewById<android.widget.FrameLayout>(R.id.game_container)
        container.addView(gameView)
        
        findViewById<FloatingActionButton>(R.id.fab_settings).setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun getGameScreen(): GameScreen? {
        if (gameScreen == null) {
            val screen = game.screen
            if (screen is GameScreen) {
                gameScreen = screen
            }
        }
        return gameScreen
    }

    private fun showSettingsDialog() {
        val screen = getGameScreen() ?: return
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_sphereqix_settings, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etHudText = dialogView.findViewById<EditText>(R.id.et_hud_text)
        val btnApply = dialogView.findViewById<Button>(R.id.btn_apply)
        val btnHome = dialogView.findViewById<android.widget.ImageButton>(R.id.btn_home)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val sbHue = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_hue)
        val sbSaturation = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_saturation)
        val sbValue = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_value)
        val sbLat = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBarLat)
        val sbLong = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBarLong)
        
        val tvHue = dialogView.findViewById<android.widget.TextView>(R.id.tv_hue_label)
        val tvSat = dialogView.findViewById<android.widget.TextView>(R.id.tv_sat_label)
        val tvVal = dialogView.findViewById<android.widget.TextView>(R.id.tv_val_label)
        val tvLat = dialogView.findViewById<android.widget.TextView>(R.id.tv_lat_label)
        val tvLong = dialogView.findViewById<android.widget.TextView>(R.id.tv_long_label)

        // Pre-fill text
        etHudText.setText(screen.getHudText())

        // Initial HSV values (default white)
        val hsv = floatArrayOf(0f, 0f, 1f)
        
        // Listener for SeekBars
        val seekBarListener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    when (seekBar?.id) {
                        R.id.sb_hue -> {
                           hsv[0] = progress.toFloat()
                           tvHue.text = "Hue: $progress"
                           updateColor()
                        }
                        R.id.sb_saturation -> {
                           hsv[1] = progress / 100f
                           tvSat.text = "Saturation: $progress%"
                           updateColor()
                        }
                        R.id.sb_value -> {
                           hsv[2] = progress / 100f
                           tvVal.text = "Value: $progress%"
                           updateColor()
                        }
                        R.id.seekBarLat -> {
                            tvLat.text = "Latitude Density: $progress"
                            screen.updateGrid(sbLat.progress, sbLong.progress)
                        }
                        R.id.seekBarLong -> {
                            tvLong.text = "Longitude Density: $progress"
                            screen.updateGrid(sbLat.progress, sbLong.progress)
                        }
                    }
                }
            }
            
            fun updateColor() {
                val color = Color.HSVToColor(hsv)
                screen.updateHudColor(color)
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }

        sbHue.setOnSeekBarChangeListener(seekBarListener)
        sbSaturation.setOnSeekBarChangeListener(seekBarListener)
        sbValue.setOnSeekBarChangeListener(seekBarListener)
        sbLat.setOnSeekBarChangeListener(seekBarListener)
        sbLong.setOnSeekBarChangeListener(seekBarListener)
        
        // Set initial progress
        sbHue.progress = 0
        sbSaturation.progress = 0
        sbValue.progress = 100
        
        // Get current grid density
        sbLat.progress = screen.getLatSegments()
        sbLong.progress = screen.getLongSegments()
        
        // Initialize labels
        tvLat.text = "Latitude Density: ${screen.getLatSegments()}"
        tvLong.text = "Longitude Density: ${screen.getLongSegments()}"

        btnApply.setOnClickListener {
            val newHudText = etHudText.text.toString()
            screen.updateHudText(newHudText)
            dialog.dismiss()
        }
        
        btnHome.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        // Add colors
        val colors = listOf(
            Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, 
            Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.LTGRAY
        )

        for (color in colors) {
            val colorView = View(this)
            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(10, 10, 10, 10)
            colorView.layoutParams = params
            colorView.setBackgroundColor(color)
            colorView.setOnClickListener {
                screen.updateHudColor(color)
                // Update sliders to match preset
                Color.colorToHSV(color, hsv)
                sbHue.progress = hsv[0].toInt()
                sbSaturation.progress = (hsv[1] * 100).toInt()
                sbValue.progress = (hsv[2] * 100).toInt()
                
                // Update labels
                tvHue.text = "Hue: ${sbHue.progress}"
                tvSat.text = "Saturation: ${sbSaturation.progress}%"
                tvVal.text = "Value: ${sbValue.progress}%"
            }
            colorContainer.addView(colorView)
        }

        dialog.show()
    }
}
