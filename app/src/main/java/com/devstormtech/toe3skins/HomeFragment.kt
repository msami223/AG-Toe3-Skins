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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.devstormtech.toe3skins.adapter.SkinsAdapter
import com.devstormtech.toe3skins.adapter.TruckFilterAdapter
import com.devstormtech.toe3skins.api.RetrofitClient
import com.devstormtech.toe3skins.model.Skin
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Response
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer

class HomeFragment : Fragment() {

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

    // ViewModel
    private val viewModel: HomeViewModel by viewModels()

    // Notification Toggle
    private lateinit var notificationBanner: CardView
    private lateinit var notificationSwitch: SwitchCompat

    // FAB
    private lateinit var fabCreateSkin: com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

    private val NOTIFICATION_PERMISSION_CODE = 101
    private val PREFS_NAME = "TOE3SkinsPrefs"
    private val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize Views
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        recyclerView = view.findViewById(R.id.rvSkins)
        rvTruckFilters = view.findViewById(R.id.rvTruckFilters)
        errorLayout = view.findViewById(R.id.errorLayout)
        tvErrorTitle = errorLayout.findViewById(R.id.tvErrorTitle)
        tvErrorMessage = errorLayout.findViewById(R.id.tvErrorMessage)
        btnRetry = errorLayout.findViewById(R.id.btnRetry)

        // Setup retry button
        btnRetry.setOnClickListener {
            viewModel.fetchSkins(false)
        }

        etSearch = view.findViewById(R.id.etHeaderSearch)
        btnSort = view.findViewById(R.id.btnHeaderSort)
        btnBack = view.findViewById(R.id.btnHeaderBack)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
        notificationBanner = view.findViewById(R.id.cardNotificationPermission)
        notificationSwitch = view.findViewById(R.id.switchNotifications)
        fabCreateSkin = view.findViewById(R.id.fabCreateSkin)

        // FAB Click Listener - Redirect to SkinMakerFragment via MainActivity (or just hide it)
        // Since we have a bottom nav now, the FAB might be redundant or could switch tabs.
        // For now, let's make it switch to the Skin Editor tab.
        fabCreateSkin.setOnClickListener {
           (activity as? MainActivity)?.switchToSkinEditor()
        }

        btnBack.visibility = View.GONE

        // 2. Setup Notification Toggle
        setupNotificationToggle()

        // 3. Notification Click
        // In Fragment, we check arguments or activity intent
        // 3. Notification Click
        // In Fragment, we check arguments or activity intent
        val notificationTarget = activity?.intent?.getStringExtra("TARGET_TAB")
        if (notificationTarget != null) {
            viewModel.setFilter(notificationTarget)
            Toast.makeText(requireContext(), "Viewing $notificationTarget Skins", Toast.LENGTH_LONG).show()
        }

        // 4. Truck Filters
        setupTruckFilters("All") // Default to "All" initially


        // 5. Skins Grid
        val spanCount = resources.getInteger(R.integer.grid_columns)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        adapter = SkinsAdapter(emptyList()) { skin ->
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("SKIN_DATA", skin)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // 6. Pull to Refresh
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchSkins(true)
        }

        // 7. Tag Search
        val tagQuery = activity?.intent?.getStringExtra("SEARCH_QUERY")
        if (tagQuery != null) {
            etSearch.setText(tagQuery)
        }

        // 8. Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 9. Sort
        btnSort.setOnClickListener { v -> showSortMenu(v) }

        // 10. Fetch Data
        // 10. Observe ViewModel
        observeViewModel()
        
        // Initial Fetch is done in ViewModel init, so we don't need to call it here unless we want to force something.
        if (tagQuery != null) {
             viewModel.setSearchQuery(tagQuery)
        }
    }
    
    private fun observeViewModel() {
        viewModel.skins.observe(viewLifecycleOwner, Observer { skins ->
            adapter.updateSkins(skins)
            if (skins.isEmpty() && viewModel.errorMessage.value == null) {
                 // It's empty but no error yet (loading or actually empty)
            } else if (skins.isNotEmpty()) {
                errorLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                if (!swipeRefreshLayout.isRefreshing) {
                    shimmerContainer.startShimmer()
                    shimmerContainer.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    errorLayout.visibility = View.GONE
                }
            } else {
                swipeRefreshLayout.isRefreshing = false
                shimmerContainer.stopShimmer()
                shimmerContainer.visibility = View.GONE
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                showError(error)
            }
        })
    }

    private fun setupNotificationToggle() {
        updateNotificationToggle()
    }

    private fun updateNotificationToggle() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userWantsNotifications = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isPermissionGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val shouldBeChecked = userWantsNotifications && isPermissionGranted

            notificationSwitch.setOnCheckedChangeListener(null)
            notificationSwitch.isChecked = shouldBeChecked

            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, true).apply()
                    if (!isPermissionGranted) {
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_CODE
                        )
                    } else {
                        subscribeToNotifications()
                    }
                } else {
                    prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply()
                    unsubscribeFromNotifications()
                }
            }
        } else {
            notificationSwitch.setOnCheckedChangeListener(null)
            notificationSwitch.isChecked = userWantsNotifications

            notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
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
                Toast.makeText(requireContext(), "✓ Notifications Enabled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to enable notifications", Toast.LENGTH_SHORT).show()
                updateNotificationToggle()
            }
    }

    private fun unsubscribeFromNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✗ Notifications Disabled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to disable notifications", Toast.LENGTH_SHORT).show()
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
                subscribeToNotifications()
                updateNotificationToggle()
            } else {
                val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, false).apply()
                Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show()
                updateNotificationToggle()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationToggle()
    }

    // fetchSkins removed - logic moved to ViewModel

    private fun setupTruckFilters(initialSelection: String) {
        val trucks = listOf("All", "Stream", "Merieles", "Moon", "Volcano", "Dawn", "Fiora", "Renovate")
        rvTruckFilters.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val initialIndex = trucks.indexOfFirst { it.equals(initialSelection, ignoreCase = true) }
        val startPos = if (initialIndex >= 0) initialIndex else 0
        val filterAdapter = TruckFilterAdapter(trucks) { selectedTruck ->
            viewModel.setFilter(selectedTruck)
        }
        filterAdapter.setSelected(startPos)
        rvTruckFilters.adapter = filterAdapter
        rvTruckFilters.scrollToPosition(startPos)
    }

    // applyFilters removed - logic moved to ViewModel

    private fun showSortMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, "Most Popular (Downloads)")
        popup.menu.add(0, 2, 1, "Most Viewed")
        popup.menu.add(0, 3, 2, "Newest First")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> viewModel.sortSkins { it.acf.downloadCount }
                2 -> viewModel.sortSkins { it.acf.viewCount }
                3 -> viewModel.sortSkins { it.id }
            }
            true
        }
        popup.show()
    }

    // sortSkinsBy removed - logic moved to ViewModel

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        recyclerView.visibility = View.GONE
        shimmerContainer.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE

        val iconView = errorLayout.findViewById<TextView>(R.id.tvErrorIcon)

        when {
            !isInternetAvailable() -> {
                iconView.text = "🔌"
                tvErrorTitle.text = "Whoops! No Internet"
                tvErrorMessage.text = "The internet gremlins are at it again! 👾\nCheck your connection."
            }
            message.contains("Server Error") -> {
                iconView.text = "😵"
                tvErrorTitle.text = "Server Hiccup"
                tvErrorMessage.text = "Our server had too much soda. Back soon! 🥤"
            }
            message.contains("Network Error") -> {
                iconView.text = "🙈"
                tvErrorTitle.text = "Connection Failed"
                tvErrorMessage.text = "We couldn't reach the mothership. 🛸\nPlease try again."
            }
            else -> {
                iconView.text = "😴"
                tvErrorTitle.text = "It's Quiet..."
                tvErrorMessage.text = "Our artists are sleeping! No skins found. 💤"
            }
        }
    }
    
    /**
     * Called by MainActivity when app is opened via notification click
     * to filter by a specific truck type
     */
    fun setInitialFilter(truckFilter: String) {
        viewModel.setFilter(truckFilter)
        
        // Only update UI if the view is already created
        if (view != null && ::rvTruckFilters.isInitialized) {
            // Update the filter adapter to show the selected truck
            val trucks = listOf("All", "Stream", "Merieles", "Moon", "Volcano", "Dawn", "Fiora", "Renovate")
            val index = trucks.indexOfFirst { it.equals(truckFilter, ignoreCase = true) }
            if (index >= 0) {
                (rvTruckFilters.adapter as? TruckFilterAdapter)?.setSelected(index)
                rvTruckFilters.scrollToPosition(index)
            }
            
            // Reapply filters with the new truck selection
            viewModel.setFilter(truckFilter)
            
            Toast.makeText(requireContext(), "Viewing $truckFilter Skins", Toast.LENGTH_SHORT).show()
        }
    }
}
