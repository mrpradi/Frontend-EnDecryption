package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.google.gson.JsonParser
import com.simats.endecryption.databinding.ActivityVerifyOtpBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.GenericResponse
import com.simats.endecryption.network.VerifyOtpRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyOtpActivity : BaseActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra("EMAIL") ?: ""
        val name = intent.getStringExtra("NAME") ?: "User"
        val isFromSignUp = intent.getBooleanExtra("FROM_SIGNUP", false)

        binding.verifyOtpButton.isEnabled = false

        binding.otpEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.verifyOtpButton.isEnabled = s?.length == 6
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.verifyOtpButton.setOnClickListener {
            val enteredOtp = binding.otpEditText.text.toString().trim()
            if (enteredOtp.length == 6) {
                verifyOtp(email, name, enteredOtp, isFromSignUp)
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.resendOtpText.setOnClickListener {
            val request = com.simats.endecryption.network.ForgotPasswordRequest(email)
            Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show()
            
            ApiClient.instance.resendOtp(request).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@VerifyOtpActivity, "New OTP sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@VerifyOtpActivity, "Failed to resend OTP", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    Toast.makeText(this@VerifyOtpActivity, "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun verifyOtp(email: String, name: String, otp: String, isFromSignUp: Boolean) {
        val request = VerifyOtpRequest(email, otp)
        
        binding.verifyOtpButton.isEnabled = false
        
        ApiClient.instance.verifyOtp(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                binding.verifyOtpButton.isEnabled = true
                if (response.isSuccessful) {
                    if (isFromSignUp) {
                        // Show welcome notification
                        NotificationHelper.showNotification(
                            this@VerifyOtpActivity,
                            "Welcome to EnDecryption",
                            "Welcome $name! Your account has been created successfully.",
                            R.drawable.ic_notification_bell,
                            "security_alerts"
                        )
                        
                        Toast.makeText(this@VerifyOtpActivity, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@VerifyOtpActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Forgot Password flow - redirect to create new password
                        val intent = Intent(this@VerifyOtpActivity, CreateNewPasswordActivity::class.java)
                        intent.putExtra("EMAIL", email)
                        intent.putExtra("OTP", otp) // Pass the OTP for verification in resetPassword
                        startActivity(intent)
                    }
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        // FastAPI returns error detail in "detail" field
                        JsonParser.parseString(errorBody ?: "{}").asJsonObject.get("detail").asString
                    } catch (e: Exception) {
                        "Invalid OTP or verification failed"
                    }
                    Toast.makeText(this@VerifyOtpActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                binding.verifyOtpButton.isEnabled = true
                Toast.makeText(this@VerifyOtpActivity, "Network Error: Check your connection", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
