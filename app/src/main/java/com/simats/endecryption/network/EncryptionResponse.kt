package com.simats.endecryption.network

import com.google.gson.annotations.SerializedName

data class EncryptionResponse(
    @SerializedName("message") val message: String,
    @SerializedName("file_id") val fileId: Int,
    @SerializedName("decryption_key") val decryptionKey: String,
    @SerializedName("encrypted_file") val encryptedFileName: String
)
