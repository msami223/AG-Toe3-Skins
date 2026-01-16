package com.devstormtech.toe3skins

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devstormtech.toe3skins.api.RetrofitClient
import com.devstormtech.toe3skins.model.Skin
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel : ViewModel() {

    // Internal mutable state
    private val _skins = MutableLiveData<List<Skin>>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _errorMessage = MutableLiveData<String?>()

    // Public immutable state
    val skins: LiveData<List<Skin>> = _skins
    val isLoading: LiveData<Boolean> = _isLoading
    val errorMessage: LiveData<String?> = _errorMessage

    private var allSkinsCache: List<Skin> = emptyList()
    private var currentFilterTruck = "All"
    private var currentSearchQuery = ""

    init {
        // Fetch skins on init
        fetchSkins()
    }

    fun fetchSkins(refresh: Boolean = false) {
        _isLoading.value = !refresh // Only show full loading if not pull-to-refresh
        _errorMessage.value = null
        
        // Fetch all pages recursively
        fetchAllSkinsPages(page = 1, accumulated = mutableListOf())
    }
    
    private fun fetchAllSkinsPages(page: Int, accumulated: MutableList<Skin>) {
        RetrofitClient.instance.getSkins(page).enqueue(object : Callback<List<Skin>> {
            override fun onResponse(call: Call<List<Skin>>, response: Response<List<Skin>>) {
                if (response.isSuccessful && response.body() != null) {
                    val skinsList = response.body() ?: emptyList()
                    accumulated.addAll(skinsList)
                    
                    // If we got 100 skins, there might be more pages
                    if (skinsList.size >= 100) {
                        // Fetch next page
                        fetchAllSkinsPages(page + 1, accumulated)
                    } else {
                        // Done fetching all pages
                        _isLoading.value = false
                        if (accumulated.isNotEmpty()) {
                            allSkinsCache = accumulated
                            applyFilters()
                        } else {
                            _skins.value = emptyList()
                            _errorMessage.value = "We are working hard on adding new skins! Check back soon."
                        }
                    }
                } else {
                    _isLoading.value = false
                    // If this is not the first page and we got an error, use what we have
                    if (page > 1 && accumulated.isNotEmpty()) {
                        allSkinsCache = accumulated
                        applyFilters()
                    } else {
                        _errorMessage.value = "Server Error: ${response.code()}"
                    }
                }
            }

            override fun onFailure(call: Call<List<Skin>>, t: Throwable) {
                _isLoading.value = false
                // If this is not the first page and we have some data, use it
                if (page > 1 && accumulated.isNotEmpty()) {
                    allSkinsCache = accumulated
                    applyFilters()
                } else {
                    _errorMessage.value = "Network Error: ${t.message}"
                }
            }
        })
    }

    fun setFilter(truck: String) {
        currentFilterTruck = truck
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val query = currentSearchQuery.lowercase()
        val filteredList = allSkinsCache.filter { skin ->
            val matchesTruck = if (currentFilterTruck == "All") {
                true
            } else {
                skin.acf.truckModel?.lowercase()?.contains(currentFilterTruck.lowercase()) == true
            }
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                skin.title.rendered?.lowercase()?.contains(query) == true ||
                        skin.acf.truckModel?.lowercase()?.contains(query) == true ||
                        skin.acf.creatorName?.lowercase()?.contains(query) == true ||
                        (skin.acf.tags?.any { it?.lowercase()?.contains(query) == true } == true)
            }
            matchesTruck && matchesSearch
        }
        _skins.value = filteredList
    }

    fun sortSkins(selector: (Skin) -> Int) {
        allSkinsCache = allSkinsCache.sortedByDescending(selector)
        applyFilters()
    }
}
