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

class RelatedSkinsAdapter(
    private var skins: List<Skin>,
    private val onSkinClick: (Skin) -> Unit
) : RecyclerView.Adapter<RelatedSkinsAdapter.RelatedSkinViewHolder>() {

    class RelatedSkinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivRelatedPreview)
        val tvModel: TextView = view.findViewById(R.id.tvRelatedModel)
        val tvTitle: TextView = view.findViewById(R.id.tvRelatedTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedSkinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_related_skin, parent, false)
        return RelatedSkinViewHolder(view)
    }

    override fun onBindViewHolder(holder: RelatedSkinViewHolder, position: Int) {
        val skin = skins[position]

        holder.tvModel.text = skin.acf.truckModel
        holder.tvTitle.text = skin.title.rendered

        // Load thumbnail
        if (skin.acf.previewImage1.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(skin.acf.previewImage1)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.ivPreview)
        }

        holder.itemView.setOnClickListener {
            onSkinClick(skin)
        }
    }

    override fun getItemCount() = skins.size

    fun updateSkins(newSkins: List<Skin>) {
        skins = newSkins
        notifyDataSetChanged()
    }
}
