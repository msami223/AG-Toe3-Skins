package com.devstormtech.toe3skins

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class TextEditorDialog(
    private val existingText: String? = null,
    private val existingSize: Float? = null,
    private val existingColor: Int? = null,
    private val onTextSaved: (String, Float, Int) -> Unit
) : DialogFragment() {

    private var textSize = existingSize ?: 48f
    private var textColor = existingColor ?: Color.BLACK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_text_editor_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etText: EditText = view.findViewById(R.id.etTextInput)
        val tvPreview: TextView = view.findViewById(R.id.tvTextPreview)
        val seekSize: SeekBar = view.findViewById(R.id.seekTextSize)
        val tvSizeValue: TextView = view.findViewById(R.id.tvSizeValue)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancelText)
        val btnSave: MaterialButton = view.findViewById(R.id.btnSaveText)

        // Color buttons
        val btnPickColor: MaterialButton = view.findViewById(R.id.btnPickColor)

        // Initialize with existing values if editing
        existingText?.let { etText.setText(it) }
        seekSize.progress = textSize.toInt()
        tvSizeValue.text = "${textSize.toInt()}sp"
        updatePreview(tvPreview, etText.text.toString())

        // Text input listener
        etText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(tvPreview, s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Size seekbar
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textSize = (progress + 20).toFloat() // Min 20, Max 120
                tvSizeValue.text = "${textSize.toInt()}sp"
                updatePreview(tvPreview, etText.text.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Color buttons
        btnPickColor.setOnClickListener {
            val colorPicker = ColorPickerDialog(textColor) { selectedColor ->
                setTextColor(selectedColor, tvPreview)
            }
            colorPicker.show(childFragmentManager, "ColorPicker")
        }

        // Action buttons
        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener {
            val text = etText.text.toString()
            if (text.isNotEmpty()) {
                onTextSaved(text, textSize, textColor)
                dismiss()
            }
        }
    }

    private fun updatePreview(textView: TextView, text: String) {
        textView.text = if (text.isEmpty()) "Preview" else text
        textView.textSize = textSize
        textView.setTextColor(textColor)
    }

    private fun setTextColor(color: Int, preview: TextView) {
        textColor = color
        preview.setTextColor(color)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
