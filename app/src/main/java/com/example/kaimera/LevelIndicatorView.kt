package com.example.kaimera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class LevelIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pitch = 0f  // Forward/backward tilt (vertical)
    private var roll = 0f   // Left/right tilt (horizontal)
    
    private var levelThreshold = 5f  // Degrees to consider "level" (configurable)
    
    fun setThreshold(threshold: Float) {
        levelThreshold = threshold
    }
    
    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0xFFFFFFFF.toInt()
    }
    
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFF00FF00.toInt()
    }
    
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }
    
    private val crosshairLength = 150f
    private val circleRadius = 60f
    
    fun updateTilt(newPitch: Float, newRoll: Float) {
        pitch = newPitch
        roll = newRoll
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Save canvas state
        canvas.save()
        
        // Rotate canvas based on roll (horizon tilt)
        canvas.rotate(-roll, centerX, centerY)
        
        // Draw horizontal horizon line
        canvas.drawLine(
            centerX - crosshairLength,
            centerY,
            centerX + crosshairLength,
            centerY,
            horizonPaint
        )
        
        // Draw vertical line (crosshair)
        canvas.drawLine(
            centerX,
            centerY - crosshairLength,
            centerX,
            centerY + crosshairLength,
            horizonPaint
        )
        
        // Restore canvas
        canvas.restore()
        
        // Draw center dot
        canvas.drawCircle(centerX, centerY, 4f, centerDotPaint)
        
        // Draw green circle when device is level
        // Device is level when roll is close to 0 (not tilted left/right)
        val isLevel = abs(roll) < levelThreshold
        if (isLevel) {
            canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = ((crosshairLength * 2) + 100).toInt()
        setMeasuredDimension(desiredSize, desiredSize)
    }
}
