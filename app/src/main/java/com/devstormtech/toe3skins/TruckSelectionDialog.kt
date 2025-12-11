package com.devstormtech.toe3skins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TruckSelectionDialog(
    private val onTruckSelected: (TruckModel) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_truck_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTrucks)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.setItemViewCacheSize(14) // Cache all truck items
        recyclerView.adapter = TruckAdapter(TruckModel.getAllTrucks()) { truck ->
            onTruckSelected(truck)
            dismiss()
        }
    }

    private inner class TruckAdapter(
        private val trucks: List<TruckModel>,
        private val onClick: (TruckModel) -> Unit
    ) : RecyclerView.Adapter<TruckAdapter.TruckViewHolder>() {

        inner class TruckViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgPreview: ImageView = view.findViewById(R.id.imgTruckPreview)
            val txtName: TextView = view.findViewById(R.id.txtTruckName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TruckViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_truck_selection, parent, false)
            return TruckViewHolder(view)
        }

        override fun onBindViewHolder(holder: TruckViewHolder, position: Int) {
            val truck = trucks[position]
            
            // Use Glide for efficient async image loading with small thumbnail
            Glide.with(holder.itemView.context)
                .load(truck.templateResource)
                .override(200, 200) // Load as 200x200 thumbnail
                .centerInside()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(holder.imgPreview)
            
            holder.txtName.text = truck.displayName
            holder.itemView.setOnClickListener { onClick(truck) }
        }

        override fun getItemCount() = trucks.size
    }
}
