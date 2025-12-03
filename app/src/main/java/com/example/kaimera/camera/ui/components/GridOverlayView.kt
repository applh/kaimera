package com.example.kaimera.camera.ui.components

import com.example.kaimera.R

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        alpha = 150 // Semi-transparent
        style = Paint.Style.STROKE
    }
    
    private var rows = 3
    private var columns = 3
    
    fun setGridSize(rows: Int, columns: Int) {
        this.rows = rows.coerceIn(1, 10)
        this.columns = columns.coerceIn(1, 10)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw vertical lines
        for (i in 1 until columns) {
            val x = width * i / columns
            canvas.drawLine(x, 0f, x, height, paint)
        }

        // Draw horizontal lines
        for (i in 1 until rows) {
            val y = height * i / rows
            canvas.drawLine(0f, y, width, y, paint)
        }
    }
}
