package com.example.kaimera.sphereqix

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.viewport.ScreenViewport

class GameScreen(private val game: SphereQixGame) : ScreenAdapter() {
    
    private lateinit var sphereMesh: SphereMesh
    private lateinit var shader: ShaderProgram
    private lateinit var camera: PerspectiveCamera
    private lateinit var stage: Stage
    private lateinit var touchpad: Touchpad

    private lateinit var inputController: InputController
    private lateinit var playerController: PlayerController
    private lateinit var enemyManager: EnemyManager
    private lateinit var levelManager: LevelManager

    override fun show() {
        sphereMesh = SphereMesh(2f, 2) // Radius 2, 2 subdivisions
        
        // ... (shader setup) ...
        val vertexShader = """
            attribute vec4 a_position;
            attribute vec3 a_normal;
            attribute vec4 a_color;
            uniform mat4 u_projViewTrans;
            uniform mat4 u_worldTrans;
            varying vec4 v_color;
            
            void main() {
                vec4 worldPos = u_worldTrans * a_position;
                vec3 normal = normalize((u_worldTrans * vec4(a_normal, 0.0)).xyz);
                
                // Multiple light sources for better illumination
                vec3 light1Dir = normalize(vec3(1.0, 1.0, 1.0));   // Top-right-front
                vec3 light2Dir = normalize(vec3(-1.0, 0.5, 0.5));  // Left
                vec3 light3Dir = normalize(vec3(0.0, -1.0, 0.5));  // Bottom
                
                float diff1 = max(dot(normal, light1Dir), 0.0) * 0.6;
                float diff2 = max(dot(normal, light2Dir), 0.0) * 0.3;
                float diff3 = max(dot(normal, light3Dir), 0.0) * 0.2;
                
                float ambient = 0.4;
                float lighting = ambient + diff1 + diff2 + diff3;
                
                v_color = a_color * vec4(vec3(lighting), 1.0);
                gl_Position = u_projViewTrans * worldPos;
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
        
        shader = ShaderProgram(vertexShader, fragmentShader)
        if (!shader.isCompiled) throw GdxRuntimeException("Shader compile error: " + shader.log)
        
        // ... (Camera setup) ...
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 0f, 5f)
        camera.lookAt(0f, 0f, 0f)
        camera.near = 0.1f
        camera.far = 100f
        camera.update()

        // Joystick Setup - will be sized to controls area (20% of screen)
        val controlsHeight = (Gdx.graphics.height * 0.2f).toInt()
        stage = Stage(com.badlogic.gdx.utils.viewport.FitViewport(Gdx.graphics.width.toFloat(), controlsHeight.toFloat()))
        createJoystick()

        inputController = InputController(camera)
        playerController = PlayerController(camera, 2f)
        enemyManager = EnemyManager(2f)
        levelManager = LevelManager(enemyManager, sphereMesh)
        
        levelManager.startLevel(0) // Start with training level

        // Input Multiplexer: Stage first (UI), then Camera Controller
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(stage)
        multiplexer.addProcessor(inputController)
        Gdx.input.inputProcessor = multiplexer
    }

    // Button states for directional controls
    private var upPressed = false
    private var downPressed = false
    private var leftPressed = false
    private var rightPressed = false
    private var rotateLeftPressed = false
    private var rotateRightPressed = false
    
    private fun createJoystick() {
        // Joystick replaced with simple button controls
        // Buttons will be rendered directly in the render method
    }

    override fun render(delta: Float) {
        val screenWidth = Gdx.graphics.width
        val screenHeight = Gdx.graphics.height
        val controlsHeight = (screenHeight * 0.2f).toInt()
        val sceneHeight = screenHeight - controlsHeight
        
        // Clear entire screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        
        // ===== RENDER 3D SCENE (Top 80%) =====
        Gdx.gl.glViewport(0, controlsHeight, screenWidth, sceneHeight)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_CULL_FACE)
        Gdx.gl.glDisable(GL20.GL_BLEND)
        
        camera.viewportWidth = screenWidth.toFloat()
        camera.viewportHeight = sceneHeight.toFloat()
        camera.update()
        
        if (!levelManager.isGameOver && !levelManager.isLevelComplete) {
            // Convert button presses to joystick-like input
            val joyX = if (leftPressed) -1f else if (rightPressed) 1f else 0f
            val joyY = if (downPressed) -1f else if (upPressed) 1f else 0f
            
            // Handle player rotation buttons
            if (rotateLeftPressed) {
                playerController.rotateDirections(60f * delta)
            }
            if (rotateRightPressed) {
                playerController.rotateDirections(-60f * delta)
            }
            
            // Update player with button input
            playerController.update(delta, joyX, joyY, sphereMesh)
            
            // Check treasure collection (training level)
            levelManager.checkTreasureCollection(playerController.position)
            
            // Update enemies and check collision (not in training level)
            if (enemyManager.update(delta, playerController.position, playerController.currentLine)) {
                levelManager.onPlayerHit()
                playerController.stopDrawing(sphereMesh)
                playerController.position.set(0f, 0f, 2f)
            }
            
            levelManager.update()
        } else {
            if (Gdx.input.justTouched()) {
                if (levelManager.isGameOver) {
                    levelManager.lives = 3
                    levelManager.isGameOver = false
                    levelManager.startLevel(levelManager.currentLevel)
                } else if (levelManager.isLevelComplete) {
                    // Progress to next level (0 -> 1, 1 -> 2, etc.)
                    levelManager.startLevel(levelManager.currentLevel + 1)
                }
            }
        }
        
        shader.bind()
        shader.setUniformMatrix("u_projViewTrans", camera.combined)
        shader.setUniformMatrix("u_worldTrans", Matrix4())
        
        sphereMesh.render(shader)
        sphereMesh.renderGrid(camera)
        
        playerController.render(camera)
        enemyManager.render(camera)
        levelManager.renderTreasures(camera)
        
        // ===== RENDER CONTROLS AREA (Bottom 20%) =====
        Gdx.gl.glViewport(0, 0, screenWidth, controlsHeight)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        
        // Draw background for controls area
        val shapeRenderer = com.badlogic.gdx.graphics.glutils.ShapeRenderer()
        shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, screenWidth.toFloat(), controlsHeight.toFloat())
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.25f, 1f)
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), controlsHeight.toFloat())
        shapeRenderer.end()
        
        // Calculate button positions (D-pad layout + rotation buttons)
        val centerX = screenWidth / 2f
        val centerY = controlsHeight / 2f
        val buttonRadius = 60f
        val buttonSpacing = 150f
        
        // Directional button positions
        val upX = centerX
        val upY = centerY + buttonSpacing
        val downX = centerX
        val downY = centerY - buttonSpacing
        val leftX = centerX - buttonSpacing
        val leftY = centerY
        val rightX = centerX + buttonSpacing
        val rightY = centerY
        
        // Rotation button positions (further left and right)
        val rotateLeftX = centerX - buttonSpacing * 2.2f
        val rotateLeftY = centerY
        val rotateRightX = centerX + buttonSpacing * 2.2f
        val rotateRightY = centerY
        
        // Handle touch input for buttons
        if (Gdx.input.isTouched) {
            val touchX = Gdx.input.x.toFloat()
            val touchY = (screenHeight - Gdx.input.y).toFloat() // Flip Y coordinate
            
            // Only process if touch is in controls area
            if (touchY < controlsHeight) {
                upPressed = Math.sqrt(((touchX - upX) * (touchX - upX) + (touchY - upY) * (touchY - upY)).toDouble()) < buttonRadius
                downPressed = Math.sqrt(((touchX - downX) * (touchX - downX) + (touchY - downY) * (touchY - downY)).toDouble()) < buttonRadius
                leftPressed = Math.sqrt(((touchX - leftX) * (touchX - leftX) + (touchY - leftY) * (touchY - leftY)).toDouble()) < buttonRadius
                rightPressed = Math.sqrt(((touchX - rightX) * (touchX - rightX) + (touchY - rightY) * (touchY - rightY)).toDouble()) < buttonRadius
                rotateLeftPressed = Math.sqrt(((touchX - rotateLeftX) * (touchX - rotateLeftX) + (touchY - rotateLeftY) * (touchY - rotateLeftY)).toDouble()) < buttonRadius
                rotateRightPressed = Math.sqrt(((touchX - rotateRightX) * (touchX - rotateRightX) + (touchY - rotateRightY) * (touchY - rotateRightY)).toDouble()) < buttonRadius
            }
        } else {
            upPressed = false
            downPressed = false
            leftPressed = false
            rightPressed = false
            rotateLeftPressed = false
            rotateRightPressed = false
        }
        
        // Draw directional buttons
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        
        // Up button (Yellow when pressed)
        shapeRenderer.color = if (upPressed) com.badlogic.gdx.graphics.Color.YELLOW else com.badlogic.gdx.graphics.Color.CYAN
        shapeRenderer.circle(upX, upY, buttonRadius)
        
        // Down button (Yellow when pressed)
        shapeRenderer.color = if (downPressed) com.badlogic.gdx.graphics.Color.YELLOW else com.badlogic.gdx.graphics.Color.MAGENTA
        shapeRenderer.circle(downX, downY, buttonRadius)
        
        // Left button (Yellow when pressed)
        shapeRenderer.color = if (leftPressed) com.badlogic.gdx.graphics.Color.YELLOW else com.badlogic.gdx.graphics.Color.GREEN
        shapeRenderer.circle(leftX, leftY, buttonRadius)
        
        // Right button (Yellow when pressed)
        shapeRenderer.color = if (rightPressed) com.badlogic.gdx.graphics.Color.YELLOW else com.badlogic.gdx.graphics.Color.RED
        shapeRenderer.circle(rightX, rightY, buttonRadius)
        
        // Rotate Left button (Yellow when pressed, Orange default)
        shapeRenderer.color = if (rotateLeftPressed) com.badlogic.gdx.graphics.Color.YELLOW else com.badlogic.gdx.graphics.Color.ORANGE
        shapeRenderer.circle(rotateLeftX, rotateLeftY, buttonRadius * 0.8f)
        
        // Rotate Right button (Yellow when pressed, Purple default)
        shapeRenderer.color = if (rotateRightPressed) com.badlogic.gdx.graphics.Color.YELLOW else com.badlogic.gdx.graphics.Color.PURPLE
        shapeRenderer.circle(rotateRightX, rotateRightY, buttonRadius * 0.8f)
        
        shapeRenderer.end()
        shapeRenderer.dispose()
        
        // Render Stage (now empty, but keep for future use)
        stage.viewport.update(screenWidth, controlsHeight, true)
        stage.act(delta)
        stage.draw()
        
        // ===== RENDER HUD (Overlay on top) =====
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        
        // Draw a test rectangle to verify 2D rendering works
        val testShape = com.badlogic.gdx.graphics.glutils.ShapeRenderer()
        testShape.projectionMatrix.setToOrtho2D(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        testShape.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        testShape.color = com.badlogic.gdx.graphics.Color.RED
        testShape.rect(50f, screenHeight - 100f, 200f, 50f) // Red rectangle at top
        testShape.end()
        testShape.dispose()
        
        levelManager.renderHUD(screenWidth, screenHeight)
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        
        // Joystick position is fixed in viewport coordinates, no need to update
    }

    override fun dispose() {
        sphereMesh.dispose()
        shader.dispose()
        stage.dispose()
    }
}
