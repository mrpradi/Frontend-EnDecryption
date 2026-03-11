package com.simats.endecryption

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText

class CreateNewPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_password)

        val backButton = findViewById<ImageView>(R.id.back_button)
        val newPasswordEditText = findViewById<TextInputEditText>(R.id.new_password_edit_text)
        val reEnterPasswordEditText = findViewById<TextInputEditText>(R.id.re_enter_password_edit_text)
        val continueButton = findViewById<Button>(R.id.continue_button)

        backButton.setOnClickListener {
            finish()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newPassword = newPasswordEditText.text.toString()
                val reEnteredPassword = reEnterPasswordEditText.text.toString()

                val passwordsMatch = newPassword.isNotEmpty() && newPassword == reEnteredPassword
                val isStrong = isPasswordStrong(newPassword)

                if (passwordsMatch && isStrong) {
                    continueButton.isEnabled = true
                    continueButton.setBackgroundColor(Color.parseColor("#00C89C"))
                    continueButton.setTextColor(Color.WHITE)
                } else {
                    continueButton.isEnabled = false
                    continueButton.setBackgroundColor(Color.parseColor("#2A445E"))
                    continueButton.setTextColor(Color.parseColor("#808080"))
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        newPasswordEditText.addTextChangedListener(textWatcher)
        reEnterPasswordEditText.addTextChangedListener(textWatcher)

        continueButton.setOnClickListener {
            // Here you would update the password in your database
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$".toRegex()
        return passwordPattern.matches(password)
    }
}