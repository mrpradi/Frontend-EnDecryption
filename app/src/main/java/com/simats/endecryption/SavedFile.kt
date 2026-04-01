package com.simats.endecryption

data class SavedFile(
    val id: Int? = null,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val filePath: String? = null
)
