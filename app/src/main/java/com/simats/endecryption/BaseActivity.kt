package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val sharedPreferences = newBase?.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val textSize = sharedPreferences?.getFloat("TextSize", 20f) ?: 20f
        val fontScale = getFontScaleForTextSize(textSize)
        
        val configuration = newBase?.resources?.configuration?.let { Configuration(it) } ?: Configuration()
        configuration.fontScale = fontScale
        
        val context = newBase?.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    private fun getFontScaleForTextSize(textSize: Float): Float {
        return when (textSize) {
            16f -> 0.85f // Small
            20f -> 1.0f  // Medium
            24f -> 1.15f // Large
            28f -> 1.3f  // Extra Large
            else -> 1.0f
        }
    }

    fun setupBottomNavigation(bottomNavigationView: BottomNavigationView, selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId
        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }
            
            val intent = when (item.itemId) {
                R.id.navigation_home -> Intent(this, MainActivity::class.java)
                R.id.navigation_files -> Intent(this, SavedFilesActivity::class.java)
                R.id.navigation_history -> Intent(this, HistoryActivity::class.java)
                R.id.navigation_notifications -> Intent(this, NotificationActivity::class.java)
                R.id.navigation_profile -> Intent(this, ProfileActivity::class.java)
                R.id.navigation_settings -> Intent(this, AppSettingsActivity::class.java)
                else -> null
            }

            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(it)
                true
            } ?: false
        }
    }
}
