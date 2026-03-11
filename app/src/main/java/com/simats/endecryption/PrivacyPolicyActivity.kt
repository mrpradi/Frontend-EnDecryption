package com.simats.endecryption

import android.os.Bundle
import com.simats.endecryption.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : BaseActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.understandButton.setOnClickListener { finish() }
    }
}
