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
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val sbHue = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_hue)
        val sbSaturation = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_saturation)
        val sbValue = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_value)

        // Pre-fill text
        etHudText.setText(screen.getHudText())

        // Initial HSV values (default white)
        val hsv = floatArrayOf(0f, 0f, 1f)
        
        // Listener for SeekBars
        val seekBarListener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    hsv[0] = sbHue.progress.toFloat()
                    hsv[1] = sbSaturation.progress / 100f
                    hsv[2] = sbValue.progress / 100f
                    val color = Color.HSVToColor(hsv)
                    screen.updateHudColor(color)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }

        sbHue.setOnSeekBarChangeListener(seekBarListener)
        sbSaturation.setOnSeekBarChangeListener(seekBarListener)
        sbValue.setOnSeekBarChangeListener(seekBarListener)
        
        // Set initial progress
        sbHue.progress = 0
        sbSaturation.progress = 0
        sbValue.progress = 100

        btnApply.setOnClickListener {
            val newHudText = etHudText.text.toString()
            screen.updateHudText(newHudText)
            dialog.dismiss()
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
            }
            colorContainer.addView(colorView)
        }

        dialog.show()
    }
}
