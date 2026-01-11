package com.devstormtech.toe3skins

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devstormtech.toe3skins.adapter.LayerAdapter
import com.google.android.material.button.MaterialButton

class LayersDialog : DialogFragment() {

    // Store data as instance variables
    private var layers: List<CanvasElement>? = null
    private var onLayerSelected: ((CanvasElement) -> Unit)? = null
    private var onLayerDeleted: ((CanvasElement) -> Unit)? = null

    companion object {
        fun newInstance(
            layers: List<CanvasElement>,
            onLayerSelected: (CanvasElement) -> Unit,
            onLayerDeleted: (CanvasElement) -> Unit
        ): LayersDialog {
            return LayersDialog().apply {
                this.layers = layers
                this.onLayerSelected = onLayerSelected
                this.onLayerDeleted = onLayerDeleted
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_layers_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.rvLayers)
        val btnClose: MaterialButton = view.findViewById(R.id.btnCloseLayers)
        val tvEmpty: TextView = view.findViewById(R.id.tvEmptyLayers)

        val layersList = layers ?: emptyList()

        if (layersList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            recyclerView.layoutManager = LinearLayoutManager(context)
            val adapter = LayerAdapter(
                layers = layersList,
                onLayerClick = { layer ->
                    onLayerSelected?.invoke(layer)
                    dismiss()
                },
                onDeleteClick = { layer ->
                    onLayerDeleted?.invoke(layer)
                    dismiss()
                }
            )
            recyclerView.adapter = adapter
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
