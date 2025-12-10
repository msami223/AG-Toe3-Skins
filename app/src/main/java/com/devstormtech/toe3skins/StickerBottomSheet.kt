package com.devstormtech.toe3skins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.adapter.StickerAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StickerBottomSheet(
    private val onStickerSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private val stickerList = listOf(
        R.drawable.sticker_angel_devil,
        R.drawable.sticker_dragon_tribal,
        R.drawable.sticker_dragon_blue,
        R.drawable.sticker_samurai_warrior,
        R.drawable.sticker_skull_snake,
        R.drawable.sticker_skull_dagger,
        R.drawable.sticker_skeleton,
        R.drawable.sticker_woman_tattoo,
        R.drawable.sticker_wolf,
        R.drawable.sticker_native_woman,
        R.drawable.sticker_samurai_katana
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_sticker_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.rvStickers)
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        val adapter = StickerAdapter(stickerList) { stickerRes ->
            onStickerSelected(stickerRes)
            dismiss()
        }
        recyclerView.adapter = adapter
    }
}
