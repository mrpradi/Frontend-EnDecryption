package com.simats.endecryption

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {

    private const val CHANNEL_ID = "security_alerts_channel"
    private const val PREFS_NAME = "NotificationPrefs"
    private const val SETTINGS_PREFS = "NotificationSettings"

    fun showNotification(context: Context, title: String, description: String, iconRes: Int, settingKey: String) {
        // 1. Check if this type of notification is enabled in settings
        val settingsPrefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        if (!settingsPrefs.getBoolean(settingKey, true)) return

        // 2. Save notification to history (silently)
        saveNotificationToHistory(context, title, description, iconRes)

        // 3. Show system push notification
        createNotificationChannel(context)

        val intent = Intent(context, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Security Alerts"
            val descriptionText = "Notifications for encryption and security events"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveNotificationToHistory(context: Context, title: String, description: String, iconRes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("notification_list", null)
        
        val type = object : TypeToken<MutableList<Notification>>() {}.type
        val notificationList: MutableList<Notification> = if (json == null) {
            mutableListOf()
        } else {
            gson.fromJson(json, type)
        }

        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val newNotification = Notification(iconRes, title, description, currentTime, true)
        
        // Add to beginning of list
        notificationList.add(0, newNotification)
        
        // Clear the "clear_all" flag since we have a new notification
        prefs.edit()
            .putString("notification_list", gson.toJson(notificationList))
            .putBoolean("clear_all", false)
            .apply()
    }

    fun getNotifications(context: Context): List<Notification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean("clear_all", false)) return emptyList()

        val gson = Gson()
        val json = prefs.getString("notification_list", null)
        val type = object : TypeToken<List<Notification>>() {}.type
        
        return if (json == null) {
            listOf(Notification(R.drawable.ic_notification_bell, "Welcome to EnDecryption!", "You can find all your system activity and security alerts here.", "Just now", true))
        } else {
            gson.fromJson(json, type)
        }
    }
}
