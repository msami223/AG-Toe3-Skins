package com.devstormtech.toe3skins

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Canvas state
    val elements = mutableListOf<CanvasElement>()
    var baseBitmap: Bitmap? = null
    var baseColor: Int? = null
    var selectedElement: CanvasElement? = null

    // Aspect ratio preservation
    private var baseImageRect = RectF()
    private var baseImageScale = 1f
    private var baseImageOffsetX = 0f
    private var baseImageOffsetY = 0f

    // Touch state
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialDistance = 0f
    private var initialRotation = 0f
    private var isMultiTouch = false
    private var isDragging = false
    private var isResizingFromHandle = false
    private var activeHandle: ResizeHandle? = null

    // Handle sizes
    private val HANDLE_SIZE = 80f
    private val TRASH_HANDLE_SIZE = 100f

    // Callbacks
    var onElementDeleted: ((CanvasElement) -> Unit)? = null

    enum class ResizeHandle {
        // Corners - proportional resize
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        // Sides - directional resize (non-proportional!)
        TOP, BOTTOM, LEFT, RIGHT
    }

    // Paint objects (reused for performance)
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#BB86FC")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(30f, 15f), 0f)
    }

    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleStrokePaint = Paint().apply {
        color = Color.parseColor("#BB86FC")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val trashPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val trashIconPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    init {
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBaseImageRect()
    }

    private fun calculateBaseImageRect() {
        baseBitmap?.let { bitmap ->
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()
            val bitmapAspect = bitmapWidth / bitmapHeight

            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val viewAspect = viewWidth / viewHeight

            // Fit image maintaining aspect ratio
            if (bitmapAspect > viewAspect) {
                // Bitmap is wider - fit to width
                baseImageScale = viewWidth / bitmapWidth
                val scaledHeight = bitmapHeight * baseImageScale
                baseImageOffsetX = 0f
                baseImageOffsetY = (viewHeight - scaledHeight) / 2f
            } else {
                // Bitmap is taller - fit to height
                baseImageScale = viewHeight / bitmapHeight
                val scaledWidth = bitmapWidth * baseImageScale
                baseImageOffsetX = (viewWidth - scaledWidth) / 2f
                baseImageOffsetY = 0f
            }

            baseImageRect = RectF(
                baseImageOffsetX,
                baseImageOffsetY,
                baseImageOffsetX + (bitmapWidth * baseImageScale),
                baseImageOffsetY + (bitmapHeight * baseImageScale)
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw base truck template with aspect ratio preserved
        baseBitmap?.let { base ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Apply base color if set
            baseColor?.let { color ->
                paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
            }

            canvas.drawBitmap(base, null, baseImageRect, paint)
        }

        // Draw all elements
        elements.forEach { element ->
            drawElement(canvas, element)
        }

        // Draw selection box for selected element
        selectedElement?.let { element ->
            drawSelectionBox(canvas, element)
        }
    }

    private fun drawElement(canvas: Canvas, element: CanvasElement) {
        when (element) {
            is CanvasElement.StickerElement -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.save()
                canvas.translate(element.x, element.y)
                canvas.rotate(element.rotation)
                canvas.scale(element.scaleX, element.scaleY) // Use scaleX and scaleY
                canvas.drawBitmap(
                    element.bitmap,
                    -element.bitmap.width / 2f,
                    -element.bitmap.height / 2f,
                    paint
                )
                canvas.restore()
            }
            is CanvasElement.TextElement -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = element.textSize
                    color = element.textColor
                }
                element.measuredWidth = paint.measureText(element.text)
                element.measuredHeight = element.textSize

                canvas.save()
                canvas.translate(element.x, element.y)
                canvas.rotate(element.rotation)
                canvas.scale(element.scaleX, element.scaleY) // Use scaleX and scaleY
                canvas.drawText(element.text, 0f, 0f, paint)
                canvas.restore()
            }
        }
    }

    private fun drawSelectionBox(canvas: Canvas, element: CanvasElement) {
        val bounds = when (element) {
            is CanvasElement.StickerElement -> element.getBounds()
            is CanvasElement.TextElement -> element.getBounds()
        }

        // Draw bounding box
        canvas.drawRect(bounds, boxPaint)

        // Draw 4 CORNER handles (proportional resize) - WHITE
        listOf(
            bounds.left to bounds.top,
            bounds.right to bounds.top,
            bounds.left to bounds.bottom,
            bounds.right to bounds.bottom
        ).forEach { (x, y) ->
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, handlePaint)
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, handleStrokePaint)
        }

        // Draw 4 SIDE handles (directional resize) - GREEN
        val midHandlePaint = Paint().apply {
            color = Color.parseColor("#00FF00")
            style = Paint.Style.FILL
        }

        listOf(
            bounds.centerX() to bounds.top,      // Top
            bounds.centerX() to bounds.bottom,   // Bottom
            bounds.left to bounds.centerY(),     // Left
            bounds.right to bounds.centerY()     // Right
        ).forEach { (x, y) ->
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, midHandlePaint)
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, handleStrokePaint)
        }

        // Trash handle
        val trashX = bounds.right + 40f
        val trashY = bounds.top - 40f
        canvas.drawCircle(trashX, trashY, TRASH_HANDLE_SIZE / 2, trashPaint)

        // Draw X icon
        val iconSize = TRASH_HANDLE_SIZE / 3
        canvas.drawLine(trashX - iconSize, trashY - iconSize, trashX + iconSize, trashY + iconSize, trashIconPaint)
        canvas.drawLine(trashX + iconSize, trashY - iconSize, trashX - iconSize, trashY + iconSize, trashIconPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(event)
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2 && selectedElement != null) {
                    isMultiTouch = true
                    isDragging = false
                    isResizingFromHandle = false
                    initialDistance = getDistance(event)
                    selectedElement?.let { element ->
                        when (element) {
                            is CanvasElement.StickerElement -> {
                                initialRotation = getRotation(event) - element.rotation
                            }
                            is CanvasElement.TextElement -> {
                                initialRotation = getRotation(event) - element.rotation
                            }
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isMultiTouch && event.pointerCount == 2) {
                    handlePinchRotate(event)
                } else if (isDragging) {
                    handleDrag(event)
                } else if (isResizingFromHandle) {
                    handleResize(event)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 1) {
                    isMultiTouch = false
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    isDragging = false
                    isResizingFromHandle = false
                    activeHandle = null
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(event: MotionEvent) {
        val x = event.x
        val y = event.y

        // Check trash button first
        selectedElement?.let { element ->
            val bounds = when (element) {
                is CanvasElement.StickerElement -> element.getBounds()
                is CanvasElement.TextElement -> element.getBounds()
            }
            val trashX = bounds.right + 40f
            val trashY = bounds.top - 40f
            if (isPointInCircle(x, y, trashX, trashY, TRASH_HANDLE_SIZE / 2)) {
                elements.remove(element)
                selectedElement = null
                onElementDeleted?.invoke(element)
                invalidate()
                return
            }

            // Check resize handles
            val handle = getHandleAtPoint(x, y, element)
            if (handle != null) {
                isResizingFromHandle = true
                activeHandle = handle
                lastTouchX = x
                lastTouchY = y
                return
            }
        }

        // Check if tapped on any element
        val tappedElement = elements.reversed().firstOrNull { element ->
            val bounds = when (element) {
                is CanvasElement.StickerElement -> element.getBounds()
                is CanvasElement.TextElement -> element.getBounds()
            }
            bounds.contains(x, y)
        }

        if (tappedElement != null) {
            elements.forEach { it.isSelected = false }
            tappedElement.isSelected = true
            selectedElement = tappedElement
            isDragging = true
            lastTouchX = x
            lastTouchY = y
        } else {
            elements.forEach { it.isSelected = false }
            selectedElement = null
            isDragging = false
        }

        invalidate()
    }

    private fun handleDrag(event: MotionEvent) {
        selectedElement?.let { element ->
            val dx = event.x - lastTouchX
            val dy = event.y - lastTouchY

            when (element) {
                is CanvasElement.StickerElement -> {
                    element.x += dx
                    element.y += dy
                }
                is CanvasElement.TextElement -> {
                    element.x += dx
                    element.y += dy
                }
            }

            lastTouchX = event.x
            lastTouchY = event.y
            invalidate()
        }
    }

    private fun handleResize(event: MotionEvent) {
        selectedElement?.let { element ->
            val bounds = when (element) {
                is CanvasElement.StickerElement -> element.getBounds()
                is CanvasElement.TextElement -> element.getBounds()
            }
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()

            when (activeHandle) {
                // WHITE CORNER HANDLES - PROPORTIONAL (both scaleX and scaleY change together)
                ResizeHandle.TOP_LEFT, ResizeHandle.TOP_RIGHT,
                ResizeHandle.BOTTOM_LEFT, ResizeHandle.BOTTOM_RIGHT -> {
                    val oldDist = hypot(
                        (lastTouchX - centerX).toDouble(),
                        (lastTouchY - centerY).toDouble()
                    ).toFloat()

                    val newDist = hypot(
                        (event.x - centerX).toDouble(),
                        (event.y - centerY).toDouble()
                    ).toFloat()

                    val scaleFactor = newDist / oldDist

                    when (element) {
                        is CanvasElement.StickerElement -> {
                            element.scaleX *= scaleFactor
                            element.scaleY *= scaleFactor
                            element.scaleX = element.scaleX.coerceAtMost(5f)
                            element.scaleY = element.scaleY.coerceAtMost(5f)
                        }
                        is CanvasElement.TextElement -> {
                            element.scaleX *= scaleFactor
                            element.scaleY *= scaleFactor
                            element.scaleX = element.scaleX.coerceAtMost(5f)
                            element.scaleY = element.scaleY.coerceAtMost(5f)
                        }
                    }
                }

                // GREEN TOP/BOTTOM - VERTICAL ONLY (only scaleY changes, scaleX stays same)
                ResizeHandle.TOP, ResizeHandle.BOTTOM -> {
                    val dy = if (activeHandle == ResizeHandle.BOTTOM) {
                        event.y - lastTouchY
                    } else {
                        lastTouchY - event.y
                    }

                    val oldHeight = bounds.height()
                    val newHeight = oldHeight + (dy * 2) // *2 because we scale from center
                    val scaleFactor = newHeight / oldHeight

                    when (element) {
                        is CanvasElement.StickerElement -> {
                            element.scaleY *= scaleFactor
                            element.scaleY = element.scaleY.coerceAtMost(5f)
                            // scaleX stays the same!
                        }
                        is CanvasElement.TextElement -> {
                            element.scaleY *= scaleFactor
                            element.scaleY = element.scaleY.coerceAtMost(5f)
                            // scaleX stays the same!
                        }
                    }
                }

                // GREEN LEFT/RIGHT - HORIZONTAL ONLY (only scaleX changes, scaleY stays same)
                ResizeHandle.LEFT, ResizeHandle.RIGHT -> {
                    val dx = if (activeHandle == ResizeHandle.RIGHT) {
                        event.x - lastTouchX
                    } else {
                        lastTouchX - event.x
                    }

                    val oldWidth = bounds.width()
                    val newWidth = oldWidth + (dx * 2) // *2 because we scale from center
                    val scaleFactor = newWidth / oldWidth

                    when (element) {
                        is CanvasElement.StickerElement -> {
                            element.scaleX *= scaleFactor
                            element.scaleX = element.scaleX.coerceAtMost(5f)
                            // scaleY stays the same!
                        }
                        is CanvasElement.TextElement -> {
                            element.scaleX *= scaleFactor
                            element.scaleX = element.scaleX.coerceAtMost(5f)
                            // scaleY stays the same!
                        }
                    }
                }

                null -> {}
            }

            lastTouchX = event.x
            lastTouchY = event.y
            invalidate()
        }
    }

    private fun handlePinchRotate(event: MotionEvent) {
        selectedElement?.let { element ->
            val distance = getDistance(event)
            val scaleFactor = distance / initialDistance
            val rotation = getRotation(event)

            when (element) {
                is CanvasElement.StickerElement -> {
                    element.scaleX *= scaleFactor
                    element.scaleY *= scaleFactor
                    element.scaleX = element.scaleX.coerceAtMost(5f)
                    element.scaleY = element.scaleY.coerceAtMost(5f)
                    element.rotation = rotation - initialRotation
                }
                is CanvasElement.TextElement -> {
                    element.scaleX *= scaleFactor
                    element.scaleY *= scaleFactor
                    element.scaleX = element.scaleX.coerceAtMost(5f)
                    element.scaleY = element.scaleY.coerceAtMost(5f)
                    element.rotation = rotation - initialRotation
                }
            }

            initialDistance = distance
            invalidate()
        }
    }

    private fun getHandleAtPoint(x: Float, y: Float, element: CanvasElement): ResizeHandle? {
        val bounds = when (element) {
            is CanvasElement.StickerElement -> element.getBounds()
            is CanvasElement.TextElement -> element.getBounds()
        }

        // Check corner handles first
        if (isPointInCircle(x, y, bounds.left, bounds.top, HANDLE_SIZE / 2)) {
            return ResizeHandle.TOP_LEFT
        }
        if (isPointInCircle(x, y, bounds.right, bounds.top, HANDLE_SIZE / 2)) {
            return ResizeHandle.TOP_RIGHT
        }
        if (isPointInCircle(x, y, bounds.left, bounds.bottom, HANDLE_SIZE / 2)) {
            return ResizeHandle.BOTTOM_LEFT
        }
        if (isPointInCircle(x, y, bounds.right, bounds.bottom, HANDLE_SIZE / 2)) {
            return ResizeHandle.BOTTOM_RIGHT
        }

        // Check side handles
        if (isPointInCircle(x, y, bounds.centerX(), bounds.top, HANDLE_SIZE / 2)) {
            return ResizeHandle.TOP
        }
        if (isPointInCircle(x, y, bounds.centerX(), bounds.bottom, HANDLE_SIZE / 2)) {
            return ResizeHandle.BOTTOM
        }
        if (isPointInCircle(x, y, bounds.left, bounds.centerY(), HANDLE_SIZE / 2)) {
            return ResizeHandle.LEFT
        }
        if (isPointInCircle(x, y, bounds.right, bounds.centerY(), HANDLE_SIZE / 2)) {
            return ResizeHandle.RIGHT
        }

        return null
    }

    private fun isPointInCircle(px: Float, py: Float, cx: Float, cy: Float, radius: Float): Boolean {
        val dx = px - cx
        val dy = py - cy
        return sqrt(dx * dx + dy * dy) <= radius
    }

    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    private fun getRotation(event: MotionEvent): Float {
        val dx = event.getX(1) - event.getX(0)
        val dy = event.getY(1) - event.getY(0)
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    fun captureCanvas(): Bitmap {
        val tempSelected = selectedElement
        selectedElement = null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)

        selectedElement = tempSelected

        return bitmap
    }
}