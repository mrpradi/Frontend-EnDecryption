package com.simats.endecryption.network

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val name: String,
    val email: String,
    val age: Int,
    val password: String
)

data class GenericResponse(
    val message: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    val age: Int
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val age: Int
)

data class ChangePasswordRequest(
    val email: String,
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

data class FileHistoryResponse(
    val user: String,
    val files: List<FileItem>
)

data class FileItem(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_format") val fileFormat: String,
    @SerializedName("file_type") val fileType: String,
    @SerializedName("decryption_key") val decryptionKey: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("created_at") val createdAt: String? = null
)
