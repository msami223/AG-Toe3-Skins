package com.devstormtech.toe3skins.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devstormtech.toe3skins.R
import com.devstormtech.toe3skins.model.Skin

class SkinsAdapter(
    private var skins: List<Skin>,
    private val onSkinClick: (Skin) -> Unit
) : RecyclerView.Adapter<SkinsAdapter.SkinViewHolder>() {

    class SkinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivSkinPreview)
        val tvTruckModel: TextView = view.findViewById(R.id.tvTruckModel)
        val tvTitle: TextView = view.findViewById(R.id.tvSkinTitle)
        val tvCreator: TextView = view.findViewById(R.id.tvCreator)
        val tvDownloads: TextView = view.findViewById(R.id.tvDownloads)
        val tvViews: TextView = view.findViewById(R.id.tvViews)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        val skin = skins[position]

        // Set Text Data with null safety
        holder.tvTruckModel.text = skin.acf.truckModel ?: "Unknown"
        holder.tvTitle.text = skin.title.rendered ?: "Untitled"
        holder.tvCreator.text = "By ${skin.acf.creatorName ?: "Unknown"}"
        holder.tvDownloads.text = "⬇ ${skin.acf.downloadCount}"
        holder.tvViews.text = "👁 ${skin.acf.viewCount}"
        holder.tvLikes.text = "❤ ${skin.acf.likeCount}"

        // Load Image using Glide with null safety
        val imageUrl = skin.acf.previewImage1
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .into(holder.ivPreview)
        }

        // Handle Click
        holder.itemView.setOnClickListener {
            onSkinClick(skin)
        }
    }

    override fun getItemCount() = skins.size

    // Helper to update list efficiently
    fun updateSkins(newSkins: List<Skin>) {
        skins = newSkins
        notifyDataSetChanged()
    }
}