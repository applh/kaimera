package com.example.kaimera.sphereqix

import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import net.mgsx.gltf.scene3d.scene.Scene

class Character(val scene: Scene, val radius: Float) {
    var heading: Float = 0f // Radians, direction relative to North (up)

    val position = Vector3()
    val up = Vector3()
    val forward = Vector3()
    val right = Vector3()
    
    private val transform = Matrix4()
    
    // Animation
    var animationController: AnimationController = scene.animationController
    private var currentAnimation = "Idle"
    private var speed = 0f
    
    var heightOffset: Float = 0f
    
    private var targetPosition: Vector3? = null
    private val tempVec = Vector3()
    
    fun hasTarget(): Boolean = targetPosition != null
    fun getTarget(): Vector3? = targetPosition
    
    init {
        // Initialize basis at starting position (0,0,radius)
        position.set(0f, 0f, radius)
        up.set(0f, 0f, 1f) // Z is up at (0,0,1)
        forward.set(0f, 1f, 0f) // Facing Y (North-ish)
        right.set(-1f, 0f, 0f) // Right is -X? (0,0,1)x(0,1,0) = (-1,0,0)
        
        updateTransform()
        loopAnimation("Idle")
    }

    fun update(delta: Float) {
        // 1. Steering Logic
        if (targetPosition != null) {
            val dist = position.dst(targetPosition!!)
            
            if (dist < 0.2f) { // Reached
                speed = 0f
                loopAnimation("Idle")
                targetPosition = null
            } else {
                // Arrival Behavior: Slow down when close to reduce turn radius
                // Turn Radius = speed / turnSpeed.
                // We need TurnRadius < dist to avoid circling.
                // So speed < dist * turnSpeed.
                // with turnSpeed = 5.0, at dist 0.2, max speed = 1.0. 
                // Currently speed is 2.0, causing overshoot/circling.
                val slowRadius = 1.0f
                var desiredSpeed = 2f
                if (dist < slowRadius) {
                    desiredSpeed = 2f * (dist / slowRadius)
                    // Ensure we don't stop completely before reaching
                    if (desiredSpeed < 0.5f) desiredSpeed = 0.5f
                }
                speed = desiredSpeed
                
                loopAnimation("Walking")

                // Desired direction (Tangent)
                tempVec.set(targetPosition).sub(position).nor() // Chord
                // Project to Tangent
                val dot = tempVec.dot(up)
                tempVec.mulAdd(up, -dot).nor()
                
                if (!tempVec.isZero) {
                    // Angle between Forward and Desired
                    val dotFwd = forward.dot(tempVec)
                    val angle = Math.acos(dotFwd.coerceIn(-1f, 1f).toDouble()).toFloat()
                    
                    // Sign: (Forward x Desired) . Up
                    val cross = forward.cpy().crs(tempVec)
                    val sign = if (cross.dot(up) > 0) 1f else -1f
                    
                    val turnSpeed = 5f * delta
                    val rotStep = if (Math.abs(angle) > turnSpeed) turnSpeed * sign else angle * sign
                    
                    // Rotate Forward and Right around Up
                    val steerRot = Quaternion().setFromAxis(up, rotStep * 57.2958f)
                    forward.mul(steerRot).nor()
                    right.mul(steerRot).nor()
                }
            }
        }

        // 2. Movement Logic (Parallel Transport)
        if (speed > 0) {
             val angularSpeed = speed / radius
             val moveAngleDeg = angularSpeed * delta * 57.2958f
             
             // Move "Forward": Rotate around "Right" axis?? 
             // If Forward=(0,1,0) and Up=(0,0,1), Right=(-1,0,0).
             // To move +Y (Forward), we rotate around +X? 
             // Right is -X. So rotate around -Right (-(-X) = +X).
             // Or rotate around Right with Negative angle?
             // Let's use `Left = Up x Forward`.
             // Rotation axis = Left.
             // Positive rotation around Left pitches "Up" towards "Forward".
             
             val left = up.cpy().crs(forward).nor()
             val moveRot = Quaternion().setFromAxis(left, moveAngleDeg)
             
             // Apply to ALL basis vectors to transport the frame
             position.mul(moveRot)
             up.mul(moveRot)
             forward.mul(moveRot)
             right.mul(moveRot)
             
             // Re-normalize interactions to prevent drift
             position.nor().scl(radius)
             up.set(position).nor()
             
             // Gram-Schmidt to keep Forward tangent
             // Forward = Forward - (Forward . Up) * Up
             val fDot = forward.dot(up)
             forward.mulAdd(up, -fDot).nor()
             
             // Recompute Right
             right.set(up).crs(forward).nor()
        }

        // Update Animation
        animationController.update(delta)
        
        updateTransform()
    }

    fun moveTo(target: Vector3) {
        if (targetPosition == null) {
            targetPosition = Vector3()
        }
        targetPosition?.set(target)
    }
    
    fun setSpeed(s: Float) {
        this.speed = s
        if (speed > 0 && currentAnimation != "Walking") {
            loopAnimation("Walking")
        } else if (speed == 0f && currentAnimation != "Idle") {
            loopAnimation("Idle")
        }
    }
    
    // fun turn() removed - no longer directly modifying heading float
    
    private fun loopAnimation(id: String) {
        if (currentAnimation == id) return
        try {
            animationController.setAnimation(id, -1)
            currentAnimation = id
        } catch (e: Exception) {
        }
    }

    // updateVectors() removed - using incremental

    private fun updateTransform() {
        if (position.isZero) {
             // Fallback
             position.set(0f, 0f, radius)
             up.set(0f, 0f, 1f)
             forward.set(0f, 1f, 0f)
             right.set(-1f, 0f, 0f)
        }
        
        // Matrix construction from basis
        transform.idt()
        // Columns: Right, Up, Forward? 
        // LibGDX/OpenGL: Forward is usually -Z.
        // If our 'forward' vector is looking at target, and model's forward is -Z (or +Z), 
        // we need to map our basis to model basis.
        // Assuming Model Forward is +Z?? GLTF usually +Z is front? No, +Z is Back. -Z is Front.
        // Let's assume Model Forward is -Z.
        // We want Model(-Z) to align with our `forward`.
        // So Transform Z column = -forward.
        
        // Model Up (+Y) = our `up`.
        // Model Right (+X) = our `right`.
        
        // Col 0: Right (Rotated 180 -> -Right)
        transform.`val`[Matrix4.M00] = -right.x
        transform.`val`[Matrix4.M10] = -right.y
        transform.`val`[Matrix4.M20] = -right.z
        
        // Col 1: Up (Unchanged)
        transform.`val`[Matrix4.M01] = up.x
        transform.`val`[Matrix4.M11] = up.y
        transform.`val`[Matrix4.M21] = up.z
        
        // Col 2: Backward (Rotated 180 -> -Backward = Forward)
        // Previous: Backward = -Forward
        // New: Backward = Forward
        val backward = forward.cpy() // Was scl(-1f)
        transform.`val`[Matrix4.M02] = backward.x
        transform.`val`[Matrix4.M12] = backward.y
        transform.`val`[Matrix4.M22] = backward.z
        
        // Col 3: Position
        transform.`val`[Matrix4.M03] = position.x
        transform.`val`[Matrix4.M13] = position.y
        transform.`val`[Matrix4.M23] = position.z
        
        // Scale
        transform.scale(0.05f, 0.05f, 0.05f) 
        
        scene.modelInstance.transform.set(transform)
    }
}
