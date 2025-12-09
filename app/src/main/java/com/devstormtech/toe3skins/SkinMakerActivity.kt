package com.devstormtech.toe3skins

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SkinMakerActivity : AppCompatActivity() {

    private lateinit var canvasImage: ImageView
    private lateinit var btnColorPicker: MaterialButton
    private lateinit var btnStickers: MaterialButton
    private lateinit var btnText: MaterialButton
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnRedo: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: ImageView

    private var currentColor = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_maker)

        initViews()
        loadTruckTemplate()
        setupListeners()
    }

    private fun initViews() {
        canvasImage = findViewById(R.id.canvasImage)
        btnColorPicker = findViewById(R.id.btnColorPicker)
        btnStickers = findViewById(R.id.btnStickers)
        btnText = findViewById(R.id.btnText)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBackSkinMaker)
    }

    private fun loadTruckTemplate() {
        // Load the Stream truck template
        canvasImage.setImageResource(R.drawable.stream_template)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnColorPicker.setOnClickListener {
            showColorPicker()
        }

        btnStickers.setOnClickListener {
            showStickerPicker()
        }

        btnText.setOnClickListener {
            showTextEditor()
        }

        btnUndo.setOnClickListener {
            Toast.makeText(this, "Undo - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnRedo.setOnClickListener {
            Toast.makeText(this, "Redo - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            saveSkin()
        }
    }

    private fun showColorPicker() {
        Toast.makeText(this, "Color Picker - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showStickerPicker() {
        Toast.makeText(this, "Sticker Picker - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showTextEditor() {
        Toast.makeText(this, "Text Editor - Coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun saveSkin() {
        Toast.makeText(this, "Save Skin - Coming soon!", Toast.LENGTH_SHORT).show()
    }
}