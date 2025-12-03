package com.example.kaimera.core.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout

class ZoomableVideoLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {

    private var scaleDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    
    private var mode = Mode.NONE
    private var scale = 1.0f
    private var lastScaleFactor = 0f

    // Zoom constraints
    private var minScale = 1.0f
    private var maxScale = 5.0f

    // Panning
    private var startX = 0f
    private var startY = 0f
    private var translateX = 0f
    private var translateY = 0f
    private var previousTranslateX = 0f
    private var previousTranslateY = 0f

    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    init {
        scaleDetector = ScaleGestureDetector(context, this)
        gestureDetector = GestureDetector(context, this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mode = Mode.DRAG
                startX = event.x - previousTranslateX
                startY = event.y - previousTranslateY
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && scale > 1f) { // Only drag if zoomed in
                    translateX = event.x - startX
                    translateY = event.y - startY
                    
                    // Apply translation with bounds checking
                    applyTranslation()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.ZOOM
            MotionEvent.ACTION_UP -> {
                mode = Mode.NONE
                previousTranslateX = translateX
                previousTranslateY = translateY
            }
            MotionEvent.ACTION_POINTER_UP -> mode = Mode.DRAG
        }

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        if (mode == Mode.DRAG && scale > 1f || mode == Mode.ZOOM) {
             parent.requestDisallowInterceptTouchEvent(true)
        }

        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val scaleFactor = detector.scaleFactor
        scale *= scaleFactor
        
        if (scale < minScale) {
            scale = minScale
        } else if (scale > maxScale) {
            scale = maxScale
        }

        if (childCount > 0) {
            val child = getChildAt(0)
            child.scaleX = scale
            child.scaleY = scale
            
            // Adjust translation to keep focus point stable or center
            // For simplicity, we just clamp translation after scaling
            applyTranslation()
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        mode = Mode.ZOOM
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        // No-op
    }

    private fun applyTranslation() {
        if (childCount > 0) {
            val child = getChildAt(0)
            
            // Calculate bounds
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val contentWidth = viewWidth * scale
            val contentHeight = viewHeight * scale
            
            val maxDx = (contentWidth - viewWidth) / 2
            val maxDy = (contentHeight - viewHeight) / 2
            
            // Clamp translation
            translateX = translateX.coerceIn(-maxDx, maxDx)
            translateY = translateY.coerceIn(-maxDy, maxDy)
            
            child.translationX = translateX
            child.translationY = translateY
        }
    }

    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false // Let child handle clicks if needed
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
    
    // Reset zoom state
    fun resetZoom() {
        scale = 1.0f
        translateX = 0f
        translateY = 0f
        previousTranslateX = 0f
        previousTranslateY = 0f
        if (childCount > 0) {
            val child = getChildAt(0)
            child.scaleX = 1f
            child.scaleY = 1f
            child.translationX = 0f
            child.translationY = 0f
        }
    }
    
    fun setMaxZoom(maxZoom: Float) {
        maxScale = maxZoom
    }
}
