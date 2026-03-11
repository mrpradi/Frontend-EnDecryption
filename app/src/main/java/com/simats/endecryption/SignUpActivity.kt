package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.simats.endecryption.databinding.ActivitySignUpBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.GenericResponse
import com.simats.endecryption.network.RegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpActivity : BaseActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTextWatchers()

        binding.continueButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.nameEditText.addTextChangedListener(textWatcher)
        binding.emailEditText.addTextChangedListener(textWatcher)
        binding.ageEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)
        binding.confirmPasswordEditText.addTextChangedListener(textWatcher)
    }

    private fun checkInputs() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        binding.continueButton.isEnabled = name.isNotEmpty() && email.isNotEmpty() && 
                age.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
    }

    private fun validateInputs(): Boolean {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Invalid email address"
            return false
        } else {
            binding.emailInputLayout.error = null
        }

        if (password.length < 8) {
            binding.passwordInputLayout.error = "Password must be at least 8 characters"
            return false
        } else if (!password.any { it.isDigit() } || !password.any { it.isLetter() }) {
            binding.passwordInputLayout.error = "Password must contain both letters and numbers"
            return false
        } else if (!password.any { "!@#$%^&*(),.?\":{}|<>".contains(it) }) {
            binding.passwordInputLayout.error = "Password must contain at least one special character"
            return false
        } else {
            binding.passwordInputLayout.error = null
        }

        if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Passwords do not match"
            return false
        } else {
            binding.confirmPasswordInputLayout.error = null
        }

        return true
    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().toIntOrNull() ?: 0
        val password = binding.passwordEditText.text.toString().trim()

        val request = RegisterRequest(name, email, age, password)
        
        binding.continueButton.isEnabled = false
        
        ApiClient.instance.registerUser(request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                binding.continueButton.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@SignUpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SignUpActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("EMAIL", email)
                    intent.putExtra("FROM_SIGNUP", true)
                    // We don't need to pass all details now as they are already in the DB
                    startActivity(intent)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                binding.continueButton.isEnabled = true
                Toast.makeText(this@SignUpActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
