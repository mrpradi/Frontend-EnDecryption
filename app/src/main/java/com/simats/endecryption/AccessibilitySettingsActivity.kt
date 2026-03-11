package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityAccessibilitySettingsBinding

class AccessibilitySettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityAccessibilitySettingsBinding
    private val PREFS_NAME = "AccessibilitySettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessibilitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.textSizeButton.setOnClickListener {
            startActivity(Intent(this, TextSizeActivity::class.java))
        }

        binding.switchContrast.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("high_contrast", isChecked)
            binding.statusContrast.text = if (isChecked) "Enabled" else "Disabled"
            showFeedback("High Contrast Mode ${if (isChecked) "enabled" else "disabled"}")
            // Note: In a real app, you would trigger a theme refresh here
        }

        binding.switchMotion.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("reduce_motion", isChecked)
            binding.statusMotion.text = if (isChecked) "Enabled" else "Disabled"
            showFeedback("Reduce Motion ${if (isChecked) "enabled" else "disabled"}")
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load High Contrast
        val isContrastEnabled = prefs.getBoolean("high_contrast", false)
        binding.switchContrast.isChecked = isContrastEnabled
        binding.statusContrast.text = if (isContrastEnabled) "Enabled" else "Disabled"

        // Load Reduce Motion
        val isMotionReduced = prefs.getBoolean("reduce_motion", false)
        binding.switchMotion.isChecked = isMotionReduced
        binding.statusMotion.text = if (isMotionReduced) "Enabled" else "Disabled"

        // Load Text Size Display Name
        val textSizePrefs = getSharedPreferences("TextSizePrefs", Context.MODE_PRIVATE)
        val selectedSize = textSizePrefs.getString("selected_text_size", "Medium")
        binding.statusTextSize.text = selectedSize
    }

    override fun onResume() {
        super.onResume()
        // Refresh text size status when returning from TextSizeActivity
        val textSizePrefs = getSharedPreferences("TextSizePrefs", Context.MODE_PRIVATE)
        val selectedSize = textSizePrefs.getString("selected_text_size", "Medium")
        binding.statusTextSize.text = selectedSize
    }

    private fun saveSetting(key: String, value: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun showFeedback(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
