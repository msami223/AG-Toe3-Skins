package com.devstormtech.toe3skins

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import adapter.DownloadsAdapter

class SettingsFragment : Fragment() {

    private val PREFS_NAME = "TOE3SkinsPrefs"
    private val KEY_THEME = "app_theme"
    
    private lateinit var recyclerDownloads: RecyclerView
    private lateinit var txtNoDownloads: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val radioLight = view.findViewById<RadioButton>(R.id.radioLight)
        val radioDark = view.findViewById<RadioButton>(R.id.radioDark)
        val radioSystem = view.findViewById<RadioButton>(R.id.radioSystem)

        // Load saved theme preference
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> radioDark.isChecked = true
            else -> radioSystem.isChecked = true
        }

        // Theme change listener
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            prefs.edit().putInt(KEY_THEME, mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        
        // Setup downloads RecyclerView
        recyclerDownloads = view.findViewById(R.id.recyclerDownloads)
        txtNoDownloads = view.findViewById(R.id.txtNoDownloads)
        
        recyclerDownloads.layoutManager = LinearLayoutManager(requireContext())
        
        loadRecentDownloads()
    }
    
    override fun onResume() {
        super.onResume()
        loadRecentDownloads() // Refresh list when returning to Settings
    }
    
    private fun loadRecentDownloads() {
        val downloads = DownloadHistoryManager.getRecentDownloads(requireContext(), limit = 20)
        
        if (downloads.isEmpty()) {
            txtNoDownloads.visibility = View.VISIBLE
            recyclerDownloads.visibility = View.GONE
        } else {
            txtNoDownloads.visibility = View.GONE
            recyclerDownloads.visibility = View.VISIBLE
            
            val adapter = DownloadsAdapter(downloads) { downloadItem ->
                openDownloadedFile(downloadItem)
            }
            recyclerDownloads.adapter = adapter
        }
    }
    
    private fun openDownloadedFile(item: DownloadItem) {
        try {
            // Try to open the file with default image viewer
            val intent = Intent(Intent.ACTION_VIEW).apply {
                // For URIs starting with "content://"
                if (item.filePath.startsWith("content://")) {
                    setDataAndType(Uri.parse(item.filePath), "image/*")
                } else {
                    // For file paths, we need to use a different approach
                    Toast.makeText(
                        requireContext(),
                        "File: ${item.filename}\nLocation: Pictures/TOE3Skins",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Could not open file: ${item.filename}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
