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
    
    private val shapeRenderer = com.badlogic.gdx.graphics.glutils.ShapeRenderer()
    
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

    
    
    private fun getTargetPercentage(): Float {
        return when (currentLevel) {
            1 -> 50f
            2 -> 60f
            3 -> 70f
            else -> 80f
        }
    }
    
    fun dispose() {
        shapeRenderer.dispose()
    }
}
