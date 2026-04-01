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

            // Call backend to send OTP
            sendOtpButton.isEnabled = false
            val request = com.simats.endecryption.network.ForgotPasswordRequest(email)
            com.simats.endecryption.network.ApiClient.instance.forgotPassword(request).enqueue(object : retrofit2.Callback<com.simats.endecryption.network.GenericResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.endecryption.network.GenericResponse>, response: retrofit2.Response<com.simats.endecryption.network.GenericResponse>) {
                    sendOtpButton.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "OTP Sent", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ForgotPasswordActivity, VerifyOtp2Activity::class.java)
                        intent.putExtra("EMAIL", email)
                        startActivity(intent)
                    } else {
                        val errorMsg = try {
                            val errorObj = org.json.JSONObject(response.errorBody()?.string() ?: "{}")
                            errorObj.getString("detail")
                        } catch (e: Exception) {
                            "Failed to send OTP"
                        }
                        Toast.makeText(this@ForgotPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.simats.endecryption.network.GenericResponse>, t: Throwable) {
                    sendOtpButton.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        backButton.setOnClickListener {
            finish()
        }

        needHelpButton.setOnClickListener {
            startActivity(Intent(this, HelpFaqActivity::class.java))
        }
    }
}