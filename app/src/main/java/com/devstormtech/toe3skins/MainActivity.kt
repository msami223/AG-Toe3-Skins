package com.devstormtech.toe3skins

import android.content.Context
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
                .commit()
        } else {
            // Restore fragments from fragment manager
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment ?: HomeFragment()
            skinMakerFragment = supportFragmentManager.findFragmentByTag("skin_maker") as? SkinMakerFragment ?: SkinMakerFragment()
            settingsFragment = supportFragmentManager.findFragmentByTag("settings") as? SettingsFragment ?: SettingsFragment()
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val transaction = supportFragmentManager.beginTransaction()
            
            // Hide all fragments first
            transaction.hide(homeFragment)
            transaction.hide(skinMakerFragment)
            transaction.hide(settingsFragment)
            
            when (item.itemId) {
                R.id.nav_home -> {
                    transaction.show(homeFragment)
                    transaction.commit()
                    true
                }
                R.id.nav_skin_maker -> {
                    transaction.show(skinMakerFragment)
                    transaction.commit()
                    true
                }
                R.id.nav_settings -> {
                    transaction.show(settingsFragment)
                    transaction.commit()
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
            skinMakerFragment.loadTruck(selectedTruck)
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.nav_skin_maker
        }
        dialog.show(supportFragmentManager, "TruckSelection")
    }
}