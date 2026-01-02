package com.devstormtech.toe3skins

data class ProjectMetadata(
    val id: String,
    val name: String,
    val truckId: String,
    val truckDisplayName: String,
    val lastModified: Long,
    val thumbnailPath: String
)

data class SerializedSticker(
    val id: String,
    val imagePath: String, // Relative to project folder
    val isExternal: Boolean, // True if loaded from URL/Gallery, False if from app resources
    val x: Float,
    val y: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float
)

data class SerializedText(
    val id: String,
    val text: String,
    val textColor: Int,
    val textSize: Float,
    val x: Float,
    val y: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float
)

data class SerializedProjectData(
    val version: Int = 1,
    val truckId: String,
    val baseColor: Int?,
    val stickers: List<SerializedSticker>,
    val texts: List<SerializedText>,
    val baseImagePath: String? = null // Optional custom background
)
