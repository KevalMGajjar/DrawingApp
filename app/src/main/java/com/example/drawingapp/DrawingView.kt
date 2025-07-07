package com.example.drawingapp

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.scale

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var drawPath: FingerPath
    private lateinit var drawPaint: Paint
    private lateinit var canvasPaint: Paint
    private lateinit var canvasBitMap: Bitmap
    private lateinit var canvas: Canvas
    private var brushSize: Float = 0f
    private var color = Color.BLACK

    private val paths = mutableListOf<FingerPath>()
    private var imageBitmap: Bitmap? = null

    init {
        setUpDrawing()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitMap = createBitmap(w, h)
        canvas = Canvas(canvasBitMap)
        canvas.drawColor(Color.WHITE)

        imageBitmap?.let {
            imageBitmap = it.scale(w, h)
            canvas.drawBitmap(imageBitmap!!, 0f, 0f, null)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background (image already drawn on canvasBitMap)
        canvas.drawBitmap(canvasBitMap, 0f, 0f, null)

        // Draw each path from memory
        for (path in paths) {
            drawPaint.strokeWidth = path.brushThickness
            drawPaint.color = path.color
            canvas.drawPath(path, drawPaint)
        }

        // Draw the current in-progress path
        if (!drawPath.isEmpty) {
            drawPaint.strokeWidth = drawPath.brushThickness
            drawPaint.color = drawPath.color
            canvas.drawPath(drawPath, drawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x ?: return false
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.color = color
                drawPath.brushThickness = brushSize
                drawPath.reset()
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(drawPath)
                drawPath = FingerPath(color, brushSize)
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun changeBrushSize(newSize: Float) {
        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics
        )
        drawPaint.strokeWidth = brushSize
    }

    fun changeBrushColor(newColor: Any) {
        color = if (newColor is String) {
            newColor.toColorInt()
        } else {
            newColor as Int
        }
    }

    fun undoPath() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            invalidate()
        }
    }

    fun getBitMap(): Pair<Bitmap, MutableList<FingerPath>> {
        val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(finalBitmap)

        // Draw background image (if exists)
        imageBitmap?.let {
            tempCanvas.drawBitmap(it, 0f, 0f, null)
        } ?: run {
            tempCanvas.drawColor(Color.WHITE) // Fallback if no background image
        }

        // Draw saved paths
        for (path in paths) {
            drawPaint.strokeWidth = path.brushThickness
            drawPaint.color = path.color
            tempCanvas.drawPath(path, drawPaint)
        }

        return Pair(finalBitmap, paths)
    }


    fun setCanvasBackground(uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val loadedBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (loadedBitmap != null) {
            if (width == 0 || height == 0) {
                post { setCanvasBackground(uri) }
                return
            }

            // Step 1: Clear existing paths and reset the current path
            paths.clear()
            drawPath.reset()

            // Step 2: Clear the canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            // Step 3: Scale and set the background image
            imageBitmap = Bitmap.createScaledBitmap(loadedBitmap, width, height, true)

            // Step 4: Draw background image onto the canvas
            canvas.drawBitmap(imageBitmap!!, 0f, 0f, null)

            // Step 5: Redraw the view
            invalidate()
        }
    }


    private fun setUpDrawing() {
        drawPath = FingerPath(color, brushSize)
        drawPaint = Paint().apply {
            color = this@DrawingView.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = brushSize
        }

        canvasPaint = Paint(Paint.DITHER_FLAG)
        brushSize = 20f
    }

    inner class FingerPath(var color: Int, var brushThickness: Float) : Path()
}
