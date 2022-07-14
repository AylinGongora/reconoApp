package com.example.upload_to_s3_android.data.remote

object HttpRoutes {

    private const val BASE_URL = "https://yark7ytbw7dk6jpbcpyhfmcpuy0vtrjy.lambda-url.us-east-1.on.aws/"
    private const val BASE_USER_URL = "https://f7ytj1daw8.execute-api.us-east-1.amazonaws.com/dev/usuario/consultar"
    const val POSTS = "$BASE_URL"
    const val POSTS_USER = "$BASE_USER_URL"
}