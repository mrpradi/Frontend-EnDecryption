package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.simats.endecryption.databinding.ActivityPrivacySecurityBinding
import java.io.File

class PrivacySecurityActivity : BaseActivity() {

    private lateinit var binding: ActivityPrivacySecurityBinding
    private val PREFS_NAME = "PrivacySettings"
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("EMAIL", null)

        loadSettings()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.cancelButton.setOnClickListener { finish() }

        binding.saveSettingsButton.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.deleteDataButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Update status text when switches are toggled
        binding.switchDataUsage.setOnCheckedChangeListener { _, isChecked ->
            binding.statusDataUsage.text = if (isChecked) "Enabled" else "Disabled"
        }
        binding.switchMetadata.setOnCheckedChangeListener { _, isChecked ->
            binding.statusMetadata.text = if (isChecked) "Enabled" else "Disabled"
        }
        binding.switchHistory.setOnCheckedChangeListener { _, isChecked ->
            binding.statusHistory.text = if (isChecked) "Enabled" else "Disabled"
        }
        binding.switchAnalytics.setOnCheckedChangeListener { _, isChecked ->
            binding.statusAnalytics.text = if (isChecked) "Enabled" else "Disabled"
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val dataUsage = prefs.getBoolean("data_usage_consent", true)
        val metadata = prefs.getBoolean("metadata_storage", true)
        val history = prefs.getBoolean("decryption_history", true)
        val analytics = prefs.getBoolean("analytics_tracking", false)

        binding.switchDataUsage.isChecked = dataUsage
        binding.switchMetadata.isChecked = metadata
        binding.switchHistory.isChecked = history
        binding.switchAnalytics.isChecked = analytics

        binding.statusDataUsage.text = if (dataUsage) "Enabled" else "Disabled"
        binding.statusMetadata.text = if (metadata) "Enabled" else "Disabled"
        binding.statusHistory.text = if (history) "Enabled" else "Disabled"
        binding.statusAnalytics.text = if (analytics) "Enabled" else "Disabled"
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("data_usage_consent", binding.switchDataUsage.isChecked)
            putBoolean("metadata_storage", binding.switchMetadata.isChecked)
            putBoolean("decryption_history", binding.switchHistory.isChecked)
            putBoolean("analytics_tracking", binding.switchAnalytics.isChecked)
            apply()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Encrypted Data?")
            .setMessage("This action is permanent and cannot be undone. All your encrypted files and metadata will be lost.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllData() {
        // 1. Delete files from "encrypted" directory
        val encryptedFilesDir = File(filesDir, "encrypted")
        if (encryptedFilesDir.exists() && encryptedFilesDir.isDirectory) {
            encryptedFilesDir.listFiles()?.forEach { it.delete() }
        }

        // 2. Clear notifications
        val notificationPrefs = getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        notificationPrefs.edit().putBoolean("clear_all", true).apply()

        // 3. Clear history locally
        val historyPrefs = getSharedPreferences("HistoryPrefs", Context.MODE_PRIVATE)
        historyPrefs.edit().clear().apply()

        // 4. Wipe data from server
        if (userEmail != null) {
            com.simats.endecryption.network.ApiClient.instance.wipeData(userEmail!!).enqueue(object : retrofit2.Callback<com.simats.endecryption.network.GenericResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.endecryption.network.GenericResponse>, response: retrofit2.Response<com.simats.endecryption.network.GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@PrivacySecurityActivity, "All remote and local data has been permanently deleted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@PrivacySecurityActivity, "Local data cleared, but server deletion failed", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.simats.endecryption.network.GenericResponse>, t: Throwable) {
                    Toast.makeText(this@PrivacySecurityActivity, "Local data cleared, but could not connect to server", Toast.LENGTH_LONG).show()
                }
            })
        } else {
            Toast.makeText(this, "Local data has been deleted", Toast.LENGTH_LONG).show()
        }
    }
}
