package com.devstormtech.toe3skins

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class ColorPickerDialog : DialogFragment() {

    private var selectedColor = Color.WHITE
    private var onColorSelected: ((Int) -> Unit)? = null

    companion object {
        private const val ARG_CURRENT_COLOR = "current_color"

        fun newInstance(
            currentColor: Int,
            onColorSelected: (Int) -> Unit
        ): ColorPickerDialog {
            return ColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CURRENT_COLOR, currentColor)
                }
                this.onColorSelected = onColorSelected
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_DeviceDefault_Dialog)
        
        selectedColor = arguments?.getInt(ARG_CURRENT_COLOR, Color.WHITE) ?: Color.WHITE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_color_picker_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val colorWheel: ColorWheelView = view.findViewById(R.id.colorWheel)
        val colorPreview: View = view.findViewById(R.id.colorPreview)
        val tvHexCode: TextView = view.findViewById(R.id.tvHexCode)
        val seekBrightness: SeekBar = view.findViewById(R.id.seekBrightness)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancelColor)
        val btnApply: MaterialButton = view.findViewById(R.id.btnApplyColor)

        // Quick color views
        val quickWhite: View = view.findViewById(R.id.quickWhite)
        val quickRed: View = view.findViewById(R.id.quickRed)
        val quickOrange: View = view.findViewById(R.id.quickOrange)
        val quickYellow: View = view.findViewById(R.id.quickYellow)
        val quickGreen: View = view.findViewById(R.id.quickGreen)
        val quickBlue: View = view.findViewById(R.id.quickBlue)
        val quickPurple: View = view.findViewById(R.id.quickPurple)
        val quickBlack: View = view.findViewById(R.id.quickBlack)

        // Initialize with current color
        colorWheel.setColor(selectedColor)
        seekBrightness.progress = (colorWheel.getBrightness() * 100).toInt()
        updatePreview(colorPreview, tvHexCode, selectedColor)

        // Color wheel callback
        colorWheel.onColorChanged = { color ->
            selectedColor = color
            updatePreview(colorPreview, tvHexCode, color)
        }

        // Brightness slider
        seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                colorWheel.setBrightness(progress / 100f)
                selectedColor = colorWheel.getCurrentColor()
                updatePreview(colorPreview, tvHexCode, selectedColor)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Quick color clicks
        quickWhite.setOnClickListener { setQuickColor(Color.WHITE, colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickRed.setOnClickListener { setQuickColor(0xFFFF4444.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickOrange.setOnClickListener { setQuickColor(0xFFFF8800.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickYellow.setOnClickListener { setQuickColor(0xFFFFDD00.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickGreen.setOnClickListener { setQuickColor(0xFF44DD44.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickBlue.setOnClickListener { setQuickColor(0xFF4488FF.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickPurple.setOnClickListener { setQuickColor(0xFFAA44FF.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }
        quickBlack.setOnClickListener { setQuickColor(0xFF222222.toInt(), colorWheel, seekBrightness, colorPreview, tvHexCode) }

        // Action buttons
        btnCancel.setOnClickListener { dismiss() }
        btnApply.setOnClickListener {
            onColorSelected?.invoke(selectedColor)
            dismiss()
        }
    }

    private fun updatePreview(preview: View, hexText: TextView, color: Int) {
        val drawable = preview.background as? GradientDrawable
        drawable?.setColor(color)
        
        val hexCode = String.format("#%06X", 0xFFFFFF and color)
        hexText.text = hexCode
    }

    private fun setQuickColor(color: Int, wheel: ColorWheelView, seekBri: SeekBar, preview: View, hexText: TextView) {
        wheel.setColor(color)
        seekBri.progress = (wheel.getBrightness() * 100).toInt()
        selectedColor = color
        updatePreview(preview, hexText, color)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
