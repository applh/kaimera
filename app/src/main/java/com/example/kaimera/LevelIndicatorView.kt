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

    private var angle = 0f
    
    private var levelThreshold = 5f  // Degrees to consider "level" (configurable)
    
    fun setThreshold(threshold: Float) {
        levelThreshold = threshold
    }
    
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x80FFFFFF.toInt()  // Semi-transparent white
    }
    
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFF00FF00.toInt()  // Green
    }
    
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }
    
    private var crosshairSizePercentage = 10 // Default 10%
    
    fun setCrosshairSizePercentage(percentage: Int) {
        crosshairSizePercentage = percentage.coerceIn(0, 100)
        invalidate()
    }
    
    private var circleSizePercentage = 10 // Default 10%
    
    fun setCircleSizePercentage(percentage: Int) {
        circleSizePercentage = percentage.coerceIn(0, 100)
        invalidate()
    }
    
    fun updateAngle(newAngle: Float) {
        angle = newAngle
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Calculate crosshair length based on percentage of max dimension
        // 100% means the full length matches the max dimension
        // So radius (length from center) is maxDimension * percentage / 100 / 2
        val maxDimension = kotlin.math.max(width, height).toFloat()
        val crosshairLength = (maxDimension * crosshairSizePercentage / 100f) / 2f
        
        // Draw rotated crosshair (always at horizon level)
        canvas.save()
        canvas.rotate(angle, centerX, centerY)
        
        // Horizontal line
        canvas.drawLine(
            centerX - crosshairLength,
            centerY,
            centerX + crosshairLength,
            centerY,
            crosshairPaint
        )
        
        // Vertical line
        canvas.drawLine(
            centerX,
            centerY - crosshairLength,
            centerX,
            centerY + crosshairLength,
            crosshairPaint
        )
        
        canvas.restore()
        
        // Draw center dot
        canvas.drawCircle(centerX, centerY, 3f, centerDotPaint)
        
        // Draw green circle every 90 degrees
        val normalized = abs(angle % 90)
        val isLevel = normalized < levelThreshold || (90 - normalized) < levelThreshold
        
        if (isLevel) {
            val circleRadius = (maxDimension * circleSizePercentage / 100f) / 2f
            canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)
        }
    }
}
