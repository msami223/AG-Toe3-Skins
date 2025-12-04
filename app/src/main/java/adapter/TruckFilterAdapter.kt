package com.devstormtech.toe3skins.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.R

class TruckFilterAdapter(
    private val trucks: List<String>,
    private val onTruckSelected: (String) -> Unit
) : RecyclerView.Adapter<TruckFilterAdapter.TruckViewHolder>() {

    private var selectedPosition = 0 // "All" is selected by default

    // --- NEW FUNCTION: Allows MainActivity to change the selection ---
    fun setSelected(position: Int) {
        selectedPosition = position
        notifyDataSetChanged() // Refreshes the list to show the new color
    }

    class TruckViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTruck: TextView = view.findViewById(R.id.tvTruckFilter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TruckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_truck_filter, parent, false)
        return TruckViewHolder(view)
    }

    override fun onBindViewHolder(holder: TruckViewHolder, position: Int) {
        val truck = trucks[position]
        holder.tvTruck.text = truck

        // Change Color based on selection
        if (selectedPosition == position) {
            // Selected: Purple Background, Black Text
            holder.tvTruck.setBackgroundResource(R.drawable.bg_filter_chip_selected)
            holder.tvTruck.setTextColor(Color.BLACK)
        } else {
            // Unselected: Dark Gray Background, White Text
            holder.tvTruck.setBackgroundResource(R.drawable.bg_filter_chip_unselected)
            holder.tvTruck.setTextColor(Color.WHITE)
        }

        // Handle Click
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Refresh only the two items that changed (more efficient)
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            // Send selection back to Main Activity
            onTruckSelected(truck)
        }
    }

    override fun getItemCount() = trucks.size
}