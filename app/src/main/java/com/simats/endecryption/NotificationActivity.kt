package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.endecryption.databinding.ActivityNotificationBinding

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private var notifications = mutableListOf<Notification>()
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("EMAIL", null)

        setupRecyclerView()
        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_notifications)

        binding.clearAllButton.setOnClickListener {
            notifications.clear()
            notificationAdapter.notifyDataSetChanged()
            // Persist the clear action
            val userEmail = getSharedPreferences("UserSession", Context.MODE_PRIVATE).getString("EMAIL", "default")
            val prefs = getSharedPreferences("NotificationPrefs_$userEmail", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("clear_all", true)
                .remove("notification_list")
                .apply()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        notifications.clear()
        
        // Load local ones first (optional fallback)
        notifications.addAll(NotificationHelper.getNotifications(this))
        notificationAdapter.notifyDataSetChanged()

        // Fetch from server
        userEmail?.let { email ->
            com.simats.endecryption.network.ApiClient.instance.getNotifications(email).enqueue(object : retrofit2.Callback<com.simats.endecryption.network.NotificationRemoteResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.endecryption.network.NotificationRemoteResponse>, response: retrofit2.Response<com.simats.endecryption.network.NotificationRemoteResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val remoteList = response.body()!!.notifications
                        
                        // Map remote items to local UI Notification objects
                        remoteList.forEach { item ->
                            val alreadyExists = notifications.any { it.description == item.message }
                            if (!alreadyExists) {
                                notifications.add(Notification(
                                    R.drawable.ic_notification_bell,
                                    "System Notification",
                                    item.message,
                                    item.createdAt ?: "Recently",
                                    true
                                ))
                            }
                        }
                        notificationAdapter.notifyDataSetChanged()
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.simats.endecryption.network.NotificationRemoteResponse>, t: Throwable) {
                    // Fail silently or log
                }
            })
        }
    }

    private fun setupRecyclerView() {
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(notifications)
        binding.notificationsRecyclerView.adapter = notificationAdapter
    }
}
