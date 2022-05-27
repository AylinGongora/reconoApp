package com.example.upload_to_s3_android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostResponse(
    val nTarjeta: String,
    val nVal: String,
    val fExpira: String,
    val fVal: String
)
