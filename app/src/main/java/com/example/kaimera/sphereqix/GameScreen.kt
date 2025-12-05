package com.example.kaimera.sphereqix

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

class GameScreen(private val game: SphereQixGame) : ScreenAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private var text: String = "HUD TEXT VISIBLE"
    
    // Render color (tint)
    private val renderColor = Color(1f, 1f, 1f, 1f)

    override fun show() {
        batch = SpriteBatch()
        generateFont()
    }

    private fun generateFont() {
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Regular.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 64
        parameter.color = Color.WHITE
        parameter.borderWidth = 2f
        parameter.borderColor = Color.BLACK
        font = generator.generateFont(parameter)
        font.region.texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        generator.dispose()
    }

    override fun render(delta: Float) {
        // Clear screen to dark background
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Reset viewport to ensure we are drawing to the specific screen area we expect (full screen)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)

        batch.begin()
        font.color = renderColor
        
        // Center text
        val layout = GlyphLayout(font, text)
        val x = (Gdx.graphics.width - layout.width) / 2f
        val y = (Gdx.graphics.height + layout.height) / 2f

        font.draw(batch, text, x, y)
        
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        batch.projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }

    // API for Activity interactions
    fun updateText(newText: String) {
        Gdx.app.postRunnable {
            text = newText
        }
    }
    
    fun updateHudColor(color: Int) {
         Gdx.app.postRunnable {
             Color.argb8888ToColor(renderColor, color)
         }
    }

    fun getHudText(): String = text

    fun updateHudText(newText: String) {
        updateText(newText)
    }
}
