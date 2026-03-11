package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.simats.endecryption.databinding.ActivityAppSettingsBinding

class AppSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadSettings()
        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_settings)
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.editProfileButton.setOnClickListener { startActivity(Intent(this, EditProfileActivity::class.java)) }
        binding.changePasswordButton.setOnClickListener { startActivity(Intent(this, ChangePasswordActivity::class.java)) }
        binding.privacySecurityButton.setOnClickListener { startActivity(Intent(this, PrivacySecurityActivity::class.java)) }
        binding.notificationsButton.setOnClickListener { startActivity(Intent(this, NotificationSettingsActivity::class.java)) }
        binding.accessibilityButton.setOnClickListener { startActivity(Intent(this, AccessibilitySettingsActivity::class.java)) }
        binding.helpFaqButton.setOnClickListener { startActivity(Intent(this, HelpFaqActivity::class.java)) }
        binding.aboutButton.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
        binding.termsConditionsButton.setOnClickListener { startActivity(Intent(this, TermsAndConditions2Activity::class.java)) }
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val autoIntegrityCheck = sharedPreferences.getBoolean("auto_integrity_check", true)
        binding.autoIntegrityCheckSwitch.isChecked = autoIntegrityCheck

        binding.autoIntegrityCheckSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("auto_integrity_check", isChecked).apply()
        }
    }
}
