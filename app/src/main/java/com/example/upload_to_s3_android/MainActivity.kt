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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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

sealed class ImageState {
    object Initial: ImageState()
    class ImageSelected(val imageUri: Uri): ImageState()
    object ImageUploaded: ImageState()
    class ImageDownloaded(val downloadedImageFile: File): ImageState()
}

class MainActivity : ComponentActivity() {

    private val service = PostsService.create()

    companion object {
        const val PHOTO_KEY = "my-photo.jpg"
    }

    private var imageState = mutableStateOf<ImageState>(ImageState.Initial)

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
                modifier = Modifier.fillMaxSize().background(Color.White)

            ) {
                Image(painter = painterResource(id = R.drawable.logo_upc_red), contentDescription = null)
                Text(text = "")
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
                        val posts = produceState<PostResponse>(
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