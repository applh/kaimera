package com.example.kaimera.sphereqix

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray

class PlayerController(private val camera: Camera, private val sphereRadius: Float) {

    val position = Vector3(0f, 0f, sphereRadius) // Start at "front" (Home Area)
    private val tmp = Vector3()
    private val shapeRenderer = ShapeRenderer()
    
    // Player rotation angle (in degrees)
    var rotationAngle = 0f
    
    // Line drawing
    var isDrawing = false
    val currentLine = ArrayList<Vector3>()
    private val minDistance = 0.1f // Min distance between points to add to line

    fun rotateDirections(angleDelta: Float) {
        rotationAngle += angleDelta
        // Keep angle in 0-360 range
        while (rotationAngle < 0f) rotationAngle += 360f
        while (rotationAngle >= 360f) rotationAngle -= 360f
    }

    fun update(delta: Float, joyX: Float, joyY: Float, sphereMesh: SphereMesh) {
        // Move player based on joystick
        if (Math.abs(joyX) > 0.1f || Math.abs(joyY) > 0.1f) {
            val speed = 2.0f * delta // Movement speed
            
            // Calculate movement direction relative to camera
            val camDir = camera.direction.cpy()
            val camUp = camera.up.cpy()
            val camRight = camDir.cpy().crs(camUp).nor()
            
            // Apply player rotation to the input
            val angleRad = Math.toRadians(rotationAngle.toDouble()).toFloat()
            val cosAngle = Math.cos(angleRad.toDouble()).toFloat()
            val sinAngle = Math.sin(angleRad.toDouble()).toFloat()
            
            // Rotate input vector
            val rotatedX = joyX * cosAngle - joyY * sinAngle
            val rotatedY = joyX * sinAngle + joyY * cosAngle
            
            // Rotate position around camera Right (for Y movement) and Up (for X movement)
            position.rotate(camRight, rotatedY * speed * 50f)
            position.rotate(camUp, -rotatedX * speed * 50f)
            
            position.nor().scl(sphereRadius)
            
            // Start drawing if moving
            startDrawing()
        } else {
            // Stop drawing if joystick released
            stopDrawing(sphereMesh)
        }
        
        // Add current position to line if drawing
        if (isDrawing) {
            if (currentLine.isEmpty() || currentLine.last().dst(position) > 0.1f) {
                currentLine.add(position.cpy())
            }
        }
    }
    
    // Deprecated old update
    fun update(delta: Float) {}

    fun handleInput(screenX: Int, screenY: Int) {
        val ray: Ray = camera.getPickRay(screenX.toFloat(), screenY.toFloat())
        val intersection = Vector3()
        
        // Intersect ray with sphere
        if (Intersector.intersectRaySphere(ray, Vector3.Zero, sphereRadius, intersection)) {
            // Move cursor to intersection point
            position.set(intersection)
            
            // The line drawing logic is now primarily handled in the update method
            // when isDrawing is true, ensuring consistency with joystick movement.
            // This block is removed to avoid duplicate point additions.
        }
    }
    
    fun startDrawing() {
        isDrawing = true
        currentLine.clear()
        currentLine.add(position.cpy())
    }
    
    fun stopDrawing(sphereMesh: SphereMesh) {
        isDrawing = false
        // Trigger capture logic
        if (currentLine.size > 2) {
            sphereMesh.checkCapture(currentLine)
        }
    }

    fun render(camera: Camera) {
        shapeRenderer.projectionMatrix = camera.combined
        
        // Draw directional arrows around player
        val arrowDistance = 0.5f // Increased for better visibility
        val arrowSize = 0.12f
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        // Calculate camera-relative directions
        val camRight = camera.direction.cpy().crs(camera.up).nor()
        val camUp = camera.up.cpy().nor()
        
        // Apply player rotation to arrow directions
        val angleRad = Math.toRadians(rotationAngle.toDouble()).toFloat()
        val cosAngle = Math.cos(angleRad.toDouble()).toFloat()
        val sinAngle = Math.sin(angleRad.toDouble()).toFloat()
        
        // Rotated directions for arrows
        val upDirRotated = camUp.cpy().scl(cosAngle).add(camRight.cpy().scl(-sinAngle))
        val downDirRotated = upDirRotated.cpy().scl(-1f)
        val rightDirRotated = camRight.cpy().scl(cosAngle).add(camUp.cpy().scl(sinAngle))
        val leftDirRotated = rightDirRotated.cpy().scl(-1f)
        
        // Down arrow (Magenta - matches down button)
        val upPos = position.cpy().add(upDirRotated.cpy().scl(arrowDistance))
        upPos.nor().scl(sphereRadius) // Project onto sphere surface
        val upDir = position.cpy().sub(upPos).nor() // Inverted: point from arrow to player
        shapeRenderer.color = Color.MAGENTA
        drawArrow(upPos, upDir, arrowSize)
        
        // Up arrow (Cyan - matches up button)
        val downPos = position.cpy().add(downDirRotated.cpy().scl(arrowDistance))
        downPos.nor().scl(sphereRadius) // Project onto sphere surface
        val downDir = position.cpy().sub(downPos).nor() // Inverted: point from arrow to player
        shapeRenderer.color = Color.CYAN
        drawArrow(downPos, downDir, arrowSize)
        
        // Right arrow (Red - matches right button)
        val leftPos = position.cpy().add(leftDirRotated.cpy().scl(arrowDistance))
        leftPos.nor().scl(sphereRadius) // Project onto sphere surface
        val leftDir = position.cpy().sub(leftPos).nor() // Inverted: point from arrow to player
        shapeRenderer.color = Color.RED
        drawArrow(leftPos, leftDir, arrowSize)
        
        // Left arrow (Green - matches left button)
        val rightPos = position.cpy().add(rightDirRotated.cpy().scl(arrowDistance))
        rightPos.nor().scl(sphereRadius) // Project onto sphere surface
        val rightDir = position.cpy().sub(rightPos).nor() // Inverted: point from arrow to player
        shapeRenderer.color = Color.GREEN
        drawArrow(rightPos, rightDir, arrowSize)
        
        // Draw player cursor (white)
        shapeRenderer.color = Color.WHITE
        shapeRenderer.translate(position.x, position.y, position.z)
        shapeRenderer.box(-0.05f, -0.05f, -0.05f, 0.1f, 0.1f, 0.1f)
        shapeRenderer.translate(-position.x, -position.y, -position.z)
        
        shapeRenderer.end()
        
        // Draw line if drawing
        if (isDrawing && currentLine.size > 1) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color = Color.YELLOW
            for (i in 0 until currentLine.size - 1) {
                val p1 = currentLine[i]
                val p2 = currentLine[i + 1]
                shapeRenderer.line(p1, p2)
            }
            // Draw line to current cursor pos
            shapeRenderer.line(currentLine.last(), position)
            shapeRenderer.end()
        }
    }
    
    private fun drawArrow(pos: Vector3, direction: Vector3, size: Float) {
        // Draw arrow as lines forming a triangle shape
        val perpendicular = direction.cpy().crs(Vector3(0f, 1f, 0f)).nor()
        if (perpendicular.len() < 0.1f) {
            perpendicular.set(direction.cpy().crs(Vector3(1f, 0f, 0f)).nor())
        }
        
        val tip = pos.cpy().add(direction.cpy().scl(size))
        val base1 = pos.cpy().add(perpendicular.cpy().scl(size * 0.5f))
        val base2 = pos.cpy().sub(perpendicular.cpy().scl(size * 0.5f))
        
        // Draw filled triangle using box as approximation
        shapeRenderer.translate(tip.x, tip.y, tip.z)
        shapeRenderer.box(-size * 0.2f, -size * 0.2f, -size * 0.2f, size * 0.4f, size * 0.4f, size * 0.4f)
        shapeRenderer.translate(-tip.x, -tip.y, -tip.z)
    }
    
    fun dispose() {
        shapeRenderer.dispose()
    }
}
