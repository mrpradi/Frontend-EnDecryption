package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import android.view.View
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

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        setupRecyclerView()
        
        if (userEmail != null) {
            fetchUserFiles(userEmail!!)
        } else {
            Toast.makeText(this, "Email not found. Please login again.", Toast.LENGTH_SHORT).show()
        }

        binding.headerLayout.findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }

        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_history)
    }

    private fun setupRecyclerView() {
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(mutableListOf())
        binding.historyRecyclerView.adapter = adapter
    }

    private fun fetchUserFiles(email: String) {
        ApiClient.instance.getHistory(email).enqueue(object : Callback<FileHistoryResponse> {
            override fun onResponse(call: Call<FileHistoryResponse>, response: Response<FileHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val files = response.body()?.files ?: emptyList()
                    
                    val historyItems = files.map { file ->
                        val type = file.fileType ?: "Unknown"
                        val format = file.fileFormat ?: ""
                        val capitalizedType = if (type.isNotBlank()) {
                            type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        } else {
                            "Unknown"
                        }
                        
                        HistoryItem(
                            title = file.fileName ?: "Unnamed File",
                            subtitle = if (format.isNotBlank()) "$capitalizedType - $format" else capitalizedType,
                            value = "",
                            timestamp = formatTimestamp(file.createdAt)
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
        if (timestamp.isNullOrEmpty()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            if (date != null) outputFormat.format(date) else timestamp
        } catch (e: Exception) {
            timestamp
        }
    }
}
