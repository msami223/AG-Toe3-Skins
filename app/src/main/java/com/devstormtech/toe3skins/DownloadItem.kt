package com.devstormtech.toe3skins

data class DownloadItem(
    val filename: String,
    val timestamp: Long,
    val filePath: String,
    val source: String // "WordPress" or "Editor"
)
