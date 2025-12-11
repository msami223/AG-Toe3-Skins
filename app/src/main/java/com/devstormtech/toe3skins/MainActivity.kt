package com.devstormtech.toe3skins

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var skinMakerFragment: SkinMakerFragment
    private lateinit var settingsFragment: SettingsFragment

    private val PREFS_NAME = "TOE3SkinsPrefs"
    private val KEY_THEME = "app_theme"
    private val KEY_SKIN_EDITOR_ACTIVATED = "skin_editor_activated"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Restore saved theme before super.onCreate
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // Fresh start: create new fragments
            homeFragment = HomeFragment()
            skinMakerFragment = SkinMakerFragment()
            settingsFragment = SettingsFragment()
            
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, skinMakerFragment, "skin_maker")
                .add(R.id.fragment_container, settingsFragment, "settings")
                .hide(skinMakerFragment)
                .hide(settingsFragment)
                .commitNow()
        } else {
            // Restore fragments from fragment manager after activity recreation (e.g., theme change)
            val existingHome = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
            val existingSkinMaker = supportFragmentManager.findFragmentByTag("skin_maker") as? SkinMakerFragment
            val existingSettings = supportFragmentManager.findFragmentByTag("settings") as? SettingsFragment
            
            if (existingHome != null && existingSkinMaker != null && existingSettings != null) {
                // All fragments found, use them
                homeFragment = existingHome
                skinMakerFragment = existingSkinMaker
                settingsFragment = existingSettings
            } else {
                // Fragments not found (can happen during rapid theme switching), recreate them
                homeFragment = existingHome ?: HomeFragment()
                skinMakerFragment = existingSkinMaker ?: SkinMakerFragment()
                settingsFragment = existingSettings ?: SettingsFragment()
                
                // Re-add any missing fragments
                val transaction = supportFragmentManager.beginTransaction()
                if (existingHome == null) {
                    transaction.add(R.id.fragment_container, homeFragment, "home")
                }
                if (existingSkinMaker == null) {
                    transaction.add(R.id.fragment_container, skinMakerFragment, "skin_maker")
                    transaction.hide(skinMakerFragment)
                }
                if (existingSettings == null) {
                    transaction.add(R.id.fragment_container, settingsFragment, "settings")
                    transaction.hide(settingsFragment)
                }
                transaction.commitNow()
            }
        }

        setupBottomNavigation()
        
        // Hide skin editor tab if not yet activated
        val isSkinEditorActivated = prefs.getBoolean(KEY_SKIN_EDITOR_ACTIVATED, false)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.menu.findItem(R.id.nav_skin_maker).isVisible = isSkinEditorActivated
        
        // Handle notification click intent
        handleNotificationIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle notification click when app is already running
        intent?.let { handleNotificationIntent(it) }
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        val targetTab = intent.getStringExtra("TARGET_TAB")
        if (targetTab != null) {
            // Clear the extra to prevent re-handling on config changes
            intent.removeExtra("TARGET_TAB")
            
            // Apply the filter to HomeFragment
            homeFragment.setInitialFilter(targetTab)
            
            // Ensure we're on the home tab
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            // Check if fragments are attached before proceeding
            if (!homeFragment.isAdded || !skinMakerFragment.isAdded || !settingsFragment.isAdded) {
                return@setOnItemSelectedListener false
            }
            
            val transaction = supportFragmentManager.beginTransaction()
            
            // Hide all fragments first
            transaction.hide(homeFragment)
            transaction.hide(skinMakerFragment)
            transaction.hide(settingsFragment)
            
            when (item.itemId) {
                R.id.nav_home -> {
                    transaction.show(homeFragment)
                    transaction.commitNowAllowingStateLoss()
                    true
                }
                R.id.nav_skin_maker -> {
                    transaction.show(skinMakerFragment)
                    transaction.commitNowAllowingStateLoss()
                    true
                }
                R.id.nav_settings -> {
                    transaction.show(settingsFragment)
                    transaction.commitNowAllowingStateLoss()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    fun switchToSkinEditor() {
        // Show truck selection dialog first
        val dialog = TruckSelectionDialog { selectedTruck ->
            // Mark skin editor as activated
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_SKIN_EDITOR_ACTIVATED, false)) {
                prefs.edit().putBoolean(KEY_SKIN_EDITOR_ACTIVATED, true).apply()
            }
            
            // Show the skin editor tab in bottom navigation
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.menu.findItem(R.id.nav_skin_maker).isVisible = true
            
            // Load the truck and switch to skin maker tab
            skinMakerFragment.loadTruck(selectedTruck)
            bottomNav.selectedItemId = R.id.nav_skin_maker
        }
        dialog.show(supportFragmentManager, "TruckSelection")
    }
}