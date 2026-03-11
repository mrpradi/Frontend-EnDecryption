package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText

class CreateCredentialsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_credentials)

        val usernameEditText = findViewById<TextInputEditText>(R.id.username_edit_text)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirm_password_edit_text)
        val createAccountButton = findViewById<Button>(R.id.create_account_button)
        val loginText = findViewById<TextView>(R.id.login_text)

        createAccountButton.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val username = usernameEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                val confirmPassword = confirmPasswordEditText.text.toString().trim()

                createAccountButton.isEnabled = username.length >= 3 && password.isNotEmpty() && password == confirmPassword
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        usernameEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        confirmPasswordEditText.addTextChangedListener(textWatcher)

        createAccountButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            saveUserCredentials(username, password)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun saveUserCredentials(username: String, password: String) {
        // TODO: Implement your database logic to save user credentials
    }
}