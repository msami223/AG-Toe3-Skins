package com.devstormtech.toe3skins

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF

sealed class CanvasElement {
    abstract val id: String
    abstract val matrix: Matrix
    abstract var isSelected: Boolean

    data class StickerElement(
        override val id: String,
        val bitmap: Bitmap,
        override val matrix: Matrix = Matrix(),
        var x: Float = 0f,
        var y: Float = 0f,
        var scaleX: Float = 1f,
        var scaleY: Float = 1f,
        var rotation: Float = 0f,
        override var isSelected: Boolean = false,
        var imagePath: String? = null,
        var isExternal: Boolean = false
    ) : CanvasElement() {
        fun getBounds(): RectF {
            val halfWidth = (bitmap.width * scaleX) / 2
            val halfHeight = (bitmap.height * scaleY) / 2
            return RectF(
                x - halfWidth,
                y - halfHeight,
                x + halfWidth,
                y + halfHeight
            )
        }
    }

    data class TextElement(
        override val id: String,
        var text: String,
        var textSize: Float = 96f,
        var textColor: Int,
        var fontFamily: String = "sans-serif",
        override val matrix: Matrix = Matrix(),
        var x: Float = 0f,
        var y: Float = 0f,
        var scaleX: Float = 1f,
        var scaleY: Float = 1f,
        var rotation: Float = 0f,
        override var isSelected: Boolean = false,
        var measuredWidth: Float = 0f,
        var measuredHeight: Float = 0f
    ) : CanvasElement() {
        fun getBounds(): RectF {
            return RectF(
                x - 20f,
                y - measuredHeight * scaleY,
                x + measuredWidth * scaleX + 20f,
                y + 20f
            )
        }
    }
}

data class CanvasState(
    val elements: List<CanvasElement> = emptyList(),
    val baseColor: Int? = null,
    val baseBitmap: Bitmap? = null
)