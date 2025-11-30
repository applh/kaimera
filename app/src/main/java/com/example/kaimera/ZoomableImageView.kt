package com.example.kaimera

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener {

    private var scaleDetector: ScaleGestureDetector
    private var gestureDetector: GestureDetector
    private var matrixCurrent = Matrix()
    private var matrixValues = FloatArray(9)
    
    // Zoom constraints
    private var minScale = 1f
    private var maxScale = 5f
    private var saveScale = 1f
    
    // View dimensions
    private var viewWidth = 0
    private var viewHeight = 0
    private var contentWidth = 0f
    private var contentHeight = 0f
    
    // State
    private var isInit = false
    private var isZooming = false

    init {
        scaleType = ScaleType.MATRIX
        scaleDetector = ScaleGestureDetector(context, this)
        gestureDetector = GestureDetector(context, this)
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        isInit = false
        saveScale = 1f
        matrixCurrent.reset()
        imageMatrix = matrixCurrent
        
        if (viewWidth > 0 && viewHeight > 0) {
            fitToScreen()
            isInit = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        
        if (!isInit && drawable != null && viewWidth > 0 && viewHeight > 0) {
            fitToScreen()
            isInit = true
        }
    }

    private fun fitToScreen() {
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val scaleX = viewWidth.toFloat() / drawableWidth
        val scaleY = viewHeight.toFloat() / drawableHeight
        val scale = scaleX.coerceAtMost(scaleY)

        matrixCurrent.setScale(scale, scale)
        
        // Center the image
        val redundantYSpace = viewHeight.toFloat() - (scale * drawableHeight)
        val redundantXSpace = viewWidth.toFloat() - (scale * drawableWidth)
        
        matrixCurrent.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
        
        contentWidth = viewWidth.toFloat() - redundantXSpace
        contentHeight = viewHeight.toFloat() - redundantYSpace
        
        imageMatrix = matrixCurrent
        saveScale = 1f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        val pointCount = event.pointerCount
        if (pointCount > 1) {
            isZooming = true
        } else if (event.action == MotionEvent.ACTION_UP) {
            isZooming = false
        }
        
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var scaleFactor = detector.scaleFactor
        val origScale = saveScale
        saveScale *= scaleFactor
        
        if (saveScale > maxScale) {
            saveScale = maxScale
            scaleFactor = maxScale / origScale
        } else if (saveScale < minScale) {
            saveScale = minScale
            scaleFactor = minScale / origScale
        }

        if (contentWidth * saveScale <= viewWidth || contentHeight * saveScale <= viewHeight) {
            matrixCurrent.postScale(scaleFactor, scaleFactor, viewWidth / 2f, viewHeight / 2f)
        } else {
            matrixCurrent.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
        }
        
        fixTranslation()
        imageMatrix = matrixCurrent
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
    }

    private fun fixTranslation() {
        matrixCurrent.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        
        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), contentWidth * saveScale)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), contentHeight * saveScale)

        if (fixTransX != 0f || fixTransY != 0f) {
            matrixCurrent.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!isZooming) { // Only scroll if not currently multi-touch zooming
             // Check if we can scroll
            matrixCurrent.getValues(matrixValues)
            // transX, transY, scaleX, scaleY were here but unused variables removed
            
            if (drawable == null) return false
            // width and height calculations removed as they were unused
            
            // Only allow scrolling if the image is larger than the view
            var scrollX = -distanceX
            var scrollY = -distanceY
            
            // Logic to restrict scrolling within bounds
            // (Simplified: just postTranslate and then fixTranslation handles bounds)
            matrixCurrent.postTranslate(scrollX, scrollY)
            fixTranslation()
            imageMatrix = matrixCurrent
        }
        return true
    }

    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
    
    fun setMaxZoom(maxZoom: Float) {
        maxScale = maxZoom
    }
}
