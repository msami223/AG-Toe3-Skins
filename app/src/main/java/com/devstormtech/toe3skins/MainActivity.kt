package com.devstormtech.toe3skins

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val skinMakerFragment = SkinMakerFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            // Initial setup: Add both fragments, hide skinMaker, show home
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, skinMakerFragment, "skin_maker")
                .hide(skinMakerFragment)
                .commit()
        } else {
            // Restore state if needed (often handled automatically by fragment manager)
            activeFragment = supportFragmentManager.findFragmentByTag("home") ?: homeFragment
            // Re-find the fragments to ensure we have the correct instances
            supportFragmentManager.findFragmentByTag("home")?.let {
                if (!it.isHidden) activeFragment = it
            }
            supportFragmentManager.findFragmentByTag("skin_maker")?.let {
                if (!it.isHidden) activeFragment = it
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .hide(skinMakerFragment)
                        .show(homeFragment)
                        .commit()
                    activeFragment = homeFragment
                    true
                }
                R.id.nav_skin_maker -> {
                    supportFragmentManager.beginTransaction()
                        .hide(homeFragment)
                        .show(skinMakerFragment)
                        .commit()
                    activeFragment = skinMakerFragment
                    true
                }
                else -> false
            }
        }
    }

    fun switchToSkinEditor() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_skin_maker
    }
}