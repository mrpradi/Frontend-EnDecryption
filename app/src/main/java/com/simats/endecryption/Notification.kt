package com.simats.endecryption

data class Notification(
    val icon: Int,
    val title: String,
    val description: String,
    val timestamp: String,
    val isNew: Boolean
)
