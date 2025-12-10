package com.devstormtech.toe3skins.adapter

import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.CanvasElement
import com.devstormtech.toe3skins.R

class LayerAdapter(
    private val layers: List<CanvasElement>,
    private val onLayerClick: (CanvasElement) -> Unit,
    private val onDeleteClick: (CanvasElement) -> Unit
) : RecyclerView.Adapter<LayerAdapter.LayerViewHolder>() {

    inner class LayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.ivLayerThumbnail)
        val layerName: TextView = view.findViewById(R.id.tvLayerName)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteLayer)
        val selectedIndicator: View = view.findViewById(R.id.viewSelectedIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layer, parent, false)
        return LayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        val layer = layers[position]

        // Set layer name and thumbnail
        when (layer) {
            is CanvasElement.StickerElement -> {
                holder.layerName.text = "Sticker ${position + 1}"
                holder.thumbnail.setImageBitmap(layer.bitmap)
            }
            is CanvasElement.TextElement -> {
                holder.layerName.text = "Text: ${layer.text.take(15)}${if(layer.text.length > 15) "..." else ""}"
                // Create text thumbnail
                val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.parseColor("#2C2C2C"))
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.textColor
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    if (layer.text.length > 10) layer.text.take(10) + "..." else layer.text,
                    50f,
                    60f,
                    paint
                )
                holder.thumbnail.setImageBitmap(bitmap)
            }
        }

        // Show selection indicator
        holder.selectedIndicator.visibility = if (layer.isSelected) View.VISIBLE else View.GONE

        // Click listeners
        holder.itemView.setOnClickListener {
            onLayerClick(layer)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(layer)
        }
    }

    override fun getItemCount() = layers.size
}
