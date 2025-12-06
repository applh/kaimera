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
    private var textContent: String = "H",
    private var textColor: Int = android.graphics.Color.WHITE,
    private val typeface: android.graphics.Typeface? = null
) : ApplicationAdapter(), GestureDetector.GestureListener {

    private lateinit var camera: PerspectiveCamera
    private lateinit var batch: SpriteBatch
    private lateinit var uiBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var gestureDetector: GestureDetector

    // 3D Mesh for Side Walls
    private var sideMesh: com.badlogic.gdx.graphics.Mesh? = null
    // Store offsets to center the text
    private var textOffsetX: Float = 0f
    private var textOffsetY: Float = 0f
    
    // Scale adjustment to match mesh geometry
    private var textScaleAdjustment: Float = 1f
    
    // Debug bounds
    private var meshMinX: Float = 0f
    private var meshMaxX: Float = 0f
    private var meshMinY: Float = 0f
    private var meshMaxY: Float = 0f
    
    private val meshGenerator = Text3DMeshGenerator()
    private lateinit var shader: com.badlogic.gdx.graphics.glutils.ShaderProgram

    // HUD Text
    private var hudTextContent: String = "HUD Text"

    // Render color (tint)
    private val renderColor = Color(1f, 1f, 1f, 1f)
    private val sideColor = Color(0.6f, 0.6f, 0.6f, 1f) // Default greyish for sides
    private val backFaceColor = Color(0.5f, 0.5f, 0.5f, 1f) // Default for back face
    private val backgroundColor = Color(0.1f, 0.1f, 0.1f, 1f)
    
    // Extrusion properties
    private var extrusionDepth = 20 // Default depth units
    private val density = 40f // Layers per unit depth (higher = more solid)

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
        
        // Generate font first so we can calculate size
        generateFont()

        // Initial distance calculation
        calculateOptimalDistance()
        updateCameraPosition()
        
        // Generate font first so we can calculate size
        generateFont()

        // Initialize shader for mesh
        val vertexShader = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            void main() {
                v_color = a_color;
                gl_Position = u_projTrans * a_position;
            }
        """.trimIndent()
        
        val fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            void main() {
                gl_FragColor = v_color;
            }
        """.trimIndent()
        
        shader = com.badlogic.gdx.graphics.glutils.ShaderProgram(vertexShader, fragmentShader)
        if (!shader.isCompiled) {
            Gdx.app.error("Shader", shader.log)
        }

        // Generate initial mesh
        updateMesh()

        // Initial distance calculation
        calculateOptimalDistance()
        updateCameraPosition()
        
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
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camera.update()

        // Render text as a billboard (always facing camera) or just flat in 3D space
        // For this task, "3D text" usually implies it exists in 3D space.
        // We will render it on the XY plane at Z=0, and the camera orbits around it.
        
        // Disable culling so we see back faces and inside of extrusion
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)
        
        // Calculate text size to center it
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, textContent)
        val width = layout.width * textScale
        val height = layout.height * textScale
        
        if (Gdx.input.justTouched()) {
             Gdx.app.log("Text3DDebug", "Layout Metrics: Width=${layout.width}, Height=${layout.height} (Scaled W=$width, H=$height). CapHeight=${font.capHeight}, XHeight=${font.xHeight}, Ascent=${font.ascent}, Descent=${font.descent}")
        }
        
        val totalDepth = extrusionDepth * 0.01f // Scaling factor

        // Enable Depth Test
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        
        // 1. Draw 3D Mesh (sides, front face, and back face - all in one!)
        if (sideMesh != null && shader.isCompiled) {
             shader.bind()
             
             // Simple transformation: scale and center the mesh
             val meshWidth = meshMaxX - meshMinX
             val meshHeight = meshMaxY - meshMinY
             val meshTransX = -0.65f * meshWidth  // Center horizontally
             val meshTransY = -0.5f * meshHeight   // Center vertically
             
             // Apply transformations: scale first, then translate
             shader.setUniformMatrix("u_projTrans", camera.combined.cpy().scl(textScale).translate(meshTransX, meshTransY, 0f))
             sideMesh!!.render(shader, GL20.GL_TRIANGLES)
        }
        
        // All rendering is now done with the 3D mesh (front, back, and sides)
        // No more 2D SpriteBatch rendering needed!


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
        sideMesh?.dispose()
        shader.dispose()
    }

    private fun updateMesh() {
        if (typeface == null) return
        
        // Calculate font size (Needs to match generation)
        // We generated bitmat font at size 64.
        
        val totalDepth = extrusionDepth * 0.01f // Same scaling as render loop
        
        val meshData = meshGenerator.generateFullMesh(
            textContent,
            typeface,
            64f, // Match font generation size
            totalDepth / textScale, // Un-scale depth so it matches font coords
            sideColor.toFloatBits(),
            renderColor.toFloatBits(), // Front face color
            backFaceColor.toFloatBits() // Back face color
        )
        
        val attributes = com.badlogic.gdx.graphics.VertexAttributes(
            com.badlogic.gdx.graphics.VertexAttribute.Position(),
            com.badlogic.gdx.graphics.VertexAttribute.ColorPacked()
        )
        
        if (sideMesh == null || sideMesh!!.maxVertices < meshData.vertexCount) {
             sideMesh?.dispose()
             sideMesh = com.badlogic.gdx.graphics.Mesh(true, meshData.vertexCount, 0, attributes)
        }
        
        sideMesh!!.setVertices(meshData.vertices, 0, meshData.vertices.size)
        sideMesh!!.setVertices(meshData.vertices, 0, meshData.vertices.size)
        
        // Calculate proper centering
        val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, textContent)
        
        // Mesh bounds are in raw coordinates (not centered)
        val meshCenterX = (meshData.minX + meshData.maxX) / 2f
        val meshCenterY = (meshData.minY + meshData.maxY) / 2f
        
        // Text is drawn from baseline at (0,0)
        // font.draw() places the baseline at the Y coordinate
        // So if we draw at y=0, the text body extends UPWARD from 0
        // To center the text, we need to shift it DOWN by half its height
        // But user reports text is "one height below" the box, so we need to add layout.height
        
        textOffsetX = meshCenterX - (layout.width * 0.1f)  // Move 0.1 width to the right
        textOffsetY = meshCenterY + layout.height / 2f - (layout.height * 0.5f)  // Move text UP to center blue cross
        
        // No scale adjustment
        textScaleAdjustment = 1f
        
        // Save debug bounds
        meshMinX = meshData.minX
        meshMaxX = meshData.maxX
        meshMinY = meshData.minY
        meshMaxY = meshData.maxY
        
        Gdx.app.log("Text3DDebug", "=== ALIGNMENT ===")
        Gdx.app.log("Text3DDebug", "Mesh center: (${meshCenterX}, ${meshCenterY})")
        Gdx.app.log("Text3DDebug", "Mesh bounds: X=[${meshData.minX}, ${meshData.maxX}] Y=[${meshData.minY}, ${meshData.maxY}]")
        Gdx.app.log("Text3DDebug", "Text offset: (${textOffsetX}, ${textOffsetY})")
        Gdx.app.log("Text3DDebug", "Layout: W=${layout.width}, H=${layout.height}")
    }

    // Public API for Android Activity
    fun getText(): String = textContent
    fun getHudText(): String = hudTextContent

    fun updateText(text: String) {
        Gdx.app.postRunnable {
            textContent = text
            updateMesh()
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

    fun updateBackgroundColor(color: Int) {
        Gdx.app.postRunnable {
            Color.argb8888ToColor(backgroundColor, color)
        }
    }

    fun updateExtrusionDepth(depth: Int) {
        Gdx.app.postRunnable {
            extrusionDepth = depth
            updateMesh()
        }
    }
    
    fun updateSideColor(color: Int) {
        Gdx.app.postRunnable {
            Color.argb8888ToColor(sideColor, color)
            // Update Mesh Color by regenerating
            // Ideally we just update color attributes, but regeneration is fast enough here.
            updateMesh()
        }
    }
    
    fun updateBackFaceColor(color: Int) {
        Gdx.app.postRunnable {
            Color.argb8888ToColor(backFaceColor, color)
        }
    }
    
    // Getters for UI sync
    fun getRenderColor(): Int = Color.argb8888(renderColor)
    fun getSideColor(): Int = Color.argb8888(sideColor)
    fun getBackFaceColor(): Int = Color.argb8888(backFaceColor)
    fun getBackgroundColor(): Int = Color.argb8888(backgroundColor)
    
    fun getExtrusionDepth(): Int = extrusionDepth
    
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
