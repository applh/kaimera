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
import com.badlogic.gdx.math.Quaternion

import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.scene.Scene

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
    // Use DirectionalLightEx for easier intensity control and better coverage
    private lateinit var camLight: DirectionalLightEx
    
    // GLTF Props
    private lateinit var sceneManager: SceneManager
    private lateinit var sceneAsset: SceneAsset
    private lateinit var character: Character
    
    // Grid settings
    private var latSegments = 24
    private var longSegments = 24

    // Debug Marker & Lines
    private lateinit var markerModel: Model
    private lateinit var markerInstance: ModelInstance
    private var showMarker = false
    
    private lateinit var debugModel: Model
    private lateinit var debugInstance: ModelInstance
    private var isDebugModelInit = false

    override fun show() {
        // HUD Init
        batch = SpriteBatch()
        generateFont()

        // 3D Init
        modelBatch = ModelBatch()
        environment = Environment()
        // Boost ambient to avoid grey/black shadows
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f))
        // Static sun (optional, keeping it simple for now)
        // environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        cam = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam.position.set(5f, 5f, 5f)
        cam.lookAt(0f, 0f, 0f)
        cam.near = 1f
        cam.far = 300f
        cam.update()

        // Add Camera Light (Directional Headlamp)
        camLight = DirectionalLightEx()
        camLight.set(Color.WHITE, cam.direction)
        camLight.intensity = 1.0f
        environment.add(camLight)

        createSphere()
        createGrid()
        
        // Create debug marker
        val mb = ModelBuilder()
        markerModel = mb.createSphere(0.1f, 0.1f, 0.1f, 16, 16, 
            Material(ColorAttribute.createDiffuse(Color.RED)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        markerInstance = ModelInstance(markerModel)

        // Create custom input controller
        val inputController = InputController(cam)
        inputController.onTap = { x, y ->
            val ray = cam.getPickRay(x, y)
            val rayFrom = ray.origin
            val rayDir = ray.direction
            
            // Sphere intersection (Radius 2f at 0,0,0)
            val limitRadius = 2f
            
            val a = rayDir.dot(rayDir)
            val b = 2 * rayFrom.dot(rayDir)
            val c = rayFrom.dot(rayFrom) - limitRadius * limitRadius
            
            val discriminant = b * b - 4 * a * c
            if (discriminant >= 0) {
                // Hit!
                val t1 = (-b - Math.sqrt(discriminant.toDouble())).toFloat() / (2 * a)
                
                // Use closest positive t
                val t = t1
                if (t >= 0) {
                     val hitPoint = rayFrom.cpy().mulAdd(rayDir, t)
                     Gdx.app.log("GameScreen", "Hit: $hitPoint")
                     
                     // Move marker
                     markerInstance.transform.setToTranslation(hitPoint)
                     showMarker = true
                     
                     character.moveTo(hitPoint)
                }
            }
        }
        Gdx.input.inputProcessor = inputController
        
        // GLTF Init
        // SceneManager creates its own ModelBatch and shaders
        val config = net.mgsx.gltf.scene3d.shaders.PBRShaderConfig()
        config.numBones = 60
        
        // Config for Depth Shader (Shadow pass)
        // PBRDepthShader uses DepthShader.Config
        val depthConfig = com.badlogic.gdx.graphics.g3d.shaders.DepthShader.Config()
        depthConfig.numBones = 60
        
        sceneManager = SceneManager(
            net.mgsx.gltf.scene3d.shaders.PBRShaderProvider(config),
            net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider(depthConfig)
        )
        sceneManager.setCamera(cam)
        sceneManager.environment = environment
        
        // Load GLB
        sceneAsset = GLBLoader().load(Gdx.files.internal("RobotExpressive.glb"))
        
        // Fix for "All Grey" / Dark appearance:
        // PBR models with high metalness look black/grey without an Environment Map (IBL).
        // We set metalness to 0 (dielectric) and roughness to 1 (matte) to ensure standard lights work.
        for (mat in sceneAsset.scene.model.materials) {
            // Force non-metallic, fully rough
            mat.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createMetallic(0f))
            mat.set(net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute.createRoughness(10f))
        }
        
        val scene = Scene(sceneAsset.scene)
        sceneManager.addScene(scene)
        
        // Create Character
        character = Character(scene, 2f) // radius matches sphere radius (Diameter 4f -> Radius 2f)
        character.setSpeed(2f) // Start walking
    }

    private fun createSphere() {
        val texture = com.badlogic.gdx.graphics.Texture(Gdx.files.internal("earth_map_clean.jpg"))
        val modelBuilder = ModelBuilder()
        sphereModel = modelBuilder.createSphere(
            4f, 4f, 4f,
            32, 32,
            Material(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.createDiffuse(texture)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()
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
            
            for (j in 1..safeLongSegments * 2) { 
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
    
    // Debug Toggle
    private var showDebug = false
    
    fun setShowDebug(enabled: Boolean) {
        this.showDebug = enabled
        if (!enabled) {
            if (isDebugModelInit) {
                debugModel.dispose()
                isDebugModelInit = false
            }
            if (::markerModel.isInitialized && showMarker) {
               showMarker = false
            }
        }
    }
    
    fun getShowDebug(): Boolean = showDebug

    private fun updateDebugLines() {
        if (!showDebug || !character.hasTarget()) return
        
        val mb = ModelBuilder()
        mb.begin()
        
        val sphereRadius = 2f
        val liftYellow = 0.05f
        val liftBlue = 0.08f 
        val width = 0.0125f 
        
        // Materials with Alpha
        val yellowMat = Material(
            ColorAttribute.createDiffuse(Color(1f, 1f, 0f, 0.5f)),
            com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        )
        val blueMat = Material(
            ColorAttribute.createDiffuse(Color(0f, 0f, 1f, 0.5f)),
            com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        )
        val greenMat = Material(
            ColorAttribute.createDiffuse(Color(0f, 1f, 0f, 0.5f)),
            com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        )
        
        // 1. Path to Target (Yellow Ribbon)
        val target = character.getTarget()!!
        
        // Calculate Arc Angle to target
        val p1 = character.position.cpy().nor()
        val p2 = target.cpy().nor()
        val dot = p1.dot(p2).coerceIn(-1f, 1f)
        val arcAngleRad = Math.acos(dot.toDouble()).toFloat()
        val arcAngleDeg = arcAngleRad * 57.2958f
        
        val dist = character.position.dst(target)
        if (dist > 0.01f) {
            val part = mb.part("path", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), yellowMat)
            createRibbonArc(part, character.position, target, sphereRadius + liftYellow, width, 20)
        }
        
        // 2. Forward Vector (Blue Ribbon - Predicted Path)
        // Length matches Yellow path
        val moveAxis = character.up.cpy().crs(character.forward).nor() // Left axis
        
        // Use calculated arc angle
        val rot = Quaternion().setFromAxis(moveAxis, arcAngleDeg) 
        val fwdEnd = character.position.cpy().mul(rot)
        
        val fwdPart = mb.part("fwd", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), blueMat)
        createRibbonArc(fwdPart, character.position, fwdEnd, sphereRadius + liftBlue, width, 20)
        
        // 3. Up Vector (Green Arrow)
        val upPart = mb.part("up", GL20.GL_TRIANGLES, (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(), greenMat)
        val upEnd = character.position.cpy().mulAdd(character.up, 1.5f)
        com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ArrowShapeBuilder.build(
             upPart,
             character.position.x, character.position.y, character.position.z,
             upEnd.x, upEnd.y, upEnd.z,
             0.05f, 0.15f, 5 
        )
        
        debugModel = mb.end()
        debugInstance = ModelInstance(debugModel)
        isDebugModelInit = true
    }
    
    private fun createRibbonArc(builder: com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder, start: com.badlogic.gdx.math.Vector3, end: com.badlogic.gdx.math.Vector3, radius: Float, width: Float, segments: Int) {
        val p1 = start.cpy().nor()
        val p2 = end.cpy().nor()
        // Rotation from p1 to p2
        val axis = p1.cpy().crs(p2).nor()
        val angle = Math.acos(p1.dot(p2).coerceIn(-1f, 1f).toDouble()).toFloat()
        val step = angle / segments
        
        val currPos = com.badlogic.gdx.math.Vector3()
        val currUp = com.badlogic.gdx.math.Vector3()
        val currRight = com.badlogic.gdx.math.Vector3()
        val q = Quaternion()
        
        val v1 = com.badlogic.gdx.math.Vector3()
        val v2 = com.badlogic.gdx.math.Vector3()
        val v3 = com.badlogic.gdx.math.Vector3()
        val v4 = com.badlogic.gdx.math.Vector3()
        
        // Initial points
        // We need previous points to form quads.
        // Or we can just emit vertices for specific shape builder if available?
        // MeshPartBuilder has .rect() etc.
        
        // Loop
        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val theta = angle * t
            q.setFromAxis(axis, theta * 57.2958f)
            
            currPos.set(p1).mul(q).nor()
            currUp.set(currPos) // Normal is Up
            
            // Tangent direction of arc? 
            // Tangent is (axis x currPos).
            val tangent = axis.cpy().crs(currPos).nor()
            
            // Ribbon "Right" is (Tangent x Up) ... which is just 'Axis' actually?
            // Wait, Axis is perpendicular to the plane of the arc.
            // So 'Axis' IS the 'Right' vector for the ribbon on the surface.
            // Or 'Left'.
            currRight.set(axis) 
            
            // Left/Right vertices
            val left = currPos.cpy().mulAdd(currRight, width).nor().scl(radius)
            val right = currPos.cpy().mulAdd(currRight, -width).nor().scl(radius)
            
            if (i > 0) {
                 // v1=prevLeft, v2=prevRight, v3=right, v4=left
                 // Normal is currUp
                 builder.rect(v1, v2, right, left, currUp)
            }
            
            v1.set(left)
            v2.set(right)
        }
    }

    override fun render(delta: Float) {
        // camController.update() handled by InputController logic now
        
        // Update light to follow camera direction (Headlamp)
        camLight.direction.set(cam.direction)

        // Clear screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        
        // Update debug lines
        if (isDebugModelInit) {
            debugModel.dispose() // Dispose previous frame's model
            isDebugModelInit = false
        }
        updateDebugLines()

        // 3D Render
        modelBatch.begin(cam)
        modelBatch.render(sphereInstance, environment)
        modelBatch.render(gridInstance, environment)
        if (showMarker) {
            modelBatch.render(markerInstance, environment)
        }
        if (isDebugModelInit && character.hasTarget()) {
           Gdx.gl.glLineWidth(8f)
           modelBatch.render(debugInstance)
           Gdx.gl.glLineWidth(1f)
        }
        modelBatch.end()

        // GLTF Render
        character.update(delta)
        sceneManager.update(delta)
        sceneManager.render()

        // HUD Render
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
        sceneManager.updateViewport(width.toFloat(), height.toFloat())
        batch.projectionMatrix.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        modelBatch.dispose()
        sphereModel.dispose()
        gridModel.dispose()
        if (::markerModel.isInitialized) {
            markerModel.dispose()
        }
        if (isDebugModelInit) {
            debugModel.dispose()
        }
        sceneManager.dispose()
        sceneAsset.dispose()
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
    
    fun updateCamLightColor(color: Int) {
         Gdx.app.postRunnable {
             Color.argb8888ToColor(camLight.color, color)
         }
    }
    
    fun updateCamLightIntensity(intensity: Float) {
         Gdx.app.postRunnable {
             camLight.intensity = intensity
         }
    }

    fun getHudColor(): Int {
        return Color.rgba8888(renderColor)
    }

    fun getCamLightColor(): Int {
        return Color.rgba8888(camLight.color)
    }

    fun getCamLightIntensity(): Float {
        return camLight.intensity
    }

    fun getLatSegments(): Int = latSegments
    fun getLongSegments(): Int = longSegments
}
