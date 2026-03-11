package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.endecryption.databinding.ActivityHistoryBinding
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.FileHistoryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve email from SharedPreferences if not in Intent
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(mutableListOf())
        binding.historyRecyclerView.adapter = adapter

        if (userEmail != null) {
            fetchUserFiles(userEmail!!)
        } else {
            Toast.makeText(this, "Email not found. Please login again.", Toast.LENGTH_SHORT).show()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_history)
    }

    private fun fetchUserFiles(email: String) {
        ApiClient.instance.getUserFiles(email).enqueue(object : Callback<FileHistoryResponse> {
            override fun onResponse(call: Call<FileHistoryResponse>, response: Response<FileHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val files = response.body()!!.files
                    
                    // Filter/Sort from newest to oldest based on ID or createdAt if available
                    // Assuming higher ID means newer, or use createdAt
                    val sortedFiles = files.sortedByDescending { it.id }

                    val historyItems = sortedFiles.map { file ->
                        val displayTime = formatTimestamp(file.createdAt)
                        HistoryItem(
                            title = file.fileName,
                            subtitle = "${file.fileType.replaceFirstChar { it.uppercase() }} - ${file.fileFormat}",
                            value = "",
                            timestamp = displayTime
                        )
                    }
                    adapter.updateData(historyItems)
                } else {
                    Toast.makeText(this@HistoryActivity, "Failed to fetch history", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<FileHistoryResponse>, t: Throwable) {
                Toast.makeText(this@HistoryActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp == null) return ""
        return try {
            // Adjust pattern to match your backend's date format (e.g., ISO 8601)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            if (date != null) outputFormat.format(date) else timestamp
        } catch (e: Exception) {
            timestamp // Return original if parsing fails
        }
    }
}
