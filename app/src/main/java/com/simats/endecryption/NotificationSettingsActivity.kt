package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityNotificationSettingsBinding

class NotificationSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding
    private val PREFS_NAME = "NotificationSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSettings()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.switchEncryption.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("encryption_completion", isChecked)
            showFeedback("Encryption alerts ${if (isChecked) "enabled" else "disabled"}")
        }

        binding.switchDecryption.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("decryption_alerts", isChecked)
            showFeedback("Decryption alerts ${if (isChecked) "enabled" else "disabled"}")
        }

        binding.switchTamper.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("tamper_detection", isChecked)
            showFeedback("Tamper detection alerts ${if (isChecked) "enabled" else "disabled"}")
        }

        binding.switchSecurity.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("security_alerts", isChecked)
            showFeedback("Security alerts ${if (isChecked) "enabled" else "disabled"}")
        }

        binding.switchUpdates.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("system_updates", isChecked)
            showFeedback("System updates ${if (isChecked) "enabled" else "disabled"}")
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        binding.switchEncryption.isChecked = prefs.getBoolean("encryption_completion", true)
        binding.switchDecryption.isChecked = prefs.getBoolean("decryption_alerts", true)
        binding.switchTamper.isChecked = prefs.getBoolean("tamper_detection", true)
        binding.switchSecurity.isChecked = prefs.getBoolean("security_alerts", true)
        binding.switchUpdates.isChecked = prefs.getBoolean("system_updates", true)
    }

    private fun saveSetting(key: String, value: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun showFeedback(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
