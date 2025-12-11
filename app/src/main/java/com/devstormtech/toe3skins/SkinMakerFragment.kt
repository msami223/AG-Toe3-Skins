package com.devstormtech.toe3skins

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.ImageDecoder


import com.google.android.material.button.MaterialButton
import java.io.FileOutputStream
import java.util.*

class SkinMakerFragment : Fragment() {

    private lateinit var canvasView: CanvasView
    private lateinit var tabLayoutTools: com.google.android.material.tabs.TabLayout
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnRedo: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: ImageView

    // Undo/Redo stacks
    private val undoStack = Stack<CanvasState>()
    private val redoStack = Stack<CanvasState>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { addCustomSticker(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_skin_maker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        
        // Ensure layout is loaded before loading the template
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (canvasView.baseBitmap == null) {
                    loadTruckTemplate()
                }
            }
        })
        
        setupListeners()
    }

    private fun initViews(view: View) {
        canvasView = view.findViewById(R.id.canvasView)
        tabLayoutTools = view.findViewById(R.id.tabLayoutTools)
        btnUndo = view.findViewById(R.id.btnUndo)
        btnRedo = view.findViewById(R.id.btnRedo)
        btnSave = view.findViewById(R.id.btnSave)
        btnBack = view.findViewById(R.id.btnBackSkinMaker)

        btnBack.visibility = View.GONE // Hide back button in fragment mode

        // Setup Tabs
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Color"))
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Stickers"))
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Text"))
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Layers"))

        // Set delete callback
        canvasView.onElementDeleted = {
            Toast.makeText(requireContext(), "Element deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTruckTemplate() {
        // Load original FULL resolution for saving (game-ready texture)
        val originalOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inScaled = false
        }
        canvasView.originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.stream_template, originalOptions)
        
        // Load downsampled version for screen display (performance)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(resources, R.drawable.stream_template, options)

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight
        val maxDimension = 2048
        
        var calculatedSampleSize = 1
        while (imageWidth / calculatedSampleSize > maxDimension || imageHeight / calculatedSampleSize > maxDimension) {
            calculatedSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = calculatedSampleSize
            inScaled = false
        }

        canvasView.baseBitmap = BitmapFactory.decodeResource(resources, R.drawable.stream_template, decodeOptions)
        canvasView.invalidate()
    }

    private fun setupListeners() {
        tabLayoutTools.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.text) {
                    "Color" -> showColorPicker()
                    "Stickers" -> showStickerPicker()
                    "Text" -> showTextEditor()
                    "Layers" -> showLayersDialog()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Allow re-opening the dialog if tapped again
                onTabSelected(tab)
            }
        })

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
        dialog.show(childFragmentManager, "ColorPicker")
    }

    private fun showStickerPicker() {
        val bottomSheet = StickerBottomSheet { stickerRes ->
            if (stickerRes == R.drawable.ic_upload) {
                pickImageLauncher.launch("image/*")
            } else {
                saveState()
                addSticker(stickerRes)
            }
        }
        bottomSheet.show(childFragmentManager, "StickerPicker")
    }

    private fun showTextEditor() {
        val dialog = TextEditorDialog { text, size, color ->
            saveState()
            addText(text, size, color)
        }
        dialog.show(childFragmentManager, "TextEditor")
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
                Toast.makeText(requireContext(), "Element deleted", Toast.LENGTH_SHORT).show()
            }
        )
        dialog.show(childFragmentManager, "LayersDialog")
    }

    private fun addSticker(stickerRes: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, stickerRes)
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 1200, 1200, true)

        val centerX = canvasView.width / 2f
        val centerY = canvasView.height / 2f

        val element = CanvasElement.StickerElement(
            id = UUID.randomUUID().toString(),
            bitmap = scaledBitmap,
            x = centerX,
            y = centerY,
            scaleX = 0.5f,
            scaleY = 0.5f,
            isSelected = true
        )

        canvasView.elements.forEach { it.isSelected = false }
        canvasView.elements.add(element)
        canvasView.selectedElement = element
        canvasView.invalidate()
    }

    private fun addCustomSticker(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }

            // Scale down if too large
            val maxDimension = 1200
            var width = bitmap.width
            var height = bitmap.height
            if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (width > height) {
                    width = maxDimension
                    height = (width / ratio).toInt()
                } else {
                    height = maxDimension
                    width = (height * ratio).toInt()
                }
            }
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)

            saveState()
            
            val centerX = canvasView.width / 2f
            val centerY = canvasView.height / 2f

            val element = CanvasElement.StickerElement(
                id = UUID.randomUUID().toString(),
                bitmap = scaledBitmap,
                x = centerX,
                y = centerY,
                scaleX = 0.5f,
                scaleY = 0.5f,
                isSelected = true
            )

            canvasView.elements.forEach { it.isSelected = false }
            canvasView.elements.add(element)
            canvasView.selectedElement = element
            canvasView.invalidate()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addText(text: String, textSize: Float, textColor: Int) {
        val actualSize = 144f
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = actualSize
        }
        val measuredWidth = paint.measureText(text)
        val measuredHeight = actualSize

        val centerX = canvasView.width / 2f
        val centerY = canvasView.height / 2f

        val element = CanvasElement.TextElement(
            id = UUID.randomUUID().toString(),
            text = text,
            textSize = textSize,
            textColor = textColor,
            x = centerX,
            y = centerY,
            scaleX = 1f,
            scaleY = 1f,
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
            Toast.makeText(requireContext(), "Undo", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "Redo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSkin() {
        val bitmap = canvasView.generateHighResBitmap()
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Template not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val filename = "truck_skin_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TOE3Skins")
                }

                val uri = requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    requireActivity().contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    Toast.makeText(requireContext(), "✅ Saved to Gallery!", Toast.LENGTH_LONG).show()
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
                requireActivity().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                Toast.makeText(requireContext(), "✅ Saved!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
