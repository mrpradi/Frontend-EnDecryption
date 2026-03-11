package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.closeButton.setOnClickListener { finish() }

        binding.termsButton.setOnClickListener {
            try {
                val intent = Intent(this, TermsAndConditions2Activity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Terms of Service page not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.privacyPolicyButton.setOnClickListener {
            try {
                val intent = Intent(this, PrivacyPolicyActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Privacy Policy page not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.licensesButton.setOnClickListener {
             try {
                val intent = Intent(this, LicensesActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Licenses page not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
