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

        RetrofitClient.instance.getSkins().enqueue(object : Callback<List<Skin>> {
            override fun onResponse(call: Call<List<Skin>>, response: Response<List<Skin>>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null) {
                    val skinsList = response.body() ?: return
                    if (skinsList.isNotEmpty()) {
                        allSkinsCache = skinsList
                        applyFilters()
                    } else {
                        // Empty list from server
                        _skins.value = emptyList()
                        _errorMessage.value = "We are working hard on adding new skins! Check back soon."
                    }
                } else {
                    _errorMessage.value = "Server Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<Skin>>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Network Error: ${t.message}"
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
