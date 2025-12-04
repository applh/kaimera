package com.example.kaimera.sphereqix

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import java.util.ArrayList
import java.util.Random

class EnemyManager(private val sphereRadius: Float) {

    abstract class Enemy(val position: Vector3, val speed: Float) {
        abstract fun update(delta: Float, playerPos: Vector3, currentLine: List<Vector3>)
    }

    class Orbiter(position: Vector3, speed: Float) : Enemy(position, speed) {
        private val axis = Vector3().setToRandomDirection()
        
        override fun update(delta: Float, playerPos: Vector3, currentLine: List<Vector3>) {
            // Rotate around axis
            position.rotate(axis, speed * delta)
            position.nor().scl(2f) // Keep on surface (radius 2)
        }
    }

    class Chaser(position: Vector3, speed: Float) : Enemy(position, speed) {
        override fun update(delta: Float, playerPos: Vector3, currentLine: List<Vector3>) {
            // Move towards player or line
            val target = if (currentLine.isNotEmpty()) currentLine.last() else playerPos
            
            // Simple spherical interpolation (slerp-like)
            // Or just move towards target vector and re-normalize
            val direction = target.cpy().sub(position).nor()
            position.mulAdd(direction, speed * delta)
            position.nor().scl(2f) // Keep on surface
        }
    }

    private val enemies = ArrayList<Enemy>()
    private val shapeRenderer = ShapeRenderer()
    private val random = Random()

    fun spawnOrbiter() {
        val pos = Vector3().setToRandomDirection().scl(sphereRadius)
        enemies.add(Orbiter(pos, 30f)) // 30 degrees per second
    }

    fun spawnChaser() {
        val pos = Vector3().setToRandomDirection().scl(sphereRadius)
        enemies.add(Chaser(pos, 1.5f)) // 1.5 units per second
    }
    
    fun clear() {
        enemies.clear()
    }

    fun update(delta: Float, playerPos: Vector3, currentLine: List<Vector3>): Boolean {
        var hit = false
        for (enemy in enemies) {
            enemy.update(delta, playerPos, currentLine)
            
            // Check collision with player
            if (enemy.position.dst(playerPos) < 0.2f) {
                hit = true
            }
            
            // Check collision with line
            for (point in currentLine) {
                if (enemy.position.dst(point) < 0.1f) {
                    hit = true
                    break
                }
            }
        }
        return hit
    }

    fun render(camera: Camera) {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        for (enemy in enemies) {
            if (enemy is Orbiter) {
                shapeRenderer.color = Color.MAGENTA
            } else {
                shapeRenderer.color = Color.ORANGE
            }
            
            shapeRenderer.translate(enemy.position.x, enemy.position.y, enemy.position.z)
            shapeRenderer.box(-0.1f, -0.1f, -0.1f, 0.2f, 0.2f, 0.2f)
            shapeRenderer.translate(-enemy.position.x, -enemy.position.y, -enemy.position.z)
        }
        
        shapeRenderer.end()
    }

    fun dispose() {
        shapeRenderer.dispose()
    }
}
