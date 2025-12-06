package com.example.kaimera.sphereqix

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class InputController(private val camera: Camera) : InputAdapter(), GestureDetector.GestureListener {

    private val lastTouch = Vector3()
    private var isDragging = false
    
    // Orbit parameters
    private var angleX = 0f
    private var angleY = 0f
    private val sensitivity = 0.5f
    // Zoom range
    private val minZoom = 2.5f
    private val maxZoom = 20f
    // Distance from center
    private var distance = 5f
    
    private val gestureDetector = GestureDetector(this)

    private var isPinching = false

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        // Forward to GestureDetector
        gestureDetector.touchDown(screenX.toFloat(), screenY.toFloat(), pointer, button)
        
        if (pointer == 0) {
            lastTouch.set(screenX.toFloat(), screenY.toFloat(), 0f)
            isDragging = true
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        gestureDetector.touchUp(screenX.toFloat(), screenY.toFloat(), pointer, button)
        if (pointer == 0) {
            isDragging = false
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        gestureDetector.touchDragged(screenX.toFloat(), screenY.toFloat(), pointer)
        
        // Manual rotation logic (only if not pinching)
        if (pointer == 0 && isDragging && !isPinching) {
            val deltaX = screenX - lastTouch.x
            val deltaY = screenY - lastTouch.y
            
            angleX -= deltaX * sensitivity
            angleY += deltaY * sensitivity
            
            // Wrap angles
            if (angleX > 360) angleX -= 360
            if (angleX < 0) angleX += 360
            
            if (angleY > 360) angleY -= 360
            if (angleY < 0) angleY += 360
            
            lastTouch.set(screenX.toFloat(), screenY.toFloat(), 0f)
            updateCamera()
            return true
        }
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        applyZoom(amountY * 0.5f)
        return true
    }
    
    // Private helper (renamed to avoid conflict)
    private fun applyZoom(amount: Float) {
        distance += amount
        if (distance < minZoom) distance = minZoom
        if (distance > maxZoom) distance = maxZoom
        updateCamera()
    }
    
    // --- GestureListener Implementation ---
    
    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        return false // InputProcessor handles this mostly, but this is the Listener callback
    }

    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean = false
    override fun longPress(x: Float, y: Float): Boolean = false
    override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean = false
    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = false
    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

    private var lastGestureDist = -1f

    override fun zoom(initialDistance: Float, distance: Float): Boolean {
        if (lastGestureDist == -1f) {
            lastGestureDist = initialDistance
        }
        
        // Calculate scale factor
        val scale = lastGestureDist / distance
        
        // Apply to camera distance
        this.distance *= scale
        
        // Clamp
        if (this.distance < minZoom) this.distance = minZoom
        if (this.distance > maxZoom) this.distance = maxZoom
        
        updateCamera()
        
        lastGestureDist = distance
        // We consider zooming part of pinching usually, or at least a multi-touch blocking event
        isPinching = true 
        return true
    }

    override fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean {
        isPinching = true
        return true
    }

    override fun pinchStop() {
        lastGestureDist = -1f
        isPinching = false
    }

    fun updateCamera() {
        val hDist = (distance * Math.cos(Math.toRadians(angleY.toDouble()))).toFloat()
        val vDist = (distance * Math.sin(Math.toRadians(angleY.toDouble()))).toFloat()
        
        val x = (hDist * Math.sin(Math.toRadians(angleX.toDouble()))).toFloat()
        val z = (hDist * Math.cos(Math.toRadians(angleX.toDouble()))).toFloat()
        
        camera.position.set(x, vDist, z)
        
        if (Math.cos(Math.toRadians(angleY.toDouble())) < 0) {
            camera.up.set(0f, -1f, 0f)
        } else {
            camera.up.set(0f, 1f, 0f)
        }
        
        camera.lookAt(0f, 0f, 0f)
        camera.update()
    }
}
