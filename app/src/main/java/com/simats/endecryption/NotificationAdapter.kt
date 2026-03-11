package com.simats.endecryption

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simats.endecryption.databinding.ItemNotificationBinding

class NotificationAdapter(private val notifications: List<Notification>) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.notificationIcon.setImageResource(notification.icon)
            binding.notificationTitle.text = notification.title
            binding.notificationDescription.text = notification.description
            binding.notificationTimestamp.text = notification.timestamp
            binding.newNotificationIndicator.visibility = if (notification.isNew) View.VISIBLE else View.GONE
        }
    }
}
