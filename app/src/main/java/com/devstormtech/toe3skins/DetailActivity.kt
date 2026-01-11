package com.devstormtech.toe3skins

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devstormtech.toe3skins.adapter.RelatedSkinsAdapter
import com.devstormtech.toe3skins.api.RetrofitClient
import com.devstormtech.toe3skins.model.Skin
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {

    private var currentSkin: Skin? = null
    private lateinit var relatedSkinsAdapter: RelatedSkinsAdapter
    private lateinit var rvRelatedSkins: RecyclerView
    private lateinit var tvRelatedSkinsTitle: TextView
    private lateinit var btnLike: ImageButton
    private lateinit var tvDetailLikes: TextView
    private var currentLikeCount: Int = 0

    // Notification Toggle
    private lateinit var notificationBanner: CardView
    private lateinit var notificationSwitch: SwitchCompat

    private val NOTIFICATION_PERMISSION_CODE = 102
    private val PREFS_NAME = "TOE3SkinsPrefs"
    private val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private val KEY_LIKED_SKINS = "liked_skins"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        
        // Track user action for interstitial ads (opening skin detail counts as 1 action)
        AdManager.onUserAction(this)

        // 1. SETUP HEADER
        val btnBack: ImageView = findViewById(R.id.btnHeaderBack)
        val etSearch: EditText = findViewById(R.id.etHeaderSearch)
        val btnSort: ImageView = findViewById(R.id.btnHeaderSort)

        // Show Back Button, Hide Sort
        btnBack.visibility = View.VISIBLE
        btnSort.visibility = View.GONE
        btnBack.setOnClickListener { finish() }

        // Initialize Notification Toggle Views
        notificationBanner = findViewById(R.id.cardNotificationPermission)
        notificationSwitch = findViewById(R.id.switchNotifications)

        // Setup Notification Toggle
        setupNotificationToggle()

        // Setup Bottom Navigation
        setupBottomNavigation()

        // --- UPDATED SEARCH LOGIC ---
        // Allow user to type, then go to Main Activity when they press "Search" on keyboard
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotEmpty()) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Clear stack to go back to Main
                    intent.putExtra("SEARCH_QUERY", query)
                    startActivity(intent)
                }
                true // We handled the event
            } else {
                false
            }
        }

        // 2. SETUP DETAIL VIEWS
        val ivPreview: ImageView = findViewById(R.id.ivDetailPreview)
        val tvTitle: TextView = findViewById(R.id.tvDetailTitle)
        val tvModel: TextView = findViewById(R.id.tvDetailModel)
        val tvCreator: TextView = findViewById(R.id.tvDetailCreator)
        val tvDownloads: TextView = findViewById(R.id.tvDetailDownloads)
        val tvViews: TextView = findViewById(R.id.tvDetailViews)
        val tvInstructions: TextView = findViewById(R.id.tvInstructions)
        val containerTags: LinearLayout = findViewById(R.id.containerTags)
        val btnDownload: Button = findViewById(R.id.btnDownload)
        val btnShare: ImageButton = findViewById(R.id.btnShare)
        
        // Related Skins Setup
        rvRelatedSkins = findViewById(R.id.rvRelatedSkins)
        tvRelatedSkinsTitle = findViewById(R.id.tvRelatedSkinsTitle)
        rvRelatedSkins.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        relatedSkinsAdapter = RelatedSkinsAdapter(emptyList()) { relatedSkin ->
            // Open the related skin's detail page
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("SKIN_DATA", relatedSkin)
            startActivity(intent)
        }
        rvRelatedSkins.adapter = relatedSkinsAdapter

        val skin = intent.getSerializableExtra("SKIN_DATA") as? Skin
        currentSkin = skin

        if (skin != null) {
            // Increment View Count immediately
            incrementViewCount(skin.id)

            tvTitle.text = skin.title.rendered
            tvModel.text = skin.acf.truckModel
            tvCreator.text = "By ${skin.acf.creatorName}"
            tvDownloads.text = "${skin.acf.downloadCount}"
            tvViews.text = "${skin.acf.viewCount}"
            
            // Likes count
            tvDetailLikes = findViewById(R.id.tvDetailLikes)
            currentLikeCount = skin.acf.likeCount
            tvDetailLikes.text = "$currentLikeCount"
            
            tvInstructions.text = if (skin.acf.instructions.isNotEmpty()) skin.acf.instructions else "No instructions provided."

            if (skin.acf.previewImage1.isNotEmpty()) {
                Glide.with(this).load(skin.acf.previewImage1).into(ivPreview)
            }

            // Tags Logic
            val tagsList = skin.acf.tags ?: emptyList()
            containerTags.removeAllViews()
            for (tag in tagsList) {
                val chip = TextView(this)
                chip.text = tag
                chip.setTextColor(resources.getColor(android.R.color.white))
                chip.textSize = 12f
                chip.setPadding(30, 15, 30, 15)
                chip.setBackgroundResource(R.drawable.bg_tag_chip)

                val params = LinearLayout.LayoutParams(-2, -2)
                params.marginEnd = 16
                chip.layoutParams = params

                chip.setOnClickListener {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.putExtra("SEARCH_QUERY", tag)
                    startActivity(intent)
                }
                containerTags.addView(chip)
            }

            // Download Logic
            btnDownload.setOnClickListener {
                downloadSkin(skin.acf.skinFileUrl, skin.title.rendered)
                incrementDownloadCount(skin.id)
            }
            
            // Like Logic
            btnLike = findViewById(R.id.btnLike)
            updateLikeButtonState(skin.id)
            btnLike.setOnClickListener {
                toggleLike(skin)
            }
            
            // Share Logic
            btnShare.setOnClickListener {
                shareSkin(skin)
            }
            
            // Load Related Skins
            loadRelatedSkins(skin)

            // Edit Skin Logic
            val btnEditSkin: Button = findViewById(R.id.btnEditSkin)
            btnEditSkin.setOnClickListener {
                downloadAndEditSkin(skin.acf.skinFileUrl, skin.title.rendered, skin.acf.truckModel)
            }
            
            // Log Screen View
            AnalyticsManager.logScreenView("Detail_Screen", "DetailActivity")
        }
    }

    private fun downloadAndEditSkin(url: String, title: String, truckModelName: String) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Skin file not available", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Preparing Editor...", Toast.LENGTH_SHORT).show()

        // We download the image to a temporary file, then pass URI to MainActivity
        Thread {
            try {
                val bitmap = Glide.with(this)
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get()

                // Save to cache directory
                val filename = "temp_edit_${System.currentTimeMillis()}.png"
                val file = java.io.File(cacheDir, filename)
                val out = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()

                runOnUiThread {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        action = "ACTION_EDIT_SKIN"
                        putExtra("IMAGE_PATH", file.absolutePath)
                        putExtra("TRUCK_MODEL_NAME", truckModelName)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish() // Close detail activity
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load skin for editing", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun setupNotificationToggle() {
        // Check current permission status and update toggle
        updateNotificationToggle()
    }

    private fun updateNotificationToggle() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val userWantsNotifications = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true) // Default ON

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            // Toggle is ON only if: user wants it AND permission is granted
            val shouldBeChecked = userWantsNotifications && isPermissionGranted

            // Update toggle without triggering listener
            notificationSwitch.setOnCheckedChangeListener(null)
            notificationSwitch.isChecked = shouldBeChecked

            // Set up the listener
            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Save user preference
                    prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, true).apply()

                    if (!isPermissionGranted) {
                        // Need to request permission
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_CODE
                        )
                    } else {
                        // Already have permission, just subscribe
                        subscribeToNotifications()
                    }
                } else {
                    // User turned OFF - save preference and unsubscribe
                    prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply()
                    unsubscribeFromNotifications()
                }
            }
        } else {
            // Android 12 and below - no permission needed, just check user preference
            notificationSwitch.setOnCheckedChangeListener(null)
            notificationSwitch.isChecked = userWantsNotifications

            // Set up the listener
            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Save user preference
                prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply()

                if (isChecked) {
                    subscribeToNotifications()
                } else {
                    unsubscribeFromNotifications()
                }
            }
        }
    }

    private fun subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnSuccessListener {
                Toast.makeText(this, "✓ Notifications Enabled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to enable notifications", Toast.LENGTH_SHORT).show()
                updateNotificationToggle()
            }
    }

    private fun unsubscribeFromNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
            .addOnSuccessListener {
                Toast.makeText(this, "✗ Notifications Disabled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to disable notifications", Toast.LENGTH_SHORT).show()
                updateNotificationToggle()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - subscribe to notifications
                subscribeToNotifications()
                updateNotificationToggle()
            } else {
                // Permission denied - turn toggle OFF and save preference
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply()

                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                updateNotificationToggle()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update toggle state when returning to activity
        updateNotificationToggle()
    }

    private fun incrementViewCount(id: Int) {
        RetrofitClient.instance.incrementView(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun incrementDownloadCount(id: Int) {
        RetrofitClient.instance.incrementDownload(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun downloadSkin(url: String, title: String) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Link missing", Toast.LENGTH_SHORT).show()
            return
        }

        // Show Rewarded Ad first
        AdManager.showRewardedAd(this) { rewardEarned ->
            if (rewardEarned) {
                // User earned reward - Proceed with download
                performDownload(url, title)
            } else {
                 Toast.makeText(this, "Watch ad to download!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performDownload(url: String, title: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Downloading $title")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val fileName = "${title.replace(" ", "_")}.png"
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            
            // Track download in history
            val downloadPath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
            DownloadHistoryManager.addDownload(
                context = this,
                filename = fileName,
                filePath = downloadPath,
                source = "WordPress"
            )
            
            Toast.makeText(this, "Download Started...", Toast.LENGTH_SHORT).show()
            
            // Log Analytics
            AnalyticsManager.logSkinDownloaded(title, currentSkin?.acf?.truckModel ?: "Unknown")
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareSkin(skin: Skin) {
        val shareText = buildString {
            append("🎨 Check out this awesome ${skin.acf.truckModel} skin!\n\n")
            append("📦 ${skin.title.rendered}\n")
            append("👤 By ${skin.acf.creatorName}\n\n")
            append("⬇️ ${skin.acf.downloadCount} downloads\n")
            append("👁️ ${skin.acf.viewCount} views\n\n")
            append("Get more skins on TOE3 Skins app! 🚚")
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "TOE3 Skin: ${skin.title.rendered}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share Skin via"))
    }
    
    private fun loadRelatedSkins(currentSkin: Skin) {
        RetrofitClient.instance.getSkins().enqueue(object : Callback<List<Skin>> {
            override fun onResponse(call: Call<List<Skin>>, response: Response<List<Skin>>) {
                if (response.isSuccessful && response.body() != null) {
                    val allSkins = response.body() ?: return
                    
                    // Filter: same truck model, exclude current skin, limit to 6
                    val relatedSkins = allSkins
                        .filter { it.id != currentSkin.id && 
                                  it.acf.truckModel.equals(currentSkin.acf.truckModel, ignoreCase = true) }
                        .take(6)
                    
                    if (relatedSkins.isNotEmpty()) {
                        tvRelatedSkinsTitle.visibility = View.VISIBLE
                        rvRelatedSkins.visibility = View.VISIBLE
                        relatedSkinsAdapter.updateSkins(relatedSkins)
                    }
                }
            }
            
            override fun onFailure(call: Call<List<Skin>>, t: Throwable) {
                // Silently fail - related skins section just won't show
            }
        })
    }
    
    // ========== LIKE FUNCTIONALITY ==========
    
    private fun toggleLike(skin: Skin) {
        val skinId = skin.id
        if (isLiked(skinId)) {
            // Already liked - unlike it
            removeLikedSkin(skinId)
            btnLike.setImageResource(R.drawable.ic_heart_outline_24dp)
            
            // Decrement local count and UI
            if (currentLikeCount > 0) currentLikeCount--
            tvDetailLikes.text = "$currentLikeCount"
            
            // Call API to remove like
            decrementLikeCount(skinId)
            
            Toast.makeText(this, "Like removed", Toast.LENGTH_SHORT).show()
        } else {
            // Not liked - like it
            saveLikedSkin(skinId)
            btnLike.setImageResource(R.drawable.ic_heart_filled_24dp)
            
            // Increment local count and UI
            currentLikeCount++
            tvDetailLikes.text = "$currentLikeCount"
            
            // Call API to add like
            incrementLikeCount(skinId)
            
            Toast.makeText(this, "❤️ You liked this!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateLikeButtonState(skinId: Int) {
        if (isLiked(skinId)) {
            btnLike.setImageResource(R.drawable.ic_heart_filled_24dp)
        } else {
            btnLike.setImageResource(R.drawable.ic_heart_outline_24dp)
        }
    }
    
    private fun isLiked(skinId: Int): Boolean {
        val likedSkins = getLikedSkins()
        return likedSkins.contains(skinId.toString())
    }
    
    private fun saveLikedSkin(skinId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val likedSkins = getLikedSkins().toMutableSet()
        likedSkins.add(skinId.toString())
        prefs.edit().putStringSet(KEY_LIKED_SKINS, likedSkins).apply()
    }
    
    private fun removeLikedSkin(skinId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val likedSkins = getLikedSkins().toMutableSet()
        likedSkins.remove(skinId.toString())
        prefs.edit().putStringSet(KEY_LIKED_SKINS, likedSkins).apply()
    }
    
    private fun getLikedSkins(): Set<String> {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getStringSet(KEY_LIKED_SKINS, emptySet()) ?: emptySet()
    }
    
    private fun incrementLikeCount(id: Int) {
        RetrofitClient.instance.incrementLike(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun decrementLikeCount(id: Int) {
        RetrofitClient.instance.decrementLike(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Reset selection so no item is selected by default (optional, but looks better since we are on Detail)
        // bottomNav.menu.setGroupCheckable(0, false, true) 
        // Actually, we probably want to treat this as separate.
        
        bottomNav.setOnItemSelectedListener { item ->
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("TARGET_NAV_ID", item.itemId)
            startActivity(intent)
            overridePendingTransition(0, 0) // No animation for smoother feel
            true
        }
    }
}