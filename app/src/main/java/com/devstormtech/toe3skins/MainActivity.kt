package com.devstormtech.toe3skins

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment

    private lateinit var skinMakerFragment: SkinMakerFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var myProjectsFragment: MyProjectsFragment

    // In-App Update
    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_REQUEST_CODE = 1001

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
            myProjectsFragment = MyProjectsFragment()
            
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, skinMakerFragment, "skin_maker")
                .add(R.id.fragment_container, settingsFragment, "settings")
                .add(R.id.fragment_container, myProjectsFragment, "my_projects")
                .hide(skinMakerFragment)
                .hide(settingsFragment)
                .hide(myProjectsFragment)
                .commitNow()
        } else {
            // Restore fragments from fragment manager after activity recreation (e.g., theme change)
            val existingHome = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
            val existingSkinMaker = supportFragmentManager.findFragmentByTag("skin_maker") as? SkinMakerFragment
            val existingSettings = supportFragmentManager.findFragmentByTag("settings") as? SettingsFragment
            val existingMyProjects = supportFragmentManager.findFragmentByTag("my_projects") as? MyProjectsFragment
            
            if (existingHome != null && existingSkinMaker != null && existingSettings != null && existingMyProjects != null) {
                // All fragments found, use them
                homeFragment = existingHome
                skinMakerFragment = existingSkinMaker
                settingsFragment = existingSettings
                myProjectsFragment = existingMyProjects
            } else {
                // Fragments not found (can happen during rapid theme switching), recreate them
                homeFragment = existingHome ?: HomeFragment()
                skinMakerFragment = existingSkinMaker ?: SkinMakerFragment()
                settingsFragment = existingSettings ?: SettingsFragment()
                myProjectsFragment = existingMyProjects ?: MyProjectsFragment()
                
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
                if (existingMyProjects == null) {
                    transaction.add(R.id.fragment_container, myProjectsFragment, "my_projects")
                    transaction.hide(myProjectsFragment)
                }
                transaction.commitNow()
            }
        }



        // Initialize In-App Update Manager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdate()

        // Initialize AdManager
        // Initialize AdManager - Moved to ConsentManager callback below
        
        // Initialize Analytics
        AnalyticsManager.initialize(this)
        AnalyticsManager.logAppOpen()
        
        // GDPR / Consent Gathering
        ConsentManager.gatherConsent(this) { canRequestAds ->
            if (canRequestAds) {
                Log.d("MainActivity", "Consent gathered, initializing Ads")
                
                // Initialize AdMob
                AdManager.initialize(this)
                
                // Initialize Yandex Ads (Required for Mediation)
                com.yandex.mobile.ads.common.MobileAds.initialize(this) { 
                     Log.d("YandexAds", "Yandex Mobile Ads initialized")
                }
            } else {
                 Log.d("MainActivity", "Cannot request ads (Consent not granted or error)")
            }
        }

        setupProjectLoading()
        setupBottomNavigation()
        
        // Hide skin editor tab if not yet activated
        val isSkinEditorActivated = prefs.getBoolean(KEY_SKIN_EDITOR_ACTIVATED, false)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.menu?.findItem(R.id.nav_skin_maker)?.isVisible = isSkinEditorActivated
        
        // Handle notification click intent (single call - removed duplicates)
        handleNotificationIntent(intent)
        handleNavigationIntent(intent)
        handleEditSkinIntent(intent)
    }

    private fun checkForUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                
                // Request IMMEDIATE update - user cannot use app until they update
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Update flow failed: ${e.message}")
                }
            }
        }

        appUpdateInfoTask.addOnFailureListener { e ->
            Log.e("MainActivity", "Failed to check for updates: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check if an IMMEDIATE update is still pending (user pressed back)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Force the update to restart if user tried to cancel
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Resume update flow failed: ${e.message}")
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // Update failed or was cancelled - check again
                Log.e("MainActivity", "Update failed! Result code: $resultCode")
                // Re-check to force the update
                checkForUpdate()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification click when app is already running

        handleNotificationIntent(intent)
        handleNavigationIntent(intent)
        handleEditSkinIntent(intent)
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

    private fun handleNavigationIntent(intent: Intent) {
        val targetNavId = intent.getIntExtra("TARGET_NAV_ID", -1)
        if (targetNavId != -1) {
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = targetNavId
            intent.removeExtra("TARGET_NAV_ID")
        }
    }

    private fun handleEditSkinIntent(intent: Intent) {
        if (intent.action == "ACTION_EDIT_SKIN") {
            val imagePath = intent.getStringExtra("IMAGE_PATH")
            val truckName = intent.getStringExtra("TRUCK_MODEL_NAME")
            
            if (imagePath != null && truckName != null) {
                // Find Truck by Name (simple check for now)
                val truck = TruckModel.getAllTrucks().find { 
                    it.displayName.equals(truckName, ignoreCase = true) || 
                    it.id.equals(truckName, ignoreCase = true) 
                } ?: TruckModel.getAllTrucks().first() // Fallback to first if not found

                // Mark skin editor as activated
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_SKIN_EDITOR_ACTIVATED, true).apply()
                
                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav.menu.findItem(R.id.nav_skin_maker).isVisible = true
                
                // Load into Fragment FIRST so isProjectLoaded() returns true
                skinMakerFragment.loadSkinForEditing(truck, imagePath)
                
                // Switch Tab (now isProjectLoaded will be true)
                bottomNav.selectedItemId = R.id.nav_skin_maker
                
                // Clear action so it doesn't run again on rotate
                intent.action = ""
            }
        }
    }

    private fun setupBottomNavigation() {
        // FIX: Add null safety check for BottomNavigationView crash
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (bottomNav == null) {
            Log.e("MainActivity", "BottomNavigationView not found in layout!")
            return
        }
        
        bottomNav.setOnItemSelectedListener { item ->
            // FIX: Wrap in try-catch to prevent crashes during rapid state changes
            try {
                // Check if fragments are initialized and attached before proceeding
                if (!::homeFragment.isInitialized || !::skinMakerFragment.isInitialized || 
                    !::settingsFragment.isInitialized || !::myProjectsFragment.isInitialized) {
                    Log.w("MainActivity", "Fragments not yet initialized")
                    return@setOnItemSelectedListener false
                }
                
                if (!homeFragment.isAdded || !skinMakerFragment.isAdded || !settingsFragment.isAdded) {
                    return@setOnItemSelectedListener false
                }
                
                val transaction = supportFragmentManager.beginTransaction()
                
                // Hide all fragments first
                transaction.hide(homeFragment)
                transaction.hide(skinMakerFragment)
                transaction.hide(settingsFragment)
                transaction.hide(myProjectsFragment)
                
                when (item.itemId) {
                    R.id.nav_home -> {
                        transaction.show(homeFragment)
                        transaction.commitNowAllowingStateLoss()
                        true
                    }
                    R.id.nav_skin_maker -> {
                        if (skinMakerFragment.isProjectLoaded()) {
                            transaction.show(skinMakerFragment)
                            transaction.commitNowAllowingStateLoss()
                            true
                        } else {
                            // Project not loaded, force selection
                            switchToSkinEditor()
                            false // Don't select the tab yet
                        }
                    }
                    R.id.nav_settings -> {
                        transaction.show(settingsFragment)
                        transaction.commitNowAllowingStateLoss()
                        true
                    }
                    R.id.nav_my_projects -> {
                        transaction.show(myProjectsFragment)
                        transaction.commitNowAllowingStateLoss()
                        true
                    }
                    else -> {
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in bottom navigation: ${e.message}")
                false
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
            
            // Now programmatically select the tab, which will trigger the listener again
            // prompting the "isProjectLoaded" check which will now be True
            bottomNav.selectedItemId = R.id.nav_skin_maker
        }
        dialog.show(supportFragmentManager, "TruckSelection")
    }
    
    private fun setupProjectLoading() {
        myProjectsFragment.onProjectSelected = { projectId ->
            val projectManager = ProjectManager(this)
            val (truck, state, name) = projectManager.loadProject(projectId)
            
            if (truck != null) {
                // Track user action for interstitial ads (loading project = 1 action)
                AdManager.onUserAction(this)
                
                // Switch to SkinMakerFragment
                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                
                // Allow SkinEditor to be visible if it wasn't
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (!prefs.getBoolean(KEY_SKIN_EDITOR_ACTIVATED, false)) {
                     prefs.edit().putBoolean(KEY_SKIN_EDITOR_ACTIVATED, true).apply()
                     bottomNav.menu.findItem(R.id.nav_skin_maker).isVisible = true
                }

                // Load Project
                skinMakerFragment.loadProject(truck, state, projectId, name)
                
                // Switch Tab
                bottomNav.selectedItemId = R.id.nav_skin_maker
                
            } else {
                android.widget.Toast.makeText(this, "Failed to load project", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}