package com.devstormtech.toe3skins

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * A visual HSV color wheel that users can tap/drag to select colors
 */
class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var wheelBitmap: Bitmap? = null
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val selectorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var selectorRadius = 16f

    // Current HSV values
    private var currentHue = 0f
    private var currentSaturation = 1f
    private var currentBrightness = 1f

    // Selector position
    private var selectorX = 0f
    private var selectorY = 0f

    var onColorChanged: ((Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        if (w <= 0 || h <= 0) return
        
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f - selectorRadius - 8f
        
        if (radius <= 0) return
        
        // Create the color wheel bitmap
        createWheelBitmap()
        
        // Position selector based on current color
        updateSelectorPosition()
    }

    private fun createWheelBitmap() {
        val size = (radius * 2).toInt()
        if (size <= 0) return
        
        try {
            wheelBitmap?.recycle()
            wheelBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(wheelBitmap!!)
            
            val cx = size / 2f
            val cy = size / 2f
            val r = radius
            
            // Use a shader for smoother gradient instead of pixel-by-pixel
            val colors = IntArray(361)
            for (i in 0..360) {
                colors[i] = Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, currentBrightness))
            }
            
            val sweepGradient = SweepGradient(cx, cy, colors, null)
            val radialGradient = RadialGradient(cx, cy, r, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP)
            
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = sweepGradient
            canvas.drawCircle(cx, cy, r, paint)
            
            // Overlay with radial gradient for saturation
            paint.shader = radialGradient
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            canvas.drawCircle(cx, cy, r, paint)
            
            // Apply brightness
            if (currentBrightness < 1f) {
                val dimPaint = Paint()
                dimPaint.color = Color.BLACK
                dimPaint.alpha = ((1f - currentBrightness) * 255).toInt()
                canvas.drawCircle(cx, cy, r, dimPaint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (radius <= 0) return
        
        // Draw the color wheel
        wheelBitmap?.let {
            if (!it.isRecycled) {
                canvas.drawBitmap(it, centerX - radius, centerY - radius, wheelPaint)
            }
        }
        
        // Draw outer ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0xFF333333.toInt()
        }
        canvas.drawCircle(centerX, centerY, radius, ringPaint)
        
        // Draw selector
        selectorFillPaint.color = getCurrentColor()
        canvas.drawCircle(selectorX, selectorY, selectorRadius, selectorFillPaint)
        canvas.drawCircle(selectorX, selectorY, selectorRadius, selectorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (radius <= 0) return super.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                var distance = sqrt(dx * dx + dy * dy)
                
                // Clamp to wheel radius
                if (distance > radius) distance = radius
                
                // Calculate angle and saturation
                val angle = atan2(dy, dx)
                currentHue = ((Math.toDegrees(angle.toDouble()) + 360) % 360).toFloat()
                currentSaturation = (distance / radius).coerceIn(0f, 1f)
                
                // Update selector position
                selectorX = centerX + cos(angle) * distance
                selectorY = centerY + sin(angle) * distance
                
                invalidate()
                onColorChanged?.invoke(getCurrentColor())
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateSelectorPosition() {
        if (radius <= 0) {
            selectorX = centerX
            selectorY = centerY
            return
        }
        val angle = Math.toRadians(currentHue.toDouble())
        val distance = currentSaturation * radius
        selectorX = centerX + (cos(angle) * distance).toFloat()
        selectorY = centerY + (sin(angle) * distance).toFloat()
    }

    fun getCurrentColor(): Int {
        return Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, currentBrightness))
    }

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSaturation = hsv[1]
        currentBrightness = hsv[2].coerceIn(0.1f, 1f) // Don't allow pure black
        
        if (radius > 0) {
            updateSelectorPosition()
            createWheelBitmap()
        }
        invalidate()
    }

    fun setBrightness(brightness: Float) {
        currentBrightness = brightness.coerceIn(0.1f, 1f)
        if (radius > 0) {
            createWheelBitmap()
        }
        invalidate()
        onColorChanged?.invoke(getCurrentColor())
    }

    fun getBrightness(): Float = currentBrightness
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wheelBitmap?.recycle()
        wheelBitmap = null
    }
}
