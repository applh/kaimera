package com.example.kaimera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
    
    private val levelThreshold = 5f  // Degrees to consider "level" (increased for easier achievement)
    
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFFFFFFFF.toInt()
    }
    
    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x80FFFFFF.toInt()
    }
    
    private val bubbleRadius = 20f
    private val trackWidth = 200f
    private val trackHeight = 40f
    
    fun updateTilt(newPitch: Float, newRoll: Float) {
        pitch = newPitch
        roll = newRoll
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Determine if device is level
        val isLevel = abs(pitch) < levelThreshold && abs(roll) < levelThreshold
        bubblePaint.color = if (isLevel) 0xFF00FF00.toInt() else 0xFFFF0000.toInt()
        
        // Draw horizontal level (roll)
        val horizontalTrack = RectF(
            centerX - trackWidth / 2,
            centerY - trackHeight / 2 - 30,
            centerX + trackWidth / 2,
            centerY + trackHeight / 2 - 30
        )
        canvas.drawRoundRect(horizontalTrack, trackHeight / 2, trackHeight / 2, trackPaint)
        
        // Draw center line for horizontal
        canvas.drawLine(
            centerX,
            horizontalTrack.top,
            centerX,
            horizontalTrack.bottom,
            centerLinePaint
        )
        
        // Draw horizontal bubble (constrained to track)
        val horizontalBubbleX = centerX + (roll * 5).coerceIn(-trackWidth / 2 + bubbleRadius, trackWidth / 2 - bubbleRadius)
        canvas.drawCircle(horizontalBubbleX, centerY - 30, bubbleRadius, bubblePaint)
        
        // Draw vertical level (pitch)
        val verticalTrack = RectF(
            centerX - trackHeight / 2,
            centerY - trackWidth / 2 + 30,
            centerX + trackHeight / 2,
            centerY + trackWidth / 2 + 30
        )
        canvas.drawRoundRect(verticalTrack, trackHeight / 2, trackHeight / 2, trackPaint)
        
        // Draw center line for vertical
        canvas.drawLine(
            verticalTrack.left,
            centerY + 30,
            verticalTrack.right,
            centerY + 30,
            centerLinePaint
        )
        
        // Draw vertical bubble (constrained to track)
        val verticalBubbleY = centerY + 30 + (pitch * 5).coerceIn(-trackWidth / 2 + bubbleRadius, trackWidth / 2 - bubbleRadius)
        canvas.drawCircle(centerX, verticalBubbleY, bubbleRadius, bubblePaint)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (trackWidth + 100).toInt()
        val desiredHeight = (trackWidth + 100).toInt()
        setMeasuredDimension(desiredWidth, desiredHeight)
    }
}
