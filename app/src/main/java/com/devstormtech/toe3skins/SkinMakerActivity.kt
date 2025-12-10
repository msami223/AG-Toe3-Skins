package com.devstormtech.toe3skins

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.FileOutputStream
import java.util.*

class SkinMakerActivity : AppCompatActivity() {

    private lateinit var canvasView: CanvasView
    private lateinit var btnColorPicker: MaterialButton
    private lateinit var btnStickers: MaterialButton
    private lateinit var btnText: MaterialButton
    private lateinit var btnLayers: MaterialButton
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnRedo: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: ImageView

    // Undo/Redo stacks
    private val undoStack = Stack<CanvasState>()
    private val redoStack = Stack<CanvasState>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_maker)

        initViews()
        loadTruckTemplate()
        setupListeners()
    }

    private fun initViews() {
        canvasView = findViewById(R.id.canvasView)
        btnColorPicker = findViewById(R.id.btnColorPicker)
        btnStickers = findViewById(R.id.btnStickers)
        btnText = findViewById(R.id.btnText)
        btnLayers = findViewById(R.id.btnLayers)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBackSkinMaker)

        // Set delete callback
        canvasView.onElementDeleted = {
            Toast.makeText(this, "Element deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTruckTemplate() {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false // Do not upscale based on screen density
        }
        BitmapFactory.decodeResource(resources, R.drawable.stream_template, options)

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight
        val maxDimension = 2048
        
        android.util.Log.d("SkinMaker", "Original Dimensions: ${imageWidth}x${imageHeight}")

        var calculatedSampleSize = 1
        
        // Strict logic: Keep doubling sample size until BOTH dimensions are <= maxDimension
        while (imageWidth / calculatedSampleSize > maxDimension || imageHeight / calculatedSampleSize > maxDimension) {
            calculatedSampleSize *= 2
        }

        android.util.Log.d("SkinMaker", "Calculated Sample Size: $calculatedSampleSize")

        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = calculatedSampleSize
            inScaled = false // Do not upscale based on screen density
        }

        canvasView.baseBitmap = BitmapFactory.decodeResource(resources, R.drawable.stream_template, decodeOptions)
        canvasView.invalidate()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnColorPicker.setOnClickListener { showColorPicker() }

        btnStickers.setOnClickListener { showStickerPicker() }

        btnText.setOnClickListener { showTextEditor() }

        btnLayers.setOnClickListener { showLayersDialog() }

        btnUndo.setOnClickListener { undo() }

        btnRedo.setOnClickListener { redo() }

        btnSave.setOnClickListener { saveSkin() }
    }

    private fun showColorPicker() {
        val dialog = ColorPickerDialog(canvasView.baseColor ?: Color.WHITE) { color ->
            saveState()
            canvasView.baseColor = color
            canvasView.invalidate()
        }
        dialog.show(supportFragmentManager, "ColorPicker")
    }

    private fun showStickerPicker() {
        val bottomSheet = StickerBottomSheet { stickerRes ->
            saveState()
            addSticker(stickerRes)
        }
        bottomSheet.show(supportFragmentManager, "StickerPicker")
    }

    private fun showTextEditor() {
        val dialog = TextEditorDialog { text, size, color ->
            saveState()
            addText(text, size, color)
        }
        dialog.show(supportFragmentManager, "TextEditor")
    }

    private fun showLayersDialog() {
        val dialog = LayersDialog(
            layers = canvasView.elements.toList(),
            onLayerSelected = { layer ->
                canvasView.elements.forEach { it.isSelected = false }
                layer.isSelected = true
                canvasView.selectedElement = layer
                canvasView.invalidate()
            },
            onLayerDeleted = { layer ->
                saveState()
                canvasView.elements.remove(layer)
                if (canvasView.selectedElement == layer) {
                    canvasView.selectedElement = null
                }
                canvasView.invalidate()
                Toast.makeText(this, "Element deleted", Toast.LENGTH_SHORT).show()
            }
        )
        dialog.show(supportFragmentManager, "LayersDialog")
    }

    private fun addSticker(stickerRes: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, stickerRes)
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 1200, 1200, true)

        val element = CanvasElement.StickerElement(
            id = UUID.randomUUID().toString(),
            bitmap = scaledBitmap,
            x = canvasView.width / 2f,
            y = canvasView.height / 2f,
            scaleX = 0.5f,  // Use scaleX instead of scale
            scaleY = 0.5f,  // Use scaleY instead of scale
            isSelected = true
        )

        canvasView.elements.forEach { it.isSelected = false }
        canvasView.elements.add(element)
        canvasView.selectedElement = element
        canvasView.invalidate()
    }

    private fun addText(text: String, textSize: Float, textColor: Int) {
        val actualSize = 144f
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = actualSize
        }
        val measuredWidth = paint.measureText(text)
        val measuredHeight = actualSize

        val element = CanvasElement.TextElement(
            id = UUID.randomUUID().toString(),
            text = text,
            textSize = actualSize,
            textColor = textColor,
            x = canvasView.width / 2f - measuredWidth / 2,
            y = canvasView.height / 2f,
            scaleX = 0.5f,  // Use scaleX instead of scale
            scaleY = 0.5f,  // Use scaleY instead of scale
            isSelected = true,
            measuredWidth = measuredWidth,
            measuredHeight = measuredHeight
        )

        canvasView.elements.forEach { it.isSelected = false }
        canvasView.elements.add(element)
        canvasView.selectedElement = element
        canvasView.invalidate()
    }

    private fun saveState() {
        val state = CanvasState(
            elements = canvasView.elements.map {
                when (it) {
                    is CanvasElement.StickerElement -> it.copy()
                    is CanvasElement.TextElement -> it.copy()
                }
            },
            baseColor = canvasView.baseColor
        )
        undoStack.push(state)
        redoStack.clear()
    }

    private fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentState = CanvasState(
                elements = canvasView.elements.toList(),
                baseColor = canvasView.baseColor
            )
            redoStack.push(currentState)

            val previousState = undoStack.pop()
            canvasView.elements.clear()
            canvasView.elements.addAll(previousState.elements)
            canvasView.baseColor = previousState.baseColor
            canvasView.selectedElement = null

            canvasView.invalidate()
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentState = CanvasState(
                elements = canvasView.elements.toList(),
                baseColor = canvasView.baseColor
            )
            undoStack.push(currentState)

            val nextState = redoStack.pop()
            canvasView.elements.clear()
            canvasView.elements.addAll(nextState.elements)
            canvasView.baseColor = nextState.baseColor
            canvasView.selectedElement = null

            canvasView.invalidate()
            Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSkin() {
        val bitmap = canvasView.captureCanvas()

        try {
            val filename = "truck_skin_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TOE3Skins")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    Toast.makeText(this, "✅ Saved to Gallery!", Toast.LENGTH_LONG).show()
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val toe3Dir = java.io.File(imagesDir, "TOE3Skins")
                if (!toe3Dir.exists()) toe3Dir.mkdirs()

                val file = java.io.File(toe3Dir, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                Toast.makeText(this, "✅ Saved!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}