package com.simats.endecryption

import android.os.Bundle
import com.simats.endecryption.databinding.ActivityTermsAndConditions2Binding

class TermsAndConditions2Activity : BaseActivity() {

    private lateinit var binding: ActivityTermsAndConditions2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsAndConditions2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.closeButton.setOnClickListener { finish() }
    }
}
