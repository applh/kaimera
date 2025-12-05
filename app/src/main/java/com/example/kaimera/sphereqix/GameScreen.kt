package com.example.kaimera.sphereqix

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder

class GameScreen(private val game: SphereQixGame) : ScreenAdapter() {

    // HUD Props
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private var text: String = "Hello World"
    private val renderColor = Color(1f, 1f, 1f, 1f)

    // 3D Props
    private lateinit var cam: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var sphereModel: Model
    private lateinit var sphereInstance: ModelInstance
    private lateinit var gridModel: Model
    private lateinit var gridInstance: ModelInstance
    private lateinit var environment: Environment
    private lateinit var camController: CameraInputController
    
    // Grid settings
    private var latSegments = 24
    private var longSegments = 24

    override fun show() {
        // HUD Init
        batch = SpriteBatch()
        generateFont()

        // 3D Init
        modelBatch = ModelBatch()
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position.set(5f, 5f, 5f)
        cam.lookAt(0f, 0f, 0f)
        cam.near = 1f
        cam.far = 300f
        cam.update()

        createSphere()
        createGrid()

        camController = CameraInputController(cam)
        camController.translateUnits = 30f // faster pan
        Gdx.input.inputProcessor = camController
    }

    private fun createSphere() {
        val modelBuilder = ModelBuilder()
        sphereModel = modelBuilder.createSphere(
            4f, 4f, 4f, // Diameter 4 (Radius 2)
            32, 32,
            Material(ColorAttribute.createDiffuse(Color.ROYAL)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
        sphereInstance = ModelInstance(sphereModel)
    }

    private fun createGrid() {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        val builder = modelBuilder.part("grid", GL20.GL_LINES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorUnpacked).toLong(), Material())
        builder.setColor(Color.CYAN)

        val radius = 2.01f // Slightly larger than sphere to avoid z-fighting
        
        // Prevent division by zero
        val safeLatSegments = if (latSegments < 2) 2 else latSegments
        val safeLongSegments = if (longSegments < 3) 3 else longSegments

        // Latitudes
        for (i in 0 until safeLatSegments) {
            val lat = Math.PI * i / safeLatSegments
            val y = (radius * Math.cos(lat)).toFloat()
            val r = (radius * Math.sin(lat)).toFloat()
            
            var prevX = r // angle 0
            var prevZ = 0f
            
            for (j in 1..safeLongSegments * 2) { // Ensure full circle resolution matches longitude density somewhat or fixed high res? 
                // Actually, let's keep the circle resolution high enough to look round, regardless of grid density, 
                // OR match the longitude lines. Matching longitude lines looks cleaner for a grid.
                val lng = Math.PI * j / safeLongSegments
                val x = (r * Math.cos(lng)).toFloat()
                val z = (r * Math.sin(lng)).toFloat()
                builder.line(prevX, y, prevZ, x, y, z)
                prevX = x
                prevZ = z
            }
        }

        // Longitudes
        for (i in 0 until safeLongSegments * 2) {
            val lng = Math.PI * i / safeLongSegments
            val cosLng = Math.cos(lng).toFloat()
            val sinLng = Math.sin(lng).toFloat()
            
            var prevY = radius
            var prevR = 0f
            
            for (j in 1..safeLatSegments) {
                val lat = Math.PI * j / safeLatSegments
                val y = (radius * Math.cos(lat)).toFloat()
                val r = (radius * Math.sin(lat)).toFloat()
                
                builder.line(prevR * cosLng, prevY, prevR * sinLng, r * cosLng, y, r * sinLng)
                
                prevY = y
                prevR = r
            }
        }

        gridModel = modelBuilder.end()
        gridInstance = ModelInstance(gridModel)
    }
    
    fun updateGrid(lat: Int, long: Int) {
        Gdx.app.postRunnable {
            latSegments = lat
            longSegments = long
            
            // Dispose old model
            if (::gridModel.isInitialized) {
                gridModel.dispose()
            }
            
            createGrid()
        }
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
        camController.update()

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // 3D Render
        modelBatch.begin(cam)
        modelBatch.render(sphereInstance, environment)
        modelBatch.render(gridInstance, environment)
        modelBatch.end()

        // HUD Render
        // Should we reset GL state for 2D? SpriteBatch usually works fine but checking depth might be needed off
        // modelBatch enables depth test. SpriteBatch disables it usually inside begin().
        batch.begin()
        font.color = renderColor
        
        val layout = GlyphLayout(font, text)
        val x = (Gdx.graphics.width - layout.width) / 2f
        val y = (Gdx.graphics.height + layout.height) / 2f
        
        font.draw(batch, text, x, y)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        cam.update()
        batch.projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        modelBatch.dispose()
        sphereModel.dispose()
        gridModel.dispose()
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

    fun getLatSegments(): Int = latSegments
    fun getLongSegments(): Int = longSegments
}
