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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return SkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
        val skin = skins[position]

        // Set Text Data
        holder.tvTruckModel.text = skin.acf.truckModel
        holder.tvTitle.text = skin.title.rendered
        holder.tvCreator.text = "By ${skin.acf.creatorName}"
        holder.tvDownloads.text = "⬇ ${skin.acf.downloadCount}"
        holder.tvViews.text = "👁 ${skin.acf.viewCount}"

        // Load Image using Glide
        // We check if previewImage1 is not empty, otherwise we don't load
        if (skin.acf.previewImage1.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(skin.acf.previewImage1)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background) // shows while loading
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