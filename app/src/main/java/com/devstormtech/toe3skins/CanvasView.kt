package com.devstormtech.toe3skins

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Canvas state
    val elements = mutableListOf<CanvasElement>()
    
    // Display bitmap (potentially downsampled for performance)
    var baseBitmap: Bitmap? = null
        set(value) {
            field = value
            calculateBaseImageRect()
            invalidate()
        }
    
    // Original full-resolution bitmap for saving (preserves game texture mapping)
    var originalBitmap: Bitmap? = null
    
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
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT
    }

    // Paint objects
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

            if (bitmapAspect > viewAspect) {
                baseImageScale = viewWidth / bitmapWidth
                val scaledHeight = bitmapHeight * baseImageScale
                baseImageOffsetX = 0f
                baseImageOffsetY = (viewHeight - scaledHeight) / 2f
            } else {
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

        baseBitmap?.let { base ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            baseColor?.let { color ->
                paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
            }
            canvas.drawBitmap(base, null, baseImageRect, paint)
        }

        elements.forEach { element ->
            drawElement(canvas, element)
        }

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
                canvas.scale(element.scaleX, element.scaleY)
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
                canvas.scale(element.scaleX, element.scaleY)
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

        canvas.drawRect(bounds, boxPaint)

        // Corners
        listOf(
            bounds.left to bounds.top, bounds.right to bounds.top,
            bounds.left to bounds.bottom, bounds.right to bounds.bottom
        ).forEach { (x, y) ->
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, handlePaint)
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, handleStrokePaint)
        }

        // Sides
        val midHandlePaint = Paint().apply { color = Color.GREEN; style = Paint.Style.FILL }
        listOf(
            bounds.centerX() to bounds.top, bounds.centerX() to bounds.bottom,
            bounds.left to bounds.centerY(), bounds.right to bounds.centerY()
        ).forEach { (x, y) ->
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, midHandlePaint)
            canvas.drawCircle(x, y, HANDLE_SIZE / 2, handleStrokePaint)
        }

        // Trash
        val trashX = bounds.right + 40f
        val trashY = bounds.top - 40f
        canvas.drawCircle(trashX, trashY, TRASH_HANDLE_SIZE / 2, trashPaint)
        
        // X Icon
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
                            is CanvasElement.StickerElement -> initialRotation = getRotation(event) - element.rotation
                            is CanvasElement.TextElement -> initialRotation = getRotation(event) - element.rotation
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isMultiTouch && event.pointerCount == 2) handlePinchRotate(event)
                else if (isDragging) handleDrag(event)
                else if (isResizingFromHandle) handleResize(event)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 1) isMultiTouch = false
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
        lastTouchX = x
        lastTouchY = y

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

            val handle = getHandleAtPoint(x, y, element)
            if (handle != null) {
                isResizingFromHandle = true
                activeHandle = handle
                return
            }
        }

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
                ResizeHandle.TOP_LEFT, ResizeHandle.TOP_RIGHT,
                ResizeHandle.BOTTOM_LEFT, ResizeHandle.BOTTOM_RIGHT -> {
                    val oldDist = hypot((lastTouchX - centerX).toDouble(), (lastTouchY - centerY).toDouble()).toFloat()
                    val newDist = hypot((event.x - centerX).toDouble(), (event.y - centerY).toDouble()).toFloat()
                    val scaleFactor = newDist / oldDist
                    
                    when (element) {
                        is CanvasElement.StickerElement -> {
                            element.scaleX *= scaleFactor
                            element.scaleY *= scaleFactor
                        }
                        is CanvasElement.TextElement -> {
                            element.scaleX *= scaleFactor
                            element.scaleY *= scaleFactor
                        }
                    }
                }
                ResizeHandle.LEFT, ResizeHandle.RIGHT -> {
                    val dx = event.x - lastTouchX
                    when (element) {
                        is CanvasElement.StickerElement -> {
                            val change = dx / element.bitmap.width
                            element.scaleX += change * 2
                            element.scaleX = element.scaleX.coerceAtLeast(0.1f)
                        }
                        is CanvasElement.TextElement -> {
                            val change = dx / 100f
                            element.scaleX += change
                            element.scaleX = element.scaleX.coerceAtLeast(0.1f)
                        }
                    }
                }
                ResizeHandle.TOP, ResizeHandle.BOTTOM -> {
                    val dy = event.y - lastTouchY
                    when (element) {
                        is CanvasElement.StickerElement -> {
                            val change = dy / element.bitmap.height
                            element.scaleY += change * 2
                            element.scaleY = element.scaleY.coerceAtLeast(0.1f)
                        }
                        is CanvasElement.TextElement -> {
                            val change = dy / 100f
                            element.scaleY += change
                            element.scaleY = element.scaleY.coerceAtLeast(0.1f)
                        }
                    }
                }
                else -> {}
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
                    element.rotation = rotation - initialRotation
                }
                is CanvasElement.TextElement -> {
                    element.scaleX *= scaleFactor
                    element.scaleY *= scaleFactor
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
        
        if (isPointInCircle(x, y, bounds.left, bounds.top, HANDLE_SIZE/2)) return ResizeHandle.TOP_LEFT
        if (isPointInCircle(x, y, bounds.right, bounds.bottom, HANDLE_SIZE/2)) return ResizeHandle.BOTTOM_RIGHT
        if (isPointInCircle(x, y, bounds.right, bounds.top, HANDLE_SIZE/2)) return ResizeHandle.TOP_RIGHT
        if (isPointInCircle(x, y, bounds.left, bounds.bottom, HANDLE_SIZE/2)) return ResizeHandle.BOTTOM_LEFT
        
        if (isPointInCircle(x, y, bounds.centerX(), bounds.top, HANDLE_SIZE/2)) return ResizeHandle.TOP
        if (isPointInCircle(x, y, bounds.centerX(), bounds.bottom, HANDLE_SIZE/2)) return ResizeHandle.BOTTOM
        if (isPointInCircle(x, y, bounds.left, bounds.centerY(), HANDLE_SIZE/2)) return ResizeHandle.LEFT
        if (isPointInCircle(x, y, bounds.right, bounds.centerY(), HANDLE_SIZE/2)) return ResizeHandle.RIGHT
        
        return null
    }

    private fun isPointInCircle(px: Float, py: Float, cx: Float, cy: Float, radius: Float): Boolean {
        return hypot((px-cx).toDouble(), (py-cy).toDouble()) <= radius
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

    /**
     * FINAL SIMPLE SAVE
     * Render the template and all elements by scaling screen coordinates directly to the original bitmap size.
     */
    fun generateHighResBitmap(): Bitmap? {
        val original = originalBitmap ?: baseBitmap ?: return null
        
        android.util.Log.d("SaveDebug", "=== FIXED SAVE ===")
        android.util.Log.d("SaveDebug", "Original bitmap size: ${original.width}x${original.height}")
        android.util.Log.d("SaveDebug", "Base rect on screen: $baseImageRect")
        
        // Scale factor from displayed template to original resolution
        val scaleX = original.width.toFloat() / baseImageRect.width()
        val scaleY = original.height.toFloat() / baseImageRect.height()
        android.util.Log.d("SaveDebug", "Scale factors: $scaleX x $scaleY")
        
        // Create output bitmap at original resolution
        val outputBitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        
        // Draw the full‑resolution template WITH color filter if set
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        baseColor?.let { color ->
            basePaint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
        // Draw template scaled to fill entire output bitmap
        val destRect = RectF(0f, 0f, original.width.toFloat(), original.height.toFloat())
        canvas.drawBitmap(original, null, destRect, basePaint)
        
        // Paint for stickers (same as on‑screen rendering)
        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        
        // Render each element using scaled coordinates
        elements.forEach { element ->
            when (element) {
                is CanvasElement.StickerElement -> {
                    // Convert screen position to original‑bitmap coordinates
                    val tx = (element.x - baseImageRect.left) * scaleX
                    val ty = (element.y - baseImageRect.top) * scaleY
                    val sx = element.scaleX * scaleX
                    val sy = element.scaleY * scaleY
                    
                    android.util.Log.d("SaveDebug", "Sticker: screen(${element.x}, ${element.y}) -> original($tx, $ty) scale($sx, $sy)")
                    
                    canvas.save()
                    canvas.translate(tx, ty)
                    canvas.rotate(element.rotation)
                    canvas.scale(sx, sy)
                    canvas.drawBitmap(
                        element.bitmap,
                        -element.bitmap.width / 2f,
                        -element.bitmap.height / 2f,
                        stickerPaint
                    )
                    canvas.restore()
                }
                is CanvasElement.TextElement -> {
                    val tx = (element.x - baseImageRect.left) * scaleX
                    val ty = (element.y - baseImageRect.top) * scaleY
                    val sx = element.scaleX * scaleX
                    val sy = element.scaleY * scaleY
                    
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = element.textSize * scaleX
                        color = element.textColor
                    }
                    
                    canvas.save()
                    canvas.translate(tx, ty)
                    canvas.rotate(element.rotation)
                    canvas.scale(sx, sy)
                    canvas.drawText(element.text, 0f, 0f, textPaint)
                    canvas.restore()
                }
            }
        }
        
        android.util.Log.d("SaveDebug", "=== SAVE COMPLETE ===")
        return outputBitmap
    }
}