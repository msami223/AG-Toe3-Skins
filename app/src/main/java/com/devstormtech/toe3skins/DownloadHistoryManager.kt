package com.devstormtech.toe3skins

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DownloadHistoryManager {
    private const val PREFS_NAME = "TOE3SkinsPrefs"
    private const val KEY_DOWNLOADS = "download_history"
    
    fun addDownload(context: Context, filename: String, filePath: String, source: String) {
        val downloads = getRecentDownloads(context, limit = 100).toMutableList()
        downloads.add(0, DownloadItem(filename, System.currentTimeMillis(), filePath, source))
        saveDownloads(context, downloads.take(100)) // Keep only last 100
    }
    
    fun getRecentDownloads(context: Context, limit: Int = 20): List<DownloadItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<DownloadItem>>() {}.type
            val allDownloads: List<DownloadItem> = Gson().fromJson(json, type)
            allDownloads.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveDownloads(context: Context, downloads: List<DownloadItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(downloads)
        prefs.edit().putString(KEY_DOWNLOADS, json).apply()
    }
}
