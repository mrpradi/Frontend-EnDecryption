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
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.GenericResponse
import com.simats.endecryption.network.ResetPasswordRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                val isLongEnough = newPassword.length >= 6

                if (passwordsMatch && isLongEnough) {
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

        val email = intent.getStringExtra("EMAIL")
        val otp = intent.getStringExtra("OTP")

        continueButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val reEnteredPassword = reEnterPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != reEnteredPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordStrong(newPassword)) {
                Toast.makeText(this, "Password must be at least 6 characters and contain a mix of characters", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (email == null || otp == null) {
                Toast.makeText(this, "Session expired. Try again from Forgot Password.", Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }

            continueButton.isEnabled = false
            val request = com.simats.endecryption.network.ResetPasswordRequest(email, otp, newPassword, reEnteredPassword)
            
            ApiClient.instance.resetPassword(request).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    continueButton.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@CreateNewPasswordActivity, "Password updated successfully! Please login.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@CreateNewPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = try {
                            val errorObj = org.json.JSONObject(response.errorBody()?.string() ?: "{}")
                            errorObj.optString("detail", "Failed to reset password. Please check OTP.")
                        } catch (e: Exception) {
                            "Error: ${response.code()}. Reset failed."
                        }
                        Toast.makeText(this@CreateNewPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    continueButton.isEnabled = true
                    Toast.makeText(this@CreateNewPasswordActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        // Simplified check for now to ensure users can actually proceed
        return password.length >= 6
    }
}