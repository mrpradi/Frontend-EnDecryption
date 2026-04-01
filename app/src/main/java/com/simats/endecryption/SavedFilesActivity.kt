package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.endecryption.databinding.ActivitySavedFilesBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.FileHistoryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SavedFilesActivity : BaseActivity() {

    private lateinit var binding: ActivitySavedFilesBinding
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve email from SharedPreferences
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("EMAIL", null)

        binding.savedFilesRecyclerView.layoutManager = LinearLayoutManager(this)

        if (userEmail != null) {
            fetchSavedFilesFromBackend(userEmail!!)
        } else {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
        }

        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_files)
        
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchSavedFilesFromBackend(email: String) {
        // Show progress or something if needed
        binding.totalFilesCount.text = "..."
        binding.encryptedFilesCount.text = "Fetching files..."

        ApiClient.instance.getUserFiles(email).enqueue(object : Callback<FileHistoryResponse> {
            override fun onResponse(call: Call<FileHistoryResponse>, response: Response<FileHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val fileList = response.body()!!.files
                    
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    
                    // Convert backend FileItem to SavedFile for the adapter
                    val savedFiles = fileList.map { fileItem ->
                        val date = try {
                            fileItem.createdAt?.let { sdf.parse(it) }
                        } catch (e: Exception) {
                            null
                        }
                        
                        SavedFile(
                            id = fileItem.id,
                            name = fileItem.fileName,
                            size = fileItem.fileSize,
                            lastModified = date?.time ?: 0L,
                            filePath = fileItem.filePath
                        )
                    }

                    binding.savedFilesRecyclerView.adapter = SavedFilesAdapter(savedFiles)
                    binding.encryptedFilesCount.text = "${savedFiles.size} encrypted files"
                    binding.totalFilesCount.text = savedFiles.size.toString()
                } else {
                    Log.e("SavedFilesActivity", "Error fetching files: ${response.code()}")
                    Toast.makeText(this@SavedFilesActivity, "Failed to load files", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FileHistoryResponse>, t: Throwable) {
                Log.e("SavedFilesActivity", "Network error: ${t.message}")
                Toast.makeText(this@SavedFilesActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
