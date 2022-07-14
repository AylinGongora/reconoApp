package com.example.upload_to_s3_android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostUserRequest(
    val body: String,
    val title: String,
    val userId: Int
)
