package com.simats.endecryption

import android.os.Bundle
import com.simats.endecryption.databinding.ActivityLicensesBinding

class LicensesActivity : BaseActivity() {

    private lateinit var binding: ActivityLicensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButtonTop.setOnClickListener { finish() }
        binding.backButtonBottom.setOnClickListener { finish() }
    }
}
