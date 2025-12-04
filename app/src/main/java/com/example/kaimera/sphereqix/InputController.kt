package com.example.kaimera.sphereqix

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3

class InputController(private val camera: Camera) : InputAdapter() {

    private val lastTouch = Vector3()
    private var isDragging = false
    
    // Orbit parameters
    private var angleX = 0f
    private var angleY = 0f
    private val sensitivity = 0.5f
    private val zoomSensitivity = 0.1f
    private val minZoom = 2.5f
    private val maxZoom = 10f
    
    // Distance from center
    private var distance = 5f

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == 0) {
            lastTouch.set(screenX.toFloat(), screenY.toFloat(), 0f)
            isDragging = true
            return true
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == 0) {
            isDragging = false
            return true
        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer == 0 && isDragging) {
            val deltaX = screenX - lastTouch.x
            val deltaY = screenY - lastTouch.y
            
            angleX -= deltaX * sensitivity
            angleY -= deltaY * sensitivity
            
            // Clamp vertical angle to avoid gimbal lock or flipping
            if (angleY > 89f) angleY = 89f
            if (angleY < -89f) angleY = -89f
            
            lastTouch.set(screenX.toFloat(), screenY.toFloat(), 0f)
            updateCamera()
            return true
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        distance += amountY * zoomSensitivity
        if (distance < minZoom) distance = minZoom
        if (distance > maxZoom) distance = maxZoom
        updateCamera()
        return true
    }
    
    // Simple pinch to zoom implementation could be added here for touch screens
    // For now, we rely on drag for rotation. 
    // Implementing full multi-touch gesture detector would be better for zoom.

    fun updateCamera() {
        val hDist = (distance * Math.cos(Math.toRadians(angleY.toDouble()))).toFloat()
        val vDist = (distance * Math.sin(Math.toRadians(angleY.toDouble()))).toFloat()
        
        val x = (hDist * Math.sin(Math.toRadians(angleX.toDouble()))).toFloat()
        val z = (hDist * Math.cos(Math.toRadians(angleX.toDouble()))).toFloat()
        
        camera.position.set(x, vDist, z)
        camera.lookAt(0f, 0f, 0f)
        camera.update()
    }
}
