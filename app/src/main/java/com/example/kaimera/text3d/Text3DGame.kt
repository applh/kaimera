package com.example.kaimera.text3d

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

class Text3DGame(
    private var textContent: String = "Hello World",
    private var textColor: Int = android.graphics.Color.WHITE
) : ApplicationAdapter(), GestureDetector.GestureListener {

    private lateinit var camera: PerspectiveCamera
    private lateinit var batch: SpriteBatch
    private lateinit var uiBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var gestureDetector: GestureDetector

    // HUD Text
    private var hudTextContent: String = "HUD Text"

    // Render color (tint)
    private val renderColor = Color(1f, 1f, 1f, 1f)

    // Camera control variables
    private var distance = 10f
    private var angleX = 0f
    private var angleY = 0f
    private val center = Vector3(0f, 0f, 0f)

    // Text properties
    private val textScale = 0.1f // Scale down font to world units

    override fun create() {
        batch = SpriteBatch()
        uiBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        
        // Initialize camera
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.near = 1f
        camera.far = 100f
        
        // Initial distance calculation
        calculateOptimalDistance()
        updateCameraPosition()

        // Set initial render color
        Color.argb8888ToColor(renderColor, textColor)

        // Generate font
        generateFont()

        // Input handling
        gestureDetector = GestureDetector(this)
        Gdx.input.inputProcessor = gestureDetector
    }

    private fun generateFont() {
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Roboto-Regular.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 64 // High res for scaling down
        parameter.color = Color.WHITE // Always white for tinting
        parameter.borderWidth = 2f
        parameter.borderColor = Color.BLACK
        font = generator.generateFont(parameter)
        font.setUseIntegerPositions(false)
        font.region.texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
        generator.dispose()
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camera.update()

        // Render text as a billboard (always facing camera) or just flat in 3D space
        // For this task, "3D text" usually implies it exists in 3D space.
        // We will render it on the XY plane at Z=0, and the camera orbits around it.
        
        batch.projectionMatrix = camera.combined
        batch.begin()
        
        // Calculate text size to center it
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, textContent)
        val width = layout.width * textScale
        val height = layout.height * textScale
        
        // Transform for 3D scaling
        val matrix = Matrix4()
        
        // Extrusion parameters
        val layers = 20
        val layerDepth = 0.02f // Distance between layers
        
        // Render back layers (extrusion)
        for (i in 0 until layers) {
            matrix.idt()
            matrix.translate(-width / 2f, height / 2f, -i * layerDepth) // Stack backwards
            matrix.scl(textScale)
            
            val oldMatrix = batch.transformMatrix.cpy()
            batch.transformMatrix = matrix
            
            // Darker tint for depth
            val depthTint = Color(renderColor)
            depthTint.mul(0.6f) // Darken
            font.color = depthTint
            
            font.draw(batch, textContent, 0f, 0f)
            
            batch.transformMatrix = oldMatrix
        }
        
        // Render front layer
        matrix.idt()
        matrix.translate(-width / 2f, height / 2f, 0f)
        matrix.scl(textScale)
        
        val oldMatrix = batch.transformMatrix.cpy()
        batch.transformMatrix = matrix
        
        font.color = renderColor
        font.draw(batch, textContent, 0f, 0f)
        
        batch.transformMatrix = oldMatrix
        batch.end()

        // Render HUD
        uiBatch.begin()
        font.color = Color.WHITE // HUD always white (or make configurable)
        // Draw top-left. Coordinate system for SpriteBatch is usually bottom-left origin unless projection set.
        // But we didn't set projection for uiBatch, so it uses default (pixels, bottom-left 0,0).
        // So top-left is x=20, y=height-20.
        font.draw(uiBatch, hudTextContent, 20f, Gdx.graphics.height - 20f)
        uiBatch.end()
    }

    private fun calculateOptimalDistance() {
        if (!::font.isInitialized) return
        
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, textContent)
        val width = layout.width * textScale
        val height = layout.height * textScale
        
        // Use the larger dimension to fit
        val size = Math.max(width, height)
        
        // FOV calculation: tan(fov/2) = (size/2) / distance
        // distance = (size/2) / tan(fov/2)
        // FOV is 67 degrees
        val fovRad = 67f * MathUtils.degreesToRadians
        val distanceNeeded = (size / 2f) / Math.tan((fovRad / 2f).toDouble()).toFloat()
        
        // Add padding (increased to 3.0f as requested)
        this.distance = distanceNeeded * 3.0f
        
        // Clamp minimum distance
        if (this.distance < 5f) this.distance = 5f
    }

    private fun updateCameraPosition() {
        // Spherical coordinates to Cartesian
        val h = distance * MathUtils.cosDeg(angleY)
        val x = h * MathUtils.sinDeg(angleX)
        val z = h * MathUtils.cosDeg(angleX)
        val y = distance * MathUtils.sinDeg(angleY)

        camera.position.set(x, y, z)
        camera.lookAt(center)
        camera.up.set(0f, 1f, 0f) // Keep up vector roughly up, might need adjustment for full orbit
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        batch.dispose()
        uiBatch.dispose()
        font.dispose()
        shapeRenderer.dispose()
    }

    // Public API for Android Activity
    fun getText(): String = textContent
    fun getHudText(): String = hudTextContent

    fun updateText(text: String) {
        Gdx.app.postRunnable {
            textContent = text
            calculateOptimalDistance()
            updateCameraPosition()
        }
    }

    fun updateHudText(text: String) {
        Gdx.app.postRunnable {
            hudTextContent = text
        }
    }

    fun updateColor(color: Int) {
        Gdx.app.postRunnable {
            textColor = color
            Color.argb8888ToColor(renderColor, color)
        }
    }
    
    fun resetCamera() {
        calculateOptimalDistance()
        angleX = 0f
        angleY = 0f
        updateCameraPosition()
    }

    // GestureListener implementation
    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
        if (count == 2) {
            resetCamera()
            return true
        }
        return false
    }

    override fun longPress(x: Float, y: Float): Boolean = false

    override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean = false

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        angleX -= deltaX * 0.5f
        angleY += deltaY * 0.5f
        
        // Clamp pitch to avoid gimbal lock or flipping
        angleY = MathUtils.clamp(angleY, -89f, 89f)
        
        updateCameraPosition()
        return true
    }

    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

    override fun zoom(initialDistance: Float, distance: Float): Boolean {
        val ratio = initialDistance / distance
        // This is a simplified zoom, might need state to track continuous zoom
        // But GestureDetector calls zoom repeatedly. 
        // Actually, ratio is total ratio. We need delta.
        // Let's just use the difference to adjust distance.
        
        // A better approach for zoom in LibGDX GestureDetector is often tracking the previous distance.
        // But let's try a simple incremental approach if possible, or just use the ratio directly if we stored initial state.
        // Since we don't have easy access to "delta" zoom here without extra state:
        
        // Let's just use a sensitivity factor.
        // If ratio > 1, we are pinching in (zooming out? no, fingers getting closer).
        // If ratio < 1, fingers spreading (zooming in).
        
        // Actually, let's implement a simpler zoom:
        // We need to know if it's growing or shrinking.
        // The standard GestureDetector doesn't give delta easily.
        // Let's just use a simple check:
        
        // For now, let's leave zoom as a TODO or implement a simple version if possible.
        // We can use the difference in span from the input processor if we implemented it manually,
        // but GestureDetector simplifies things.
        
        // Let's try:
        if (ratio > 1) {
            this.distance += 0.1f
        } else {
            this.distance -= 0.1f
        }
        this.distance = MathUtils.clamp(this.distance, 2f, 50f)
        updateCameraPosition()
        
        return true
    }

    override fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean = false
    override fun pinchStop() {}

    fun scheduleSnapshot(callback: (com.badlogic.gdx.graphics.Pixmap) -> Unit) {
        Gdx.app.postRunnable {
            val pixmap = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.width, Gdx.graphics.height)
            // Flip because GL is bottom-up
            val flipped = com.badlogic.gdx.graphics.Pixmap(pixmap.width, pixmap.height, pixmap.format)
            val width = pixmap.width
            val height = pixmap.height
            for (x in 0 until width) {
                for (y in 0 until height) {
                    flipped.drawPixel(x, y, pixmap.getPixel(x, height - y - 1))
                }
            }
            pixmap.dispose()
            callback(flipped)
        }
    }
}
