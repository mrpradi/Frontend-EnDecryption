package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.endecryption.databinding.ActivitySavedFilesBinding
import java.io.File

class SavedFilesActivity : BaseActivity() {

    private lateinit var binding: ActivitySavedFilesBinding
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve email from SharedPreferences
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        binding.savedFilesRecyclerView.layoutManager = LinearLayoutManager(this)

        val savedFiles = if (userEmail != null) getSavedFilesForUser(userEmail!!) else emptyList()
        binding.savedFilesRecyclerView.adapter = SavedFilesAdapter(savedFiles)

        binding.encryptedFilesCount.text = "${savedFiles.size} encrypted files"
        binding.totalFilesCount.text = savedFiles.size.toString()

        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_files)

        val resultIntent = Intent()
        resultIntent.putExtra("fileCount", savedFiles.size)
        setResult(RESULT_OK, resultIntent)
        
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun getSavedFilesForUser(email: String): List<SavedFile> {
        // Create a user-specific directory for encrypted files
        val userDirName = email.replace("@", "_").replace(".", "_")
        val encryptedFilesDir = File(filesDir, "encrypted/$userDirName")
        
        if (!encryptedFilesDir.exists()) {
            encryptedFilesDir.mkdirs()
            return emptyList()
        }

        return encryptedFilesDir.listFiles()?.map { file ->
            SavedFile(file.name, file.length(), file.lastModified())
        } ?: emptyList()
    }
}
