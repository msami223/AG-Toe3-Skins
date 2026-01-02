package com.devstormtech.toe3skins

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class TextEditorDialog(
    private val existingText: String? = null,
    private val existingSize: Float? = null,
    private val existingColor: Int? = null,
    private val existingFont: String? = null,
    private val onTextSaved: (String, Float, Int, String) -> Unit
) : DialogFragment() {

    private var textSize = existingSize ?: 48f
    private var textColor = existingColor ?: Color.WHITE
    private var fontFamily = existingFont ?: "sans-serif"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_DeviceDefault_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_text_editor_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etText: TextInputEditText = view.findViewById(R.id.etTextInput)
        val tvPreview: TextView = view.findViewById(R.id.tvTextPreview)
        val seekSize: SeekBar = view.findViewById(R.id.seekTextSize)
        val tvSizeValue: TextView = view.findViewById(R.id.tvSizeValue)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancelText)
        val btnSave: MaterialButton = view.findViewById(R.id.btnSaveText)
        val btnCustomColor: MaterialButton = view.findViewById(R.id.btnCustomColor)
        val chipGroupFonts: ChipGroup = view.findViewById(R.id.chipGroupFonts)

        // Font chips
        val chipSans: Chip = view.findViewById(R.id.chipSans)
        val chipSerif: Chip = view.findViewById(R.id.chipSerif)
        val chipMono: Chip = view.findViewById(R.id.chipMono)
        val chipCursive: Chip = view.findViewById(R.id.chipCursive)
        val chipBold: Chip = view.findViewById(R.id.chipBold)

        // Quick color views
        val colorWhite: View = view.findViewById(R.id.colorWhite)
        val colorRed: View = view.findViewById(R.id.colorRed)
        val colorYellow: View = view.findViewById(R.id.colorYellow)
        val colorBlue: View = view.findViewById(R.id.colorBlue)
        val colorBlack: View = view.findViewById(R.id.colorBlack)

        // Initialize with existing values if editing
        existingText?.let { etText.setText(it) }
        seekSize.progress = ((textSize - 20) / 1f).toInt().coerceIn(0, 100)
        tvSizeValue.text = "${textSize.toInt()}sp"
        
        // Set initial font chip
        when (fontFamily) {
            "sans-serif" -> chipSans.isChecked = true
            "serif" -> chipSerif.isChecked = true
            "monospace" -> chipMono.isChecked = true
            "cursive" -> chipCursive.isChecked = true
            "sans-serif-bold" -> chipBold.isChecked = true
        }
        
        updatePreview(tvPreview, etText.text?.toString() ?: "")

        // Text input listener
        etText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview(tvPreview, s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Font chip selection
        chipGroupFonts.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                fontFamily = when (checkedIds[0]) {
                    R.id.chipSans -> "sans-serif"
                    R.id.chipSerif -> "serif"
                    R.id.chipMono -> "monospace"
                    R.id.chipCursive -> "cursive"
                    R.id.chipBold -> "sans-serif-bold"
                    else -> "sans-serif"
                }
                updatePreview(tvPreview, etText.text?.toString() ?: "")
            }
        }

        // Size seekbar
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textSize = (progress + 20).toFloat() // Min 20, Max 120
                tvSizeValue.text = "${textSize.toInt()}sp"
                updatePreview(tvPreview, etText.text?.toString() ?: "")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Quick color buttons
        colorWhite.setOnClickListener { setTextColor(Color.WHITE, tvPreview) }
        colorRed.setOnClickListener { setTextColor(0xFFFF4444.toInt(), tvPreview) }
        colorYellow.setOnClickListener { setTextColor(0xFFFFDD00.toInt(), tvPreview) }
        colorBlue.setOnClickListener { setTextColor(0xFF4488FF.toInt(), tvPreview) }
        colorBlack.setOnClickListener { setTextColor(0xFF222222.toInt(), tvPreview) }

        // Custom color picker
        btnCustomColor.setOnClickListener {
            val colorPicker = ColorPickerDialog(textColor) { selectedColor ->
                setTextColor(selectedColor, tvPreview)
            }
            colorPicker.show(childFragmentManager, "ColorPicker")
        }

        // Action buttons
        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener {
            val text = etText.text?.toString() ?: ""
            if (text.isNotEmpty()) {
                onTextSaved(text, textSize, textColor, fontFamily)
                dismiss()
            }
        }
    }

    private fun updatePreview(textView: TextView, text: String) {
        textView.text = if (text.isEmpty()) "Your Text" else text
        textView.textSize = (textSize * 0.5f).coerceAtMost(36f) // Scale down for preview
        textView.setTextColor(textColor)
        
        // Apply font
        val typeface = when (fontFamily) {
            "sans-serif" -> Typeface.SANS_SERIF
            "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
            "sans-serif-bold" -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            else -> Typeface.SANS_SERIF
        }
        textView.typeface = typeface
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
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
