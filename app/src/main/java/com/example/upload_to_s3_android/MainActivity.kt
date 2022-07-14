package com.example.upload_to_s3_android

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import coil.compose.rememberImagePainter
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.predictions.models.Label
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.example.upload_to_s3_android.data.remote.PostsService
import com.example.upload_to_s3_android.data.remote.dto.PostRequest
import com.example.upload_to_s3_android.data.remote.dto.PostResponse
import com.example.upload_to_s3_android.ui.theme.Rojo
import java.io.File

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.upload_to_s3_android.data.remote.PostsUserService
import com.example.upload_to_s3_android.data.remote.dto.PostUserResponse
import androidx.compose.runtime.produceState
import com.example.upload_to_s3_android.data.remote.api.SimpleApi
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class ImageState {
    object Initial: ImageState()
    class ImageSelected(val imageUri: Uri): ImageState()
    object ImageUploaded: ImageState()
    class ImageDownloaded(val downloadedImageFile: File): ImageState()
}

sealed class LoginState {
    object Logout: LoginState()
    object Login: LoginState()
    object Logerror: LoginState()
}

sealed class LogoutState {
    object LogError: LoginState()
    object Login: LoginState()
}

class MainActivity : ComponentActivity() {

    private val service = PostsService.create()
    private val serviceUser = PostsUserService.create()

    // codigo en s3
    companion object {
        const val PHOTO_KEY = "my-photo.jpg"
    }

    private var imageState = mutableStateOf<ImageState>(ImageState.Initial)
    private var loginState = mutableStateOf<LoginState>(LoginState.Logout)
    private var logoutState = mutableStateOf<LoginState>(LogoutState.Login)
    private var logError = false

    private val getImageLauncher = registerForActivityResult(GetContent()) { uri ->
        uri?.let { imageState.value = ImageState.ImageSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAmplify()

        setContent {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)

            ) {
                Image(painter = painterResource(id = R.drawable.logo_upc_red), contentDescription = null)
                Text(text = "")
                when (val state = loginState.value) {
                    is LoginState.Logout -> {
                        // call the function which contains all the input fields
                        var text by remember { mutableStateOf(TextFieldValue("")) }
                        TextField(
                            value = text,
                            onValueChange = { newValue -> text = newValue },
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            label = { Text("Usuario") },
                            placeholder = { Text("Usuario") },
                        )
                        Text(text = "")
                        var pass by remember { mutableStateOf(TextFieldValue("")) }

                        OutlinedTextField(
                            value = pass,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            label = { Text(text = "Password") },
                            placeholder = { Text(text = "Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            onValueChange = {
                                pass = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        when (val state = logoutState.value) {
                            is LogoutState.LogError -> {
                                Text(text = "Verifica usuario y password", color = Rojo)
                            }

                        }

                        Button(onClick = {login(text.text,pass.text)}, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                            Text(text = "Login")
                        }
                    }

                    is LoginState.Login -> {
                        when (val state = imageState.value) {
                            // Show Open Gallery Button
                            is ImageState.Initial -> {
                                Button(onClick = { getImageLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                    Text(text = "Seleccionar Tarjeta")
                                }
                            }

                            // Show Upload Button
                            is ImageState.ImageSelected -> {

                                Button(onClick = { uploadPhoto(state.imageUri) }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                    Text(text = "Validar tarjeta"
                                    )
                                }
                                Text(text = "")

                                Image(
                                    painter = rememberImagePainter(
                                        data  = state.imageUri  // or ht
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth()
                                )


                            }

                            // Show Download Button
                            is ImageState.ImageUploaded -> {

                                val postR = PostResponse("","","1","1")
                                val posts = produceState(
                                    initialValue = postR,
                                    producer = {
                                        value = service.createPost()!!
                                    }
                                )

                                Log.i("posts", posts.value.toString())

                                Text(text = "Nro. Tarjeta: "+posts.value.nTarjeta)
                                Text(text = "% Nro. Tarjeta: "+posts.value.nVal)
                                Text(text = "Fecha Venc.: "+posts.value.fExpira)
                                Text(text = "% Fecha Venc.: "+posts.value.fVal)

                                /*Button(onClick = ::downloadPhoto) {
                                    Text(text = "Descargar tarjeta")
                                }*/
                                Text(text = "")
                                Button(onClick = { getImageLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                    Text(text = "Seleccionar Nueva Tarjeta")
                                }
                            }

                            // Show downloaded image
                            is ImageState.ImageDownloaded -> {
                                Image(
                                    painter = rememberImagePainter(state.downloadedImageFile),
                                    contentDescription = null,
                                    // modifier = Modifier.fillMaxSize()
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(text = "")
                                Button(onClick = { getImageLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                    Text(text = "Seleccionar Imagen")
                                }
                            }
                        }

                        Text(text = "")
                        Text(text = "")
                        Text(text = "")
                        Button(onClick = { logout() }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                            Text(text = "Logout")
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }


            }
        }
    }

    private fun configureAmplify() {
        try {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin(AWSS3StoragePlugin())
            Amplify.configure(applicationContext)

            Log.i("kilo", "Initialized Amplify")
        } catch (error: AmplifyException) {
            Log.e("kilo", "Could not initialize Amplify", error)
        }
    }

    private fun uploadPhoto(imageUri: Uri) {
        val stream = contentResolver.openInputStream(imageUri)!!

        Amplify.Storage.uploadInputStream(
            PHOTO_KEY,
            stream,
            { imageState.value = ImageState.ImageUploaded },
            { error -> Log.e("kilo", "Failed upload", error) }
        )
    }

    private fun login(usuario: String,pass: String ) {

        Log.e("usuario", usuario)
        Log.e("pass", pass)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://f7ytj1daw8.execute-api.us-east-1.amazonaws.com/dev/usuario/")
            .build()

        val service = retrofit.create(SimpleApi::class.java)

        // Create JSON using JSONObject
        val jsonObject = JSONObject()
        val persona = JSONObject("""{"email":""""+usuario+"""", "password":""""+pass+""""}""")
        jsonObject.put("persona", persona)

        // Convert JSONObject to String
        val jsonObjectString = jsonObject.toString()

        // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            val response = service.pushPost(requestBody)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {

                    // Convert raw JSON to pretty JSON using GSON library
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val prettyJson = gson.toJson(
                        JsonParser.parseString(
                            response.body()?.string() // About this thread blocking annotation : https://github.com/square/retrofit/issues/3255
                        )
                    )

                    Log.d("Pretty Printed JSON :", prettyJson)

                    val jsonObject = JSONObject(prettyJson)
                    Log.d(">>>>", jsonObject.getInt("input").toString())

                    if(jsonObject.getInt("input") ==1){
                        Log.d("success :", "1")
                        loginState.value = LoginState.Login
                        logoutState.value = LogoutState.Login
                    }else{
                        Log.d("unsuccess :", "0")
                        logoutState.value = LogoutState.LogError
                    }

                } else {

                    Log.e("RETROFIT_ERROR", response.code().toString())

                }
            }
        }
    }

    private fun logout() {
        loginState.value = LoginState.Logout
    }



    private fun downloadPhoto() {
        val localFile = File("${applicationContext.filesDir}/downloaded-image.jpg")

        Amplify.Storage.downloadFile(
            PHOTO_KEY,
            localFile,
            { imageState.value = ImageState.ImageDownloaded(localFile) },
            { Log.e("kilo", "Failed download", it) }
        )
    }

}
