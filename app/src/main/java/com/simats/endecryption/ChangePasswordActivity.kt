package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityChangePasswordBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.ChangePasswordRequest
import com.simats.endecryption.network.GenericResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        binding.changePasswordButton.setOnClickListener {
            handleChangePassword()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun handleChangePassword() {
        val currentPassword = binding.currentPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (userEmail == null) return

        val request = ChangePasswordRequest(userEmail!!, currentPassword, newPassword, confirmPassword)
        
        binding.changePasswordButton.isEnabled = false
        ApiClient.instance.changePassword(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                binding.changePasswordButton.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Failed to change password"
                    Toast.makeText(this@ChangePasswordActivity, error, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                binding.changePasswordButton.isEnabled = true
                Toast.makeText(this@ChangePasswordActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
