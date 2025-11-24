package com.example.kamerai

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Vertical lines
        val x1 = width / 3
        val x2 = width * 2 / 3
        canvas.drawLine(x1, 0f, x1, height, paint)
        canvas.drawLine(x2, 0f, x2, height, paint)

        // Horizontal lines
        val y1 = height / 3
        val y2 = height * 2 / 3
        canvas.drawLine(0f, y1, width, y1, paint)
        canvas.drawLine(0f, y2, width, y2, paint)
    }
}
