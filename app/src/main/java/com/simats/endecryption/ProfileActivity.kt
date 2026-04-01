package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.simats.endecryption.databinding.ActivityProfileBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.UserProfile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("EMAIL", userEmail)
            startActivity(intent)
        }

        binding.editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("EMAIL", userEmail)
            startActivity(intent)
        }

        binding.changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("EMAIL", userEmail)
            startActivity(intent)
        }

        binding.appSettingsButton.setOnClickListener {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    val sessionPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                    sessionPref.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_profile)
    }

    override fun onResume() {
        super.onResume()
        if (userEmail != null) {
            fetchProfileData(userEmail!!)
        }
    }

    private fun fetchProfileData(email: String) {
        ApiClient.instance.getProfile(email).enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    updateUI(profile)
                    
                    // Update session email if it changed
                    if (profile.email != userEmail) {
                        userEmail = profile.email
                        getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                            .edit().putString("EMAIL", profile.email).apply()
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(profile: UserProfile) {
        binding.userName.text = profile.name
        binding.userEmail.text = profile.email
        
        binding.fullNameInfo.text = "Full Name: ${profile.name}"
        binding.emailInfo.text = "Email: ${profile.email}"
        binding.ageInfo.text = "Age: ${profile.age} years"
    }
}
