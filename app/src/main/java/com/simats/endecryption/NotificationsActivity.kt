package com.simats.endecryption

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val notificationsRecyclerView = findViewById<RecyclerView>(R.id.notifications_recycler_view)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)

        val notificationsList = ArrayList<Notification>()
        // TODO: Populate notificationsList with actual data

        val adapter = NotificationsAdapter(notificationsList)
        notificationsRecyclerView.adapter = adapter
    }
}