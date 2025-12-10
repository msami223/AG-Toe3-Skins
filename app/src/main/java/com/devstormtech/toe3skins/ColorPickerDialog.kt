package com.devstormtech.toe3skins

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class ColorPickerDialog(
    private val currentColor: Int,
    private val onColorSelected: (Int) -> Unit
) : DialogFragment() {

    private var red = Color.red(currentColor)
    private var green = Color.green(currentColor)
    private var blue = Color.blue(currentColor)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_color_picker_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val colorPreview: View = view.findViewById(R.id.colorPreview)
        val seekRed: SeekBar = view.findViewById(R.id.seekRed)
        val seekGreen: SeekBar = view.findViewById(R.id.seekGreen)
        val seekBlue: SeekBar = view.findViewById(R.id.seekBlue)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancelColor)
        val btnApply: MaterialButton = view.findViewById(R.id.btnApplyColor)

        // Preset color buttons
        val btnWhite: MaterialButton = view.findViewById(R.id.btnColorWhite)
        val btnRed: MaterialButton = view.findViewById(R.id.btnColorRed)
        val btnBlue: MaterialButton = view.findViewById(R.id.btnColorBlue)
        val btnGreen: MaterialButton = view.findViewById(R.id.btnColorGreen)
        val btnYellow: MaterialButton = view.findViewById(R.id.btnColorYellow)
        val btnBlack: MaterialButton = view.findViewById(R.id.btnColorBlack)

        // Initialize seekbars
        seekRed.progress = red
        seekGreen.progress = green
        seekBlue.progress = blue
        updateColorPreview(colorPreview)

        // Seekbar listeners
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                when (seekBar?.id) {
                    R.id.seekRed -> red = progress
                    R.id.seekGreen -> green = progress
                    R.id.seekBlue -> blue = progress
                }
                updateColorPreview(colorPreview)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekRed.setOnSeekBarChangeListener(seekBarListener)
        seekGreen.setOnSeekBarChangeListener(seekBarListener)
        seekBlue.setOnSeekBarChangeListener(seekBarListener)

        // Preset color clicks
        btnWhite.setOnClickListener { setColor(255, 255, 255, seekRed, seekGreen, seekBlue, colorPreview) }
        btnRed.setOnClickListener { setColor(255, 0, 0, seekRed, seekGreen, seekBlue, colorPreview) }
        btnBlue.setOnClickListener { setColor(0, 0, 255, seekRed, seekGreen, seekBlue, colorPreview) }
        btnGreen.setOnClickListener { setColor(0, 255, 0, seekRed, seekGreen, seekBlue, colorPreview) }
        btnYellow.setOnClickListener { setColor(255, 255, 0, seekRed, seekGreen, seekBlue, colorPreview) }
        btnBlack.setOnClickListener { setColor(0, 0, 0, seekRed, seekGreen, seekBlue, colorPreview) }

        // Action buttons
        btnCancel.setOnClickListener { dismiss() }
        btnApply.setOnClickListener {
            onColorSelected(Color.rgb(red, green, blue))
            dismiss()
        }
    }

    private fun updateColorPreview(view: View) {
        view.setBackgroundColor(Color.rgb(red, green, blue))
    }

    private fun setColor(r: Int, g: Int, b: Int, seekRed: SeekBar, seekGreen: SeekBar, seekBlue: SeekBar, preview: View) {
        red = r
        green = g
        blue = b
        seekRed.progress = r
        seekGreen.progress = g
        seekBlue.progress = b
        updateColorPreview(preview)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
