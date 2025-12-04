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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.devstormtech.toe3skins.api.RetrofitClient
import com.devstormtech.toe3skins.model.Skin
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {

    // Notification Toggle
    private lateinit var notificationBanner: CardView
    private lateinit var notificationSwitch: SwitchCompat

    private val NOTIFICATION_PERMISSION_CODE = 102
    private val PREFS_NAME = "TOE3SkinsPrefs"
    private val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

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

        val skin = intent.getSerializableExtra("SKIN_DATA") as? Skin

        if (skin != null) {
            // Increment View Count immediately
            incrementViewCount(skin.id)

            tvTitle.text = skin.title.rendered
            tvModel.text = skin.acf.truckModel
            tvCreator.text = "By ${skin.acf.creatorName}"
            tvDownloads.text = "${skin.acf.downloadCount}"
            tvViews.text = "${skin.acf.viewCount}"
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
        }
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
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Downloading $title")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val fileName = "${title.replace(" ", "_")}.png"
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Download Started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}