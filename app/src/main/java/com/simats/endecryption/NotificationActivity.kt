package com.simats.endecryption

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.simats.endecryption.databinding.ActivityNotificationBinding

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private var notifications = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupBottomNavigation(binding.bottomNavigation, R.id.navigation_notifications)

        binding.clearAllButton.setOnClickListener {
            notifications.clear()
            notificationAdapter.notifyDataSetChanged()
            // Persist the clear action
            val prefs = getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
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
        notifications.addAll(NotificationHelper.getNotifications(this))
        notificationAdapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(notifications)
        binding.notificationsRecyclerView.adapter = notificationAdapter
    }
}
