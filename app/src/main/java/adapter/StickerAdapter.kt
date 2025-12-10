package com.devstormtech.toe3skins.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.R

class StickerAdapter(
    private val stickers: List<Int>,
    private val onStickerClick: (Int) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    inner class StickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stickerImage: ImageView = view.findViewById(R.id.ivSticker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker, parent, false)
        return StickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val stickerRes = stickers[position]
        holder.stickerImage.setImageResource(stickerRes)
        holder.itemView.setOnClickListener {
            onStickerClick(stickerRes)
        }
    }

    override fun getItemCount() = stickers.size
}
