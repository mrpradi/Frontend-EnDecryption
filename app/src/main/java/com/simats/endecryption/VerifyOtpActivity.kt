package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
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
                verifyOtp(email, enteredOtp, isFromSignUp)
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.resendOtpText.setOnClickListener {
            // Your backend doesn't have a resend OTP endpoint yet
            Toast.makeText(this, "Resend OTP not implemented in backend", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyOtp(email: String, otp: String, isFromSignUp: Boolean) {
        val request = VerifyOtpRequest(email, otp)
        
        binding.verifyOtpButton.isEnabled = false
        
        ApiClient.instance.verifyOtp(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                binding.verifyOtpButton.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyOtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                    if (isFromSignUp) {
                        Toast.makeText(this@VerifyOtpActivity, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@VerifyOtpActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        // This handles Forgot Password flow
                        startActivity(Intent(this@VerifyOtpActivity, CreateNewPasswordActivity::class.java))
                    }
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Verification failed"
                    Toast.makeText(this@VerifyOtpActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                binding.verifyOtpButton.isEnabled = true
                Toast.makeText(this@VerifyOtpActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
