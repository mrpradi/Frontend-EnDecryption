package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityVerifyOtp2Binding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.GenericResponse
import com.simats.endecryption.network.VerifyOtpRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyOtp2Activity : BaseActivity() {

    private lateinit var binding: ActivityVerifyOtp2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtp2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra("EMAIL")
        
        binding.continueButton.setOnClickListener {
            val enteredOtp = binding.otpEditText.text.toString().trim()
            if (enteredOtp.isEmpty() || email == null) {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.continueButton.isEnabled = false
            val request = VerifyOtpRequest(email, enteredOtp)
            
            // Reverting to verifyResetOtp as some backends distinguish between signup and reset OTPs
            ApiClient.instance.verifyResetOtp(request).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    binding.continueButton.isEnabled = true
                    if (response.isSuccessful) {
                        val intent = Intent(this@VerifyOtp2Activity, CreateNewPasswordActivity::class.java)
                        intent.putExtra("EMAIL", email)
                        intent.putExtra("OTP", enteredOtp)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = try {
                            val errorObj = org.json.JSONObject(response.errorBody()?.string() ?: "{}")
                            errorObj.optString("detail", "Invalid OTP or session expired")
                        } catch (e: Exception) {
                            "Verification failed (Error: ${response.code()})"
                        }
                        Toast.makeText(this@VerifyOtp2Activity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    binding.continueButton.isEnabled = true
                    Toast.makeText(this@VerifyOtp2Activity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.resendOtpText.setOnClickListener {
             if (email == null) return@setOnClickListener
             val request = com.simats.endecryption.network.ForgotPasswordRequest(email)
             Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show()
             
             ApiClient.instance.resendOtp(request).enqueue(object : Callback<GenericResponse> {
                 override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                     if (response.isSuccessful) {
                         Toast.makeText(this@VerifyOtp2Activity, "New OTP sent!", Toast.LENGTH_SHORT).show()
                     } else {
                         Toast.makeText(this@VerifyOtp2Activity, "Failed to resend OTP", Toast.LENGTH_SHORT).show()
                     }
                 }

                 override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                     Toast.makeText(this@VerifyOtp2Activity, "Network Error", Toast.LENGTH_SHORT).show()
                 }
             })
        }
    }
}