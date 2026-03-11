package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityVerifyOtp2Binding

class VerifyOtp2Activity : BaseActivity() {

    private var otp: String? = null
    private lateinit var binding: ActivityVerifyOtp2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtp2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        otp = intent.getStringExtra("OTP")

        binding.continueButton.setOnClickListener {
            val enteredOtp = binding.otpEditText.text.toString().trim()
            if (enteredOtp == otp) {
                startActivity(Intent(this, CreateCredentialsActivity::class.java))
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }
}