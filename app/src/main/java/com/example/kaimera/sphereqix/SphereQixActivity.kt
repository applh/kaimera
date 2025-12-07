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
        val btnClose = dialogView.findViewById<android.widget.ImageButton>(R.id.btn_close)
        val btnHome = dialogView.findViewById<android.widget.ImageButton>(R.id.btn_home)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val sbHue = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_hue)
        val sbSaturation = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_saturation)
        val sbValue = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_value)
        val sbLat = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBarLat)
        val sbLong = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBarLong)
        
        // Pre-fill text
        etHudText.setText(screen.getHudText())

        // Initial HSV values
        val hsvHud = floatArrayOf(0f, 0f, 1f)
        val hsvLight = floatArrayOf(0f, 0f, 1f)
        
        // Fetch current values
        Color.colorToHSV(screen.getHudColor(), hsvHud)
        Color.colorToHSV(screen.getCamLightColor(), hsvLight)
        val currentIntensity = screen.getCamLightIntensity()
        
        val rgTarget = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_color_target)
        val sbIntensity = dialogView.findViewById<android.widget.SeekBar>(R.id.sb_intensity)
        val tvIntensity = dialogView.findViewById<android.widget.TextView>(R.id.tv_intensity_label)
        
        // Init UI
        sbIntensity.progress = (currentIntensity * 10).toInt()
        tvIntensity.text = "Light Intensity: $currentIntensity"
        
        // State
        var editTarget = 0 // 0 = HUD, 1 = Light

        val tvHue = dialogView.findViewById<android.widget.TextView>(R.id.tv_hue_label)
        val tvSat = dialogView.findViewById<android.widget.TextView>(R.id.tv_sat_label)
        val tvVal = dialogView.findViewById<android.widget.TextView>(R.id.tv_val_label)
        val tvLat = dialogView.findViewById<android.widget.TextView>(R.id.tv_lat_label)
        val tvLong = dialogView.findViewById<android.widget.TextView>(R.id.tv_long_label)

        // Wrapper to update labels
        val updateLabels = {
             val hsv = if (editTarget == 0) hsvHud else hsvLight
             tvHue.text = "Hue: ${hsv[0].toInt()}"
             tvSat.text = "Saturation: ${(hsv[1] * 100).toInt()}%"
             tvVal.text = "Value: ${(hsv[2] * 100).toInt()}%"
        }

        // Wrapper to update color
        val updateColor = {
             val hsv = if (editTarget == 0) hsvHud else hsvLight
             val color = Color.HSVToColor(hsv)
             if (editTarget == 0) screen.updateHudColor(color) else screen.updateCamLightColor(color)
        }
        
        // Listener for SeekBars
        val seekBarListener = object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    when (seekBar?.id) {
                        R.id.sb_hue -> {
                            val hsv = if (editTarget == 0) hsvHud else hsvLight
                            hsv[0] = progress.toFloat()
                            updateLabels()
                            updateColor()
                        }
                        R.id.sb_saturation -> {
                            val hsv = if (editTarget == 0) hsvHud else hsvLight
                            hsv[1] = progress / 100f
                            updateLabels()
                            updateColor()
                        }
                        R.id.sb_value -> {
                            val hsv = if (editTarget == 0) hsvHud else hsvLight
                            hsv[2] = progress / 100f
                            updateLabels()
                            updateColor()
                        }
                        R.id.sb_intensity -> {
                            val intensity = progress / 10f
                            tvIntensity.text = "Light Intensity: $intensity"
                            screen.updateCamLightIntensity(intensity)
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
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        }

        sbHue.setOnSeekBarChangeListener(seekBarListener)
        sbSaturation.setOnSeekBarChangeListener(seekBarListener)
        sbValue.setOnSeekBarChangeListener(seekBarListener)
        sbIntensity.setOnSeekBarChangeListener(seekBarListener)
        sbLat.setOnSeekBarChangeListener(seekBarListener)
        sbLong.setOnSeekBarChangeListener(seekBarListener)
        
        // Target Switch Logic
        val switchDebug = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_debug_arrows)
        
        // Init values
        if (gameScreen != null) {
            switchDebug.isChecked = gameScreen!!.getShowDebug()
        }
        
        switchDebug.setOnCheckedChangeListener { _, isChecked ->
            gameScreen?.setShowDebug(isChecked)
        }
        
        rgTarget.setOnCheckedChangeListener { _, checkedId ->
            editTarget = if (checkedId == R.id.rb_target_hud) 0 else 1
            
            // Update sliders to reflect current target
            val hsv = if (editTarget == 0) hsvHud else hsvLight
            sbHue.progress = hsv[0].toInt()
            sbSaturation.progress = (hsv[1] * 100).toInt()
            sbValue.progress = (hsv[2] * 100).toInt()
            
            updateLabels()
            
            // Light Intensity only relevant for Light? Or keep visible?
            // User requested intensity for light.
        }
        
        // Set initial progress
        sbHue.progress = hsvHud[0].toInt()
        sbSaturation.progress = (hsvHud[1] * 100).toInt()
        sbValue.progress = (hsvHud[2] * 100).toInt()
        
        updateLabels()
        
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
        
        btnClose.setOnClickListener {
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
                if (editTarget == 0) {
                    screen.updateHudColor(color)
                    Color.colorToHSV(color, hsvHud)
                } else {
                    screen.updateCamLightColor(color)
                    Color.colorToHSV(color, hsvLight)
                }
                
                // Update sliders
                val hsv = if (editTarget == 0) hsvHud else hsvLight
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
