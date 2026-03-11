package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityEditProfileBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.GenericResponse
import com.simats.endecryption.network.UpdateProfileRequest
import com.simats.endecryption.network.UserProfile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var currentEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        if (currentEmail != null) {
            fetchCurrentProfile(currentEmail!!)
        }

        binding.saveChangesButton.setOnClickListener {
            updateProfile()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchCurrentProfile(email: String) {
        ApiClient.instance.getProfile(email).enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    binding.nameEditText.setText(profile.name)
                    binding.emailEditText.setText(profile.email)
                    binding.ageEditText.setText(profile.age.toString())
                }
            }
            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Failed to load current data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateProfile() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val ageStr = binding.ageEditText.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull() ?: 0
        if (age <= 0) {
            Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentEmail == null) return

        val request = UpdateProfileRequest(name, email, age)
        ApiClient.instance.updateProfile(currentEmail!!, request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    
                    // Update session email if it changed
                    if (email != currentEmail) {
                        getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                            .edit().putString("EMAIL", email).apply()
                    }
                    
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Update failed"
                    Toast.makeText(this@EditProfileActivity, error, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
