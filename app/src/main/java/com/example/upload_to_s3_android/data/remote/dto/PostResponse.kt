package com.example.upload_to_s3_android.data.remote.dto

import kotlinx.serialization.Serializable
import org.json.JSONArray

@Serializable
data class PostResponse(
    val nTarjeta: String,
    val nVal: String,
    val fExpira: String,
    val fVal: String,
    val nCvc: String,
    val pCvc: String,
    val stringsValues: JSONArray,
    val dataTarjeta: String
)
