package com.simats.endecryption.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val message: String,
    @SerializedName("user_id") val userId: Int?
)
