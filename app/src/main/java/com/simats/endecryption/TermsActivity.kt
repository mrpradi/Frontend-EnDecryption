package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import com.simats.endecryption.databinding.ActivityTermsBinding

class TermsActivity : BaseActivity() {

    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.continueButton.isEnabled = false

        binding.agreeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.continueButton.isEnabled = isChecked
        }

        binding.continueButton.setOnClickListener {
            if (binding.agreeCheckbox.isChecked) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}