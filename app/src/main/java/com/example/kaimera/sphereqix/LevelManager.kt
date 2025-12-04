package com.example.kaimera.sphereqix

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3

class LevelManager(private val enemyManager: EnemyManager, private val sphereMesh: SphereMesh) {

    var currentLevel = 0
    var lives = 3
    var score = 0
    var capturePercentage = 0f
    
    var isGameOver = false
    var isLevelComplete = false
    
    // Training level (level 0) - treasure collection
    data class Treasure(val position: com.badlogic.gdx.math.Vector3, var collected: Boolean = false)
    private val treasures = ArrayList<Treasure>()
    private var totalTreasures = 0
    private var collectedTreasures = 0
    
    
    private val batch = com.badlogic.gdx.graphics.g2d.SpriteBatch()
    
    // Create a properly configured font using FreeType for Android
    private val font: com.badlogic.gdx.graphics.g2d.BitmapFont by lazy {
        try {
            val generator = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator(
                com.badlogic.gdx.Gdx.files.internal("fonts/Roboto-Regular.ttf")
            )
            val parameter = com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter()
            parameter.size = 48
            parameter.color = com.badlogic.gdx.graphics.Color.WHITE
            parameter.borderWidth = 2f
            parameter.borderColor = com.badlogic.gdx.graphics.Color.BLACK
            parameter.shadowOffsetX = 2
            parameter.shadowOffsetY = 2
            parameter.shadowColor = com.badlogic.gdx.graphics.Color(0f, 0f, 0f, 0.5f)
            val generatedFont = generator.generateFont(parameter)
            generator.dispose()
            com.badlogic.gdx.Gdx.app.log("LevelManager", "FreeType font loaded successfully")
            generatedFont
        } catch (e: Exception) {
            com.badlogic.gdx.Gdx.app.error("LevelManager", "Failed to load FreeType font, using default", e)
            // Fallback to default font with large scale
            com.badlogic.gdx.graphics.g2d.BitmapFont().apply {
                data.setScale(3f)
            }
        }
    }

    
    // Create a white 1x1 texture for drawing backgrounds
    private val whiteTexture: com.badlogic.gdx.graphics.Texture by lazy {
        val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE)
        pixmap.fill()
        val texture = com.badlogic.gdx.graphics.Texture(pixmap)
        pixmap.dispose()
        texture
    }


    fun startLevel(level: Int) {
        currentLevel = level
        isLevelComplete = false
        capturePercentage = 0f
        enemyManager.clear()
        
        if (level == 0) {
            // Training level - spawn treasures, no enemies
            spawnTreasures(5) // 5 treasures to collect
        } else {
            // Regular level - clear treasures, spawn enemies
            treasures.clear()
            totalTreasures = 0
            collectedTreasures = 0
            
            // Reset mesh state
            for (face in sphereMesh.faces) {
                face.isCaptured = false
            }
            
            // Pre-capture home area
            val homeCenter = Vector3(0f, 0f, sphereMesh.radius)
            val homeRadius = 0.5f
            
            for (face in sphereMesh.faces) {
                if (face.center.dst(homeCenter) < homeRadius) {
                    face.isCaptured = true
                }
            }
            
            // Spawn enemies based on level
            when (level) {
                1 -> enemyManager.spawnOrbiter()
                2 -> {
                    enemyManager.spawnOrbiter()
                    enemyManager.spawnOrbiter()
                }
                3 -> {
                    enemyManager.spawnOrbiter()
                    enemyManager.spawnChaser()
                }
                else -> {
                    enemyManager.spawnOrbiter()
                    enemyManager.spawnChaser()
                    enemyManager.spawnChaser()
                }
            }
        }
        
        sphereMesh.updateMeshVertices()
    }
    
    private fun spawnTreasures(count: Int) {
        treasures.clear()
        totalTreasures = count
        collectedTreasures = 0
        
        val random = java.util.Random()
        for (i in 0 until count) {
            // Random position on sphere
            val theta = random.nextFloat() * 2f * Math.PI.toFloat()
            val phi = Math.acos((2f * random.nextFloat() - 1f).toDouble()).toFloat()
            
            val x = sphereMesh.radius * Math.sin(phi.toDouble()).toFloat() * Math.cos(theta.toDouble()).toFloat()
            val y = sphereMesh.radius * Math.sin(phi.toDouble()).toFloat() * Math.sin(theta.toDouble()).toFloat()
            val z = sphereMesh.radius * Math.cos(phi.toDouble()).toFloat()
            
            treasures.add(Treasure(com.badlogic.gdx.math.Vector3(x, y, z)))
        }
    }
    
    fun checkTreasureCollection(playerPos: com.badlogic.gdx.math.Vector3) {
        if (currentLevel != 0) return
        
        for (treasure in treasures) {
            if (!treasure.collected && treasure.position.dst(playerPos) < 0.3f) {
                treasure.collected = true
                collectedTreasures++
                
                if (collectedTreasures >= totalTreasures) {
                    isLevelComplete = true
                }
            }
        }
    }
    
    fun renderTreasures(camera: com.badlogic.gdx.graphics.Camera) {
        if (currentLevel != 0) return
        
        val shapeRenderer = com.badlogic.gdx.graphics.glutils.ShapeRenderer()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        
        for (treasure in treasures) {
            if (!treasure.collected) {
                shapeRenderer.color = com.badlogic.gdx.graphics.Color.GOLD
                shapeRenderer.translate(treasure.position.x, treasure.position.y, treasure.position.z)
                shapeRenderer.box(-0.15f, -0.15f, -0.15f, 0.3f, 0.3f, 0.3f)
                shapeRenderer.translate(-treasure.position.x, -treasure.position.y, -treasure.position.z)
            }
        }
        
        shapeRenderer.end()
        shapeRenderer.dispose()
    }
    
    fun update() {
        if (isGameOver || isLevelComplete) return
        
        // Update capture percentage
        val totalFaces = sphereMesh.faces.size
        val capturedFaces = sphereMesh.faces.count { it.isCaptured }
        capturePercentage = (capturedFaces.toFloat() / totalFaces.toFloat()) * 100f
        
        // Check win condition (not for training level)
        if (currentLevel > 0 && capturePercentage >= getTargetPercentage() && !isLevelComplete) {
            isLevelComplete = true
        }
    }
    
    fun onPlayerHit() {
        lives--
        if (lives <= 0) {
            isGameOver = true
        }
    }

    fun renderHUD(screenWidth: Int, screenHeight: Int) {
        // Since text rendering isn't working, use shapes to show game state
        val shapeRenderer = com.badlogic.gdx.graphics.glutils.ShapeRenderer()
        shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled)
        
        // Draw background panel at top
        shapeRenderer.color = com.badlogic.gdx.graphics.Color(0.2f, 0.2f, 0.3f, 0.9f)
        shapeRenderer.rect(10f, screenHeight - 120f, screenWidth - 20f, 110f)
        
        val leftMargin = 30f
        val topY = screenHeight - 30f
        
        if (currentLevel == 0) {
            // Training level - show treasure progress as colored squares
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.GOLD
            for (i in 0 until totalTreasures) {
                val x = leftMargin + i * 60f
                if (i < collectedTreasures) {
                    // Collected - filled gold square
                    shapeRenderer.color = com.badlogic.gdx.graphics.Color.GOLD
                    shapeRenderer.rect(x, topY - 50f, 50f, 50f)
                } else {
                    // Not collected - gray outline
                    shapeRenderer.color = com.badlogic.gdx.graphics.Color.DARK_GRAY
                    shapeRenderer.rect(x, topY - 50f, 50f, 50f)
                }
            }
        } else {
            // Regular level - show lives as hearts and capture percentage as progress bar
            
            // Lives (hearts) - red filled circles
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.RED
            for (i in 0 until lives) {
                val x = leftMargin + i * 50f
                shapeRenderer.circle(x + 20f, topY - 25f, 20f)
            }
            
            // Empty hearts for lost lives
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.DARK_GRAY
            for (i in lives until 3) {
                val x = leftMargin + i * 50f
                shapeRenderer.circle(x + 20f, topY - 25f, 20f)
            }
            
            // Capture percentage as progress bar
            val barWidth = screenWidth - 60f
            val barHeight = 30f
            val barY = topY - 90f
            
            // Background (empty bar)
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.DARK_GRAY
            shapeRenderer.rect(leftMargin, barY, barWidth, barHeight)
            
            // Progress (filled bar)
            val fillWidth = barWidth * (capturePercentage / 100f)
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.GREEN
            shapeRenderer.rect(leftMargin, barY, fillWidth, barHeight)
            
            // Target marker (vertical line)
            val targetX = leftMargin + barWidth * (getTargetPercentage() / 100f)
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.YELLOW
            shapeRenderer.rect(targetX - 2f, barY - 5f, 4f, barHeight + 10f)
        }
        
        // Game over / Level complete overlays
        if (isGameOver) {
            // Red X overlay
            shapeRenderer.color = com.badlogic.gdx.graphics.Color(1f, 0f, 0f, 0.8f)
            shapeRenderer.rect(screenWidth / 2f - 200f, screenHeight / 2f - 200f, 400f, 400f)
            
            // Draw X shape
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.WHITE
            shapeRenderer.rectLine(screenWidth / 2f - 150f, screenHeight / 2f - 150f, 
                                   screenWidth / 2f + 150f, screenHeight / 2f + 150f, 20f)
            shapeRenderer.rectLine(screenWidth / 2f + 150f, screenHeight / 2f - 150f,
                                   screenWidth / 2f - 150f, screenHeight / 2f + 150f, 20f)
        } else if (isLevelComplete) {
            // Green checkmark overlay
            shapeRenderer.color = com.badlogic.gdx.graphics.Color(0f, 1f, 0f, 0.8f)
            shapeRenderer.rect(screenWidth / 2f - 200f, screenHeight / 2f - 200f, 400f, 400f)
            
            // Draw checkmark shape
            shapeRenderer.color = com.badlogic.gdx.graphics.Color.WHITE
            shapeRenderer.rectLine(screenWidth / 2f - 100f, screenHeight / 2f,
                                   screenWidth / 2f - 20f, screenHeight / 2f - 80f, 20f)
            shapeRenderer.rectLine(screenWidth / 2f - 20f, screenHeight / 2f - 80f,
                                   screenWidth / 2f + 120f, screenHeight / 2f + 120f, 20f)
        }
        
        shapeRenderer.end()
        shapeRenderer.dispose()
    }
    
    private fun getTargetPercentage(): Float {
        return when (currentLevel) {
            1 -> 50f
            2 -> 60f
            3 -> 70f
            else -> 80f
        }
    }
    
    fun dispose() {
        batch.dispose()
        font.dispose()
        whiteTexture.dispose()
    }
}
