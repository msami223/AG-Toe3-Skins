package com.devstormtech.toe3skins

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.devstormtech.toe3skins.adapter.SkinsAdapter
import com.devstormtech.toe3skins.adapter.TruckFilterAdapter
import com.devstormtech.toe3skins.api.RetrofitClient
import com.devstormtech.toe3skins.model.Skin
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var rvTruckFilters: RecyclerView
    private lateinit var errorLayout: View
    private lateinit var tvErrorTitle: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button
    private lateinit var etSearch: EditText
    private lateinit var btnSort: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var adapter: SkinsAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Notification Toggle
    private lateinit var notificationBanner: CardView
    private lateinit var notificationSwitch: SwitchCompat

    private var allSkins: List<Skin> = emptyList()
    private var currentFilterTruck = "All"

    private val NOTIFICATION_PERMISSION_CODE = 101
    private val PREFS_NAME = "TOE3SkinsPrefs"
    private val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Views
        shimmerContainer = findViewById(R.id.shimmerViewContainer)
        recyclerView = findViewById(R.id.rvSkins)
        rvTruckFilters = findViewById(R.id.rvTruckFilters)
        errorLayout = findViewById(R.id.errorLayout)
        tvErrorTitle = errorLayout.findViewById(R.id.tvErrorTitle)
        tvErrorMessage = errorLayout.findViewById(R.id.tvErrorMessage)
        btnRetry = errorLayout.findViewById(R.id.btnRetry)

        // Setup retry button
        btnRetry.setOnClickListener {
            fetchSkins(null, false)
        }

        etSearch = findViewById(R.id.etHeaderSearch)
        btnSort = findViewById(R.id.btnHeaderSort)
        btnBack = findViewById(R.id.btnHeaderBack)
        swipeRefreshLayout = findViewById(R.id.swipeRefresh)
        notificationBanner = findViewById(R.id.cardNotificationPermission)
        notificationSwitch = findViewById(R.id.switchNotifications)

        btnBack.visibility = View.GONE

        // 2. Setup Notification Toggle
        setupNotificationToggle()

        // 3. Notification Click
        val notificationTarget = intent.getStringExtra("TARGET_TAB")
        if (notificationTarget != null) {
            currentFilterTruck = notificationTarget
            Toast.makeText(this, "Viewing $notificationTarget Skins", Toast.LENGTH_LONG).show()
        }

        // 4. Truck Filters
        setupTruckFilters(currentFilterTruck)

        // 5. Skins Grid
        // Dynamic columns: 2 for phones, 5 for tablets
        val spanCount = resources.getInteger(R.integer.grid_columns)
        recyclerView.layoutManager = GridLayoutManager(this, spanCount)
        adapter = SkinsAdapter(emptyList()) { skin ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("SKIN_DATA", skin)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // 6. Pull to Refresh
        swipeRefreshLayout.setOnRefreshListener {
            fetchSkins(null, true)
        }

        // 7. Tag Search
        val tagQuery = intent.getStringExtra("SEARCH_QUERY")
        if (tagQuery != null) {
            etSearch.setText(tagQuery)
        }

        // 8. Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 9. Sort
        btnSort.setOnClickListener { view -> showSortMenu(view) }

        // 10. Fetch Data
        fetchSkins(tagQuery, false)
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

    private fun fetchSkins(initialQuery: String?, isRefreshing: Boolean) {
        if (!isRefreshing) {
            shimmerContainer.startShimmer()
            shimmerContainer.visibility = View.VISIBLE
        }
        recyclerView.visibility = View.GONE
        errorLayout.visibility = View.GONE

        RetrofitClient.instance.getSkins().enqueue(object : Callback<List<Skin>> {
            override fun onResponse(call: Call<List<Skin>>, response: Response<List<Skin>>) {
                if (isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    shimmerContainer.stopShimmer()
                    shimmerContainer.visibility = View.GONE
                }

                if (response.isSuccessful && response.body() != null) {
                    val skins = response.body()!!
                    if (skins.isNotEmpty()) {
                        allSkins = skins
                        if (!initialQuery.isNullOrEmpty()) {
                            applyFilters()
                        } else {
                            applyFilters()
                        }
                    } else {
                        showError("We are working hard on adding new skins! Check back soon.")
                    }
                } else {
                    showError("Server Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Skin>>, t: Throwable) {
                if (isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    shimmerContainer.stopShimmer()
                    shimmerContainer.visibility = View.GONE
                }
                showError("Network Error: ${t.message}")
            }
        })
    }

    private fun setupTruckFilters(initialSelection: String) {
        val trucks = listOf("All", "Stream", "Merieles", "Moon", "Volcano", "Dawn", "Fiora", "Renovate")
        rvTruckFilters.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val initialIndex = trucks.indexOfFirst { it.equals(initialSelection, ignoreCase = true) }
        val startPos = if (initialIndex >= 0) initialIndex else 0
        val filterAdapter = TruckFilterAdapter(trucks) { selectedTruck ->
            currentFilterTruck = selectedTruck
            applyFilters()
        }
        filterAdapter.setSelected(startPos)
        rvTruckFilters.adapter = filterAdapter
        rvTruckFilters.scrollToPosition(startPos)
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().lowercase()
        val filteredList = allSkins.filter { skin ->
            val matchesTruck = if (currentFilterTruck == "All") {
                true
            } else {
                skin.acf.truckModel.lowercase().contains(currentFilterTruck.lowercase())
            }
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                skin.title.rendered.lowercase().contains(query) ||
                        skin.acf.truckModel.lowercase().contains(query) ||
                        skin.acf.creatorName.lowercase().contains(query) ||
                        (skin.acf.tags?.any { it.lowercase().contains(query) } == true)
            }
            matchesTruck && matchesSearch
        }
        adapter.updateSkins(filteredList)
        if (filteredList.isEmpty()) {
            showError("We are working hard on adding new skins! Check back soon.")
        } else {
            errorLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showSortMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "Most Popular (Downloads)")
        popup.menu.add(0, 2, 1, "Most Viewed")
        popup.menu.add(0, 3, 2, "Newest First")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> sortSkinsBy { it.acf.downloadCount }
                2 -> sortSkinsBy { it.acf.viewCount }
                3 -> sortSkinsBy { it.id }
            }
            true
        }
        popup.show()
    }

    private fun sortSkinsBy(selector: (Skin) -> Int) {
        val sortedList = allSkins.sortedByDescending(selector)
        allSkins = sortedList
        applyFilters()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showError(message: String) {
        // Hide other views
        recyclerView.visibility = View.GONE
        shimmerContainer.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE

        // Detect error type and show appropriate message
        when {
            !isInternetAvailable() -> {
                tvErrorTitle.text = "No Internet Connection"
                tvErrorMessage.text = "Please check your connection and try again"
            }
            message.contains("Server Error") -> {
                tvErrorTitle.text = "Server Error"
                tvErrorMessage.text = "Our servers are having issues. Please try again later"
            }
            message.contains("Network Error") -> {
                tvErrorTitle.text = "Connection Failed"
                tvErrorMessage.text = "Unable to reach the server. Please check your internet"
            }
            else -> {
                tvErrorTitle.text = "Something Went Wrong"
                tvErrorMessage.text = message
            }
        }
    }
}