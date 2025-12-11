package com.devstormtech.toe3skins.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devstormtech.toe3skins.R

// Sealed class to represent either a local drawable or a remote URL sticker
sealed class StickerItem {
    data class Local(val resourceId: Int) : StickerItem()
    data class Remote(val url: String, val title: String) : StickerItem()
}

class StickerAdapter(
    private val stickers: List<StickerItem>,
    private val onStickerClick: (StickerItem) -> Unit
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
        val sticker = stickers[position]
        
        when (sticker) {
            is StickerItem.Local -> {
                holder.stickerImage.setImageResource(sticker.resourceId)
            }
            is StickerItem.Remote -> {
                Glide.with(holder.itemView.context)
                    .load(sticker.url)
                    .placeholder(R.drawable.ic_upload)
                    .into(holder.stickerImage)
            }
        }
        
        holder.itemView.setOnClickListener {
            onStickerClick(sticker)
        }
    }

    override fun getItemCount() = stickers.size
}
