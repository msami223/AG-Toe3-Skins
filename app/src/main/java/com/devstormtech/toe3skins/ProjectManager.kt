package com.devstormtech.toe3skins

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectManager(private val context: Context) {

    private val projectsDir = File(context.filesDir, "projects")
    private val gson = Gson()

    init {
        if (!projectsDir.exists()) {
            projectsDir.mkdirs()
        }
    }

    fun getAllProjects(): List<ProjectMetadata> {
        val projects = mutableListOf<ProjectMetadata>()
        projectsDir.listFiles()?.forEach { projectFolder ->
            if (projectFolder.isDirectory) {
                val metadataFile = File(projectFolder, "metadata.json")
                if (metadataFile.exists()) {
                    try {
                        val json = metadataFile.readText()
                        val metadata = gson.fromJson(json, ProjectMetadata::class.java)
                        projects.add(metadata)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return projects.sortedByDescending { it.lastModified }
    }

    fun saveProject(
        projectId: String?,
        name: String,
        truck: TruckModel,
        canvasState: CanvasState,
        thumbnail: Bitmap
    ): ProjectMetadata {
        val id = projectId ?: UUID.randomUUID().toString()
        val projectFolder = File(projectsDir, id)
        if (!projectFolder.exists()) projectFolder.mkdirs()

        val imagesFolder = File(projectFolder, "images")
        if (!imagesFolder.exists()) imagesFolder.mkdirs()

        // 1. Save Thumbnail
        val thumbFile = File(projectFolder, "thumbnail.png")
        FileOutputStream(thumbFile).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.PNG, 80, out)
        }

        // 2. Serialize Stickers & Save Images
        val serializedStickers = canvasState.elements
            .filterIsInstance<CanvasElement.StickerElement>()
            .map { sticker ->
                val imageFileName = "sticker_${sticker.id}.png"
                val imageFile = File(imagesFolder, imageFileName)
                
                // Always save the current bitmap state to ensure we capture any edits or if it was new
                FileOutputStream(imageFile).use { out ->
                    sticker.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                SerializedSticker(
                    id = sticker.id,
                    imagePath = "images/$imageFileName",
                    isExternal = true, // We treat all saved stickers as external files now
                    x = sticker.x,
                    y = sticker.y,
                    scaleX = sticker.scaleX,
                    scaleY = sticker.scaleY,
                    rotation = sticker.rotation
                )
            }

        // 3. Serialize Text
        val serializedTexts = canvasState.elements
            .filterIsInstance<CanvasElement.TextElement>()
            .map { text ->
                SerializedText(
                    id = text.id,
                    text = text.text,
                    textColor = text.textColor,
                    textSize = text.textSize,
                    x = text.x,
                    y = text.y,
                    scaleX = text.scaleX,
                    scaleY = text.scaleY,
                    rotation = text.rotation
                )
            }

        // 3b. Save Base Bitmap if present (using CanvasState.baseBitmap if passed via state, but wait, 
        // saveProject currently doesn't check canvasState.baseBitmap because I just added it.
        // I need to use the passed canvasState)
        
        var baseImagePath: String? = null
        if (canvasState.baseBitmap != null) {
            val baseFile = File(imagesFolder, "base.png")
            FileOutputStream(baseFile).use { out ->
                canvasState.baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            baseImagePath = "images/base.png"
        }

        // 4. Save Data JSON
        val projectData = SerializedProjectData(
            truckId = truck.id,
            baseColor = canvasState.baseColor,
            stickers = serializedStickers,
            texts = serializedTexts,
            baseImagePath = baseImagePath
        )
        val dataFile = File(projectFolder, "data.json")
        dataFile.writeText(gson.toJson(projectData))

        // 5. Save Metadata
        val metadata = ProjectMetadata(
            id = id,
            name = name,
            truckId = truck.id,
            truckDisplayName = truck.displayName,
            lastModified = System.currentTimeMillis(),
            thumbnailPath = thumbFile.absolutePath
        )
        val metadataFile = File(projectFolder, "metadata.json")
        metadataFile.writeText(gson.toJson(metadata))

        return metadata
    }

    fun loadProject(projectId: String): Triple<TruckModel?, CanvasState, String> {
        val projectFolder = File(projectsDir, projectId)
        val dataFile = File(projectFolder, "data.json")
        val metadataFile = File(projectFolder, "metadata.json")
        
        if (!dataFile.exists()) return Triple(null, CanvasState(), "")

        try {
            // Read Name from Metadata
            var projectName = "Untitled"
            if (metadataFile.exists()) {
                 val metaJson = metadataFile.readText()
                 val metadata = gson.fromJson(metaJson, ProjectMetadata::class.java)
                 projectName = metadata.name
            }

            val json = dataFile.readText()
            val data = gson.fromJson(json, SerializedProjectData::class.java)

            // Find Truck Model (You'll need a way to look up TruckModel by ID)
            // For now, we'll try to find it in the hardcoded list in TruckSelectionDialog or pass a lookup
            // But since TruckModel is a simple data class, we can probably reconstruct it or find it.
            // Let's assume the caller will handle finding the TruckModel if we just return the ID, 
            // OR better, let's use the static list we have.
            // Since we don't have a central TruckRepository yet, we will look it up from the known list.
            val truck = TruckModel.getAllTrucks().find { it.id == data.truckId }

            val elements = mutableListOf<CanvasElement>()

            // Load Stickers
            data.stickers.forEach { sStick ->
                val imageFile = File(projectFolder, sStick.imagePath)
                if (imageFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        elements.add(CanvasElement.StickerElement(
                            id = sStick.id,
                            bitmap = bitmap,
                            x = sStick.x,
                            y = sStick.y,
                            scaleX = sStick.scaleX,
                            scaleY = sStick.scaleY,
                            rotation = sStick.rotation,
                            isSelected = false
                        ))
                    }
                }
            }

            // Load Texts
            data.texts.forEach { sText ->
                // Recalculate measurements
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = sText.textSize
                }
                val measuredWidth = paint.measureText(sText.text)
                
                elements.add(CanvasElement.TextElement(
                    id = sText.id,
                    text = sText.text,
                    textColor = sText.textColor,
                    textSize = sText.textSize,
                    x = sText.x,
                    y = sText.y,
                    scaleX = sText.scaleX,
                    scaleY = sText.scaleY,
                    rotation = sText.rotation,
                    isSelected = false,
                    measuredWidth = measuredWidth,
                    measuredHeight = sText.textSize // Approx
                ))
            }

            // Load Base Bitmap if exists
            var baseBitmap: Bitmap? = null
            if (!data.baseImagePath.isNullOrEmpty()) {
                val baseFile = File(projectFolder, data.baseImagePath)
                if (baseFile.exists()) {
                    val decoded = BitmapFactory.decodeFile(baseFile.absolutePath)
                    // Ensure it's mutable if we ever want to draw on it again? 
                    // CanvasState just holds it. The fragment will set it to view.
                    // If View needs mutable, it should copy. But BitmapFactory.decodeFile generally returns immutable.
                    // Let's return it as is.
                    baseBitmap = decoded
                }
            }

            return Triple(truck, CanvasState(elements, data.baseColor, baseBitmap), projectName)

        } catch (e: Exception) {
            e.printStackTrace()
            return Triple(null, CanvasState(), "")
        }
    }
    
    fun deleteProject(projectId: String) {
        val projectFolder = File(projectsDir, projectId)
        if (projectFolder.exists()) {
            projectFolder.deleteRecursively()
        }
    }
}
