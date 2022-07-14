package com.example.upload_to_s3_android.data.remote

import android.util.Log
import com.example.upload_to_s3_android.data.remote.dto.PostResponse
import com.example.upload_to_s3_android.data.remote.dto.PostUserResponse
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.json.JSONObject

class PostsUserServiceImpl(
    private val client: HttpClient
) : PostsUserService {

    override suspend fun getPosts(): List<PostUserResponse> {
        return try {
            client.post { url(HttpRoutes.POSTS_USER) }
        } catch(e: RedirectResponseException) {
            // 3xx - responses
            println("Error: ${e.response.status.description}")
            emptyList()
        } catch(e: ClientRequestException) {
            // 4xx - responses
            println("Error: ${e.response.status.description}")
            emptyList()
        } catch(e: ServerResponseException) {
            // 5xx - responses
            println("Error: ${e.response.status.description}")
            emptyList()
        } catch(e: Exception) {
            println("Error: ${e.message}")
            emptyList()
        }
    }

    override suspend fun createPost(): PostUserResponse? {

        var resp = PostUserResponse("")
        try {
             val res = client.post<String> {
                url(HttpRoutes.POSTS_USER)
                contentType(ContentType.Application.Json)
                body = "\"persona\": {\n" +
                        "        \"email\":\"oscarquerevalu@gmail.com\",\n" +
                        "        \"password\":\"hola123\"\n" +
                        "    }"
            }

            Log.e("res", res)
            var respn = JSONObject(res)
            var obj = respn.getJSONObject("input")

            Log.e("obj", obj.toString())
            resp = PostUserResponse("")

        } catch(e: RedirectResponseException) {
            // 3xx - responses
            println("Error: ${e.response.status.description}")
            null
        } catch(e: ClientRequestException) {
            // 4xx - responses
            println("Error: ${e.response.status.description}")
            null
        } catch(e: ServerResponseException) {
            // 5xx - responses
            println("Error: ${e.response.status.description}")
            null
        } catch(e: Exception) {
            println("Error: ${e.message}")
            null
        }
        return resp
    }
}