package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text)
        val sendOtpButton = findViewById<Button>(R.id.send_otp_button)
        val backButton = findViewById<TextView>(R.id.back_button)
        val needHelpButton = findViewById<Button>(R.id.need_help_button)

        sendOtpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter Email id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Toast.makeText(this, "Enter a valid Email id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // For now, let's generate a random 6-digit OTP until the backend is connected.
            val otp = (100000..999999).random().toString()
            val intent = Intent(this, VerifyOtpActivity::class.java)
            intent.putExtra("OTP", otp)
            intent.putExtra("EMAIL", email)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }

        needHelpButton.setOnClickListener {
            startActivity(Intent(this, HelpFaqActivity::class.java))
        }
    }
}