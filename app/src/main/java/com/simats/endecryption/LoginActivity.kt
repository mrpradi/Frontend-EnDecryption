package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.simats.endecryption.databinding.ActivityLoginBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.LoginRequest
import com.simats.endecryption.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(email, password)

            binding.loginButton.isEnabled = false

            ApiClient.instance.loginUser(loginRequest)
                .enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        binding.loginButton.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            val loginResponse = response.body()!!

                            // Save user session in SharedPreferences
                            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putInt("USER_ID", loginResponse.userId ?: -1)
                                putString("EMAIL", email)
                                apply()
                            }

                            Toast.makeText(
                                this@LoginActivity,
                                loginResponse.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("EMAIL", email)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Invalid email or password"
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        binding.loginButton.isEnabled = true
                        Log.e("LoginActivity", "Login failure: ${t.message}", t)
                        Toast.makeText(
                            this@LoginActivity,
                            "Server Error: ${t.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}
