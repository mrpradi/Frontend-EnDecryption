package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val encryptCard = findViewById<CardView>(R.id.encrypt_card)
        val decryptCard = findViewById<CardView>(R.id.decrypt_card)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        encryptCard.setOnClickListener {
            startActivity(Intent(this, EncryptionActivity::class.java))
        }

        decryptCard.setOnClickListener {
            startActivity(Intent(this, DecryptionActivity::class.java))
        }

        setupBottomNavigation(bottomNavigation, R.id.navigation_home)
    }
}