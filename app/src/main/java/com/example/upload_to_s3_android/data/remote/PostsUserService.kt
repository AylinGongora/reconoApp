package com.example.upload_to_s3_android.data.remote

import com.example.upload_to_s3_android.data.remote.dto.PostResponse
import com.example.upload_to_s3_android.data.remote.dto.PostUserResponse
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*

interface PostsUserService {

    suspend fun getPosts(): List<PostUserResponse>

    suspend fun createPost(): PostUserResponse?

    companion object {
        fun create(): PostsUserService {
            return PostsUserServiceImpl(
                client = HttpClient(Android) {
                    install(Logging) {
                        level = LogLevel.ALL
                    }
                    install(JsonFeature) {
                        serializer = KotlinxSerializer()
                    }
                }
            )
        }
    }
}