package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.simats.endecryption.databinding.ActivityTextSizeBinding

class TextSizeActivity : BaseActivity() {

    private lateinit var binding: ActivityTextSizeBinding
    private var currentSize: Float = 20f
    private var selectedSizeLabel: String = "Medium"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextSizeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentSettings()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.smallButton.setOnClickListener { selectSize(16f, "Small") }
        binding.mediumButton.setOnClickListener { selectSize(20f, "Medium") }
        binding.largeButton.setOnClickListener { selectSize(24f, "Large") }
        binding.extraLargeButton.setOnClickListener { selectSize(28f, "Extra Large") }

        binding.saveChangesButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadCurrentSettings() {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        currentSize = prefs.getFloat("TextSize", 20f)
        
        selectedSizeLabel = when (currentSize) {
            16f -> "Small"
            20f -> "Medium"
            24f -> "Large"
            28f -> "Extra Large"
            else -> "Medium"
        }
        
        updateUI()
    }

    private fun selectSize(size: Float, label: String) {
        currentSize = size
        selectedSizeLabel = label
        updateUI()
    }

    private fun updateUI() {
        // Update Preview Text Size
        binding.previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentSize)

        // Reset all buttons
        val buttons = listOf(binding.smallButton, binding.mediumButton, binding.largeButton, binding.extraLargeButton)
        for (btn in buttons) {
            btn.setBackgroundResource(R.drawable.text_size_option_background)
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            btn.icon = null
            btn.strokeWidth = 0
        }

        // Highlight selected button
        val selectedBtn = when (selectedSizeLabel) {
            "Small" -> binding.smallButton
            "Medium" -> binding.mediumButton
            "Large" -> binding.largeButton
            "Extra Large" -> binding.extraLargeButton
            else -> binding.mediumButton
        }

        selectedBtn.setBackgroundResource(R.drawable.bg_rounded_dark_green) // Using a green background for selection
        selectedBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        selectedBtn.icon = ContextCompat.getDrawable(this, R.drawable.ic_check_circle)
        selectedBtn.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        selectedBtn.iconTint = ContextCompat.getColorStateList(this, android.R.color.white)
    }

    private fun saveSettings() {
        // Save for BaseActivity font scaling
        val appPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        appPrefs.edit().putFloat("TextSize", currentSize).apply()

        // Save for AccessibilitySettings display label
        val accPrefs = getSharedPreferences("TextSizePrefs", Context.MODE_PRIVATE)
        accPrefs.edit().putString("selected_text_size", selectedSizeLabel).apply()

        Toast.makeText(this, "Text size updated to $selectedSizeLabel. Restarting app to apply changes...", Toast.LENGTH_SHORT).show()
        
        // Restart app to apply font scale change globally
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
