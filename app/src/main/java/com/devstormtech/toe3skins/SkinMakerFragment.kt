package com.devstormtech.toe3skins

import android.content.ContentValues
import android.graphics.Bitmap
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
import android.graphics.Matrix


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
    private lateinit var tvTruckName: android.widget.TextView
    private lateinit var btnChangeModel: MaterialButton

    // Undo/Redo stacks
    private val undoStack = Stack<CanvasState>()
    private val redoStack = Stack<CanvasState>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { addCustomSticker(it) }
    }

    private lateinit var projectManager: ProjectManager
    private var currentProjectId: String? = null

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
                // Auto-load removed to enforce selection
                // if (canvasView.baseBitmap == null) {
                //    loadTruckTemplate()
                // }
            }
        })
        
        setupListeners()
    }

    private fun initViews(view: View) {
        canvasView = view.findViewById(R.id.canvasView)
        tabLayoutTools = view.findViewById(R.id.tabLayoutTools)
        btnUndo = view.findViewById(R.id.btnUndo)
        btnRedo = view.findViewById(R.id.btnRedo)
        btnRedo = view.findViewById(R.id.btnRedo)
        btnSave = view.findViewById(R.id.btnSave)
        val btnExport = view.findViewById<MaterialButton>(R.id.btnExport)
        btnExport?.setOnClickListener { exportSkin() }
        btnBack = view.findViewById(R.id.btnBackSkinMaker)
        tvTruckName = view.findViewById(R.id.tvTruckName)
        btnChangeModel = view.findViewById(R.id.btnChangeModel)

        btnBack.visibility = View.GONE // Hide back button in fragment mode
        btnChangeModel.visibility = View.GONE // Hide until truck is loaded

        // Setup Tabs
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Color"))
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Stickers"))
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Text"))
        tabLayoutTools.addTab(tabLayoutTools.newTab().setText("Layers"))

        // Set delete callback
        canvasView.onElementDeleted = {
            Toast.makeText(requireContext(), "Element deleted", Toast.LENGTH_SHORT).show()
        }
        
        projectManager = ProjectManager(requireContext())
    }

    private fun exportSkin() {
        if (currentTruck == null) {
            Toast.makeText(requireContext(), "No truck loaded!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val bitmap = canvasView.generateHighResBitmap()
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Failed to generate skin.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val filename = "TOE3_Skin_${System.currentTimeMillis()}.png"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TOE3Skins")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { out ->
                    if (out != null) {
                         bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(requireContext(), "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                // Show ad after export
                AdManager.onUserAction(requireActivity())
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
             Toast.makeText(requireContext(), "Failed to create file.", Toast.LENGTH_SHORT).show()
        }
    }

    // Current loaded truck
    private var currentTruck: TruckModel? = null

    /**
     * Public method to load a truck template - called from MainActivity after truck selection
     */
    fun loadTruck(truck: TruckModel) {
        currentTruck = truck
        currentProjectId = null // Reset project ID for new truck
        
        // Update header to show truck name
        tvTruckName.text = "Editing: ${truck.displayName}"
        btnChangeModel.visibility = View.VISIBLE
        
        // Clear previous elements
        canvasView.elements.clear()
        canvasView.selectedElement = null
        canvasView.baseColor = null
        undoStack.clear()
        redoStack.clear()
        
        loadTruckTemplate(truck.templateResource)
    }

    fun loadProject(truck: TruckModel, state: CanvasState, projectId: String, projectName: String) {
        currentTruck = truck
        currentProjectId = projectId
        currentProjectName = projectName
        
        tvTruckName.text = "Editing: ${truck.displayName}"
        btnChangeModel.visibility = View.VISIBLE
        
        // Reset stacks
        undoStack.clear()
        redoStack.clear()
        
        // Load Template first (sets up originalBitmap/baseBitmap defaults)
        loadTruckTemplate(truck.templateResource)
        
        // Restore custom base bitmap if present (fixes "White Truck" issue)
        if (state.baseBitmap != null) {
            canvasView.baseBitmap = state.baseBitmap
             // Also update originalBitmap to match visual consistency? 
             // ideally we'd have saved the high-res one, but for now this prevents data loss.
             // If we don't update originalBitmap, high-res export might be blank/default.
             // Let's effectively promote this bitmap to original for consistency.
             canvasView.originalBitmap = state.baseBitmap
        }
        
        // Apply State
        canvasView.elements.clear()
        canvasView.elements.addAll(state.elements)
        canvasView.baseColor = state.baseColor
        canvasView.invalidate()
    }

    fun loadSkinForEditing(truck: TruckModel, skinPath: String) {
        currentTruck = truck
        currentProjectId = null // New Project
        currentProjectName = null
        
        tvTruckName.text = "Editing: ${truck.displayName}"
        btnChangeModel.visibility = View.VISIBLE
        
        // FULL RESET - Clear everything from previous session
        undoStack.clear()
        redoStack.clear()
        canvasView.elements.clear()
        canvasView.selectedElement = null
        canvasView.baseColor = null
        canvasView.baseBitmap = null
        canvasView.originalBitmap = null
        
        // Load fresh Template
        loadTruckTemplate(truck.templateResource)
        
        // Elements already cleared above
        
        // Decode the skin bitmap from file
        val bitmap = try {
            BitmapFactory.decodeFile(skinPath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (bitmap != null) {
            // USE THE DOWNLOADED SKIN DIRECTLY AS THE BASE (no white template)
            // Make it mutable so we can draw stickers on it later
            val mutableSkin = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            
            // Set the skin as BOTH baseBitmap (display) and originalBitmap (export)
            canvasView.baseBitmap = mutableSkin
            canvasView.originalBitmap = mutableSkin
            canvasView.invalidate()
            
            Toast.makeText(context, "Skin loaded", Toast.LENGTH_SHORT).show()
        } else {
             Toast.makeText(context, "Failed to load skin image", Toast.LENGTH_SHORT).show()
        }
    }


    fun isProjectLoaded(): Boolean {
        return currentTruck != null && canvasView.baseBitmap != null
    }

    private fun loadTruckTemplate(templateResource: Int = R.drawable.template_stream_st) {
        // Load original FULL resolution for saving (game-ready texture)
        val originalOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inScaled = false
        }
        canvasView.originalBitmap = BitmapFactory.decodeResource(resources, templateResource, originalOptions)
        
        // Load downsampled version for screen display (performance)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(resources, templateResource, options)

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

        canvasView.baseBitmap = BitmapFactory.decodeResource(resources, templateResource, decodeOptions)
        canvasView.invalidate()
    }

    private fun setupListeners() {
        // Capture state before modification starts (Drag, Resize, Rotate)
        canvasView.interactionListener = object : CanvasView.OnInteractionListener {
            override fun onModificationStart() {
                saveState()
            }
        }

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
        
        btnChangeModel.setOnClickListener {
            // Show confirmation if there are unsaved elements
            if (canvasView.elements.isNotEmpty() || canvasView.baseColor != null) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Change Truck Model?")
                    .setMessage("Changing the truck model will clear all your current work. Continue?")
                    .setPositiveButton("Yes") { _, _ ->
                        showTruckSelectionDialog()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                showTruckSelectionDialog()
            }
        }
    }
    
    private fun showTruckSelectionDialog() {
        val dialog = TruckSelectionDialog { selectedTruck ->
            loadTruck(selectedTruck)
        }
        dialog.show(childFragmentManager, "TruckSelection")
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
        val bottomSheet = StickerBottomSheet(
            onLocalStickerSelected = { stickerRes ->
                if (stickerRes == R.drawable.ic_upload) {
                    pickImageLauncher.launch("image/*")
                } else {
                    saveState()
                    addSticker(stickerRes)
                }
            },
            onRemoteStickerSelected = { stickerUrl ->
                saveState()
                addRemoteSticker(stickerUrl)
            }
        )
        bottomSheet.show(childFragmentManager, "StickerPicker")
    }

    private fun showTextEditor() {
        val dialog = TextEditorDialog { text, size, color, font ->
            saveState()
            addText(text, size, color, font)
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

    private fun addRemoteSticker(imageUrl: String) {
        // Use Glide to load the image from URL
        com.bumptech.glide.Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(
                    resource: android.graphics.Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                ) {
                    // Scale down if too large
                    val maxDimension = 1200
                    var width = resource.width
                    var height = resource.height
                    val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                        val ratio = width.toFloat() / height.toFloat()
                        if (width > height) {
                            width = maxDimension
                            height = (width / ratio).toInt()
                        } else {
                            height = maxDimension
                            width = (height * ratio).toInt()
                        }
                        android.graphics.Bitmap.createScaledBitmap(resource, width, height, true)
                    } else {
                        resource
                    }

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

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                
                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    Toast.makeText(requireContext(), "Failed to load sticker", Toast.LENGTH_SHORT).show()
                }
            })
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

    private fun addText(text: String, textSize: Float, textColor: Int, fontFamily: String = "sans-serif") {
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
            fontFamily = fontFamily,
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
            baseColor = canvasView.baseColor,
            baseBitmap = canvasView.baseBitmap
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

    private var currentProjectName: String? = null

    private fun saveSkin() {
        if (currentTruck == null) return

        val container = android.widget.LinearLayout(requireContext())
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(50, 40, 50, 10)

        val input = android.widget.EditText(requireContext())
        input.hint = "My Awesome Skin"
        input.setText(currentProjectName ?: "") // Pre-fill name
        container.addView(input)

        val checkNew = android.widget.CheckBox(requireContext())
        checkNew.text = "Save as copy"
        
        // Only show "Save as copy" if we are already editing an existing project
        if (currentProjectId != null) {
            checkNew.isChecked = false
            container.addView(checkNew)
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(if(currentProjectId != null) "Update Project" else "Save Project")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifBlank { "Untitled Skin" }
                currentProjectName = name
                
                val currentState = CanvasState(
                    elements = canvasView.elements.toList(),
                    baseColor = canvasView.baseColor,
                    baseBitmap = canvasView.baseBitmap // Save the background!
                )
                
                // If "Save as copy" is checked, we force a new ID (null)
                val targetProjectId = if (checkNew.isChecked) null else currentProjectId
                
                // Generate Thumbnail
                val thumbnail = canvasView.generateHighResBitmap() ?: return@setPositiveButton
                
                // Run on background thread
                Thread {
                    val metadata = projectManager.saveProject(
                        projectId = targetProjectId,
                        name = name,
                        truck = currentTruck!!,
                        canvasState = currentState,
                        thumbnail = thumbnail
                    )
                    
                    requireActivity().runOnUiThread {
                        currentProjectId = metadata.id
                        currentProjectName = metadata.name // Update local name
                        Toast.makeText(requireContext(), "Project saved!", Toast.LENGTH_SHORT).show()
                        // Show ad after saving project
                        AdManager.onUserAction(requireActivity())
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}
