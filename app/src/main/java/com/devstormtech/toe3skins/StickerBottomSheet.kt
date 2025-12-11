package com.devstormtech.toe3skins

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.adapter.StickerAdapter
import com.devstormtech.toe3skins.adapter.StickerItem
import com.devstormtech.toe3skins.api.RetrofitClient
import com.devstormtech.toe3skins.model.Sticker
import com.devstormtech.toe3skins.model.getImageUrl
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StickerBottomSheet(
    private val onLocalStickerSelected: (Int) -> Unit,
    private val onRemoteStickerSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    // Local drawable stickers with names for search
    private val localStickersWithNames = listOf(
        Pair(StickerItem.Local(R.drawable.ic_upload), "upload custom"),
        Pair(StickerItem.Local(R.drawable.sticker_angel_devil), "angel devil"),
        Pair(StickerItem.Local(R.drawable.sticker_dragon_tribal), "dragon tribal"),
        Pair(StickerItem.Local(R.drawable.sticker_dragon_blue), "dragon blue"),
        Pair(StickerItem.Local(R.drawable.sticker_samurai_warrior), "samurai warrior"),
        Pair(StickerItem.Local(R.drawable.sticker_skull_snake), "skull snake"),
        Pair(StickerItem.Local(R.drawable.sticker_skull_dagger), "skull dagger"),
        Pair(StickerItem.Local(R.drawable.sticker_skeleton), "skeleton"),
        Pair(StickerItem.Local(R.drawable.sticker_woman_tattoo), "woman tattoo"),
        Pair(StickerItem.Local(R.drawable.sticker_wolf), "wolf"),
        Pair(StickerItem.Local(R.drawable.sticker_native_woman), "native woman"),
        Pair(StickerItem.Local(R.drawable.sticker_samurai_katana), "samurai katana")
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private var allStickers = mutableListOf<Pair<StickerItem, String>>()
    private var filteredStickers = mutableListOf<StickerItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_sticker_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvStickers)
        searchEditText = view.findViewById(R.id.etStickerSearch)
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        // Start with local stickers
        allStickers.addAll(localStickersWithNames)
        applyFilter("")

        // Setup search listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Fetch WordPress stickers
        fetchWordPressStickers()
    }

    private fun applyFilter(query: String) {
        filteredStickers.clear()
        
        if (query.isEmpty()) {
            filteredStickers.addAll(allStickers.map { it.first })
        } else {
            val lowerQuery = query.lowercase()
            allStickers.forEach { (sticker, name) ->
                if (name.lowercase().contains(lowerQuery)) {
                    filteredStickers.add(sticker)
                }
            }
        }
        
        setupAdapter()
    }

    private fun setupAdapter() {
        val adapter = StickerAdapter(filteredStickers) { stickerItem ->
            when (stickerItem) {
                is StickerItem.Local -> {
                    onLocalStickerSelected(stickerItem.resourceId)
                }
                is StickerItem.Remote -> {
                    onRemoteStickerSelected(stickerItem.url)
                }
            }
            dismiss()
        }
        recyclerView.adapter = adapter
    }

    private fun fetchWordPressStickers() {
        RetrofitClient.instance.getStickers().enqueue(object : Callback<List<Sticker>> {
            override fun onResponse(call: Call<List<Sticker>>, response: Response<List<Sticker>>) {
                if (response.isSuccessful && response.body() != null) {
                    val wpStickers = response.body()!!
                    
                    // Convert WordPress stickers to StickerItem.Remote with names
                    val remoteStickers = wpStickers.mapNotNull { sticker ->
                        val imageUrl = sticker.getImageUrl()
                        if (imageUrl != null) {
                            Pair(
                                StickerItem.Remote(imageUrl, sticker.title.rendered),
                                sticker.title.rendered
                            )
                        } else {
                            null
                        }
                    }
                    
                    // Add to the list after local stickers
                    if (remoteStickers.isNotEmpty()) {
                        allStickers.addAll(remoteStickers)
                        applyFilter(searchEditText.text?.toString() ?: "")
                    }
                }
            }

            override fun onFailure(call: Call<List<Sticker>>, t: Throwable) {
                // Silently fail - local stickers will still work
                android.util.Log.e("StickerBottomSheet", "Failed to fetch WP stickers: ${t.message}")
            }
        })
    }
}
