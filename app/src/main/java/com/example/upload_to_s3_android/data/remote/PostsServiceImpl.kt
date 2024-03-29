package com.example.upload_to_s3_android.data.remote

import com.example.upload_to_s3_android.data.remote.dto.PostResponse
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.json.JSONArray
import org.json.JSONObject

class PostsServiceImpl(
    private val client: HttpClient
) : PostsService {

    override suspend fun getPosts(): List<PostResponse> {
        return try {
            client.post { url(HttpRoutes.POSTS) }
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

    override suspend fun createPost(): PostResponse? {

        var resp = PostResponse("", "", "", "","","", JSONArray() ,"")
        try {
             val res = client.post<String> {
                url(HttpRoutes.POSTS)
                contentType(ContentType.Application.Json)
                body = ""
            }

            var respn = JSONObject(res)
            var obj = respn.getJSONObject("Tarjeta")
            var stringsValues = obj.getJSONArray("stringsValues")
            var valoresNombre = ""
            for (i in 0 until stringsValues.length()) {
                val nombre = stringsValues.getString(i)
                valoresNombre += nombre+","
            }
            println("valoresNombre: ${valoresNombre}")
            resp = PostResponse(obj.getString("nTarjeta"), obj.getString("nVal"), obj.getString("fExpira"), obj.getString("fVal"), obj.getString("nCvc"),obj.getString("pCvc"),stringsValues, valoresNombre)

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