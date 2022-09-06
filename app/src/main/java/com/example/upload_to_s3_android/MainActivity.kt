package com.example.upload_to_s3_android

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.GetContent
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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.upload_to_s3_android.data.remote.PostsUserService
import com.example.upload_to_s3_android.data.remote.dto.PostUserResponse
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.upload_to_s3_android.data.remote.api.SimpleApi
import com.example.upload_to_s3_android.model.Reporte
import com.example.upload_to_s3_android.model.Status
import com.example.upload_to_s3_android.ui.theme.Negro
import com.example.upload_to_s3_android.ui.theme.Purple700
import com.example.upload_to_s3_android.ui.theme.Verde
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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

sealed class ImageStateCvc {
    object Initial: ImageStateCvc()
    class ImageSelected(val imageUri: Uri): ImageStateCvc()
    object ImageUploaded: ImageStateCvc()
}

sealed class LoginState {
    object Logout: LoginState()
    object Login: LoginState()
    object Logerror: LoginState()
}

sealed class ValidateState {
    object Success: ValidateState()
    object None: ValidateState()
    object Error: ValidateState()
}

sealed class LogoutState {
    object LogError: LoginState()
    object Login: LoginState()
}

sealed class ReporteState {
    object VerReporte: ReporteState()
    object CerrarReporte: ReporteState()
}

class MainActivity : ComponentActivity() {

    private val service = PostsService.create()
    private val serviceUser = PostsUserService.create()
    private var mensajeVal = ""
    private var nombreUsuario = ""
    private var listaReporte: MutableList<Reporte> = mutableListOf()


    companion object {
        const val PHOTO_KEY = "my-photo.jpg"
        const val PHOTO_KEY_CVC = "cvc.jpg"
    }

    private var imageState = mutableStateOf<ImageState>(ImageState.Initial)
    private var imageStateCvc = mutableStateOf<ImageStateCvc>(ImageStateCvc.Initial)
    private var loginState = mutableStateOf<LoginState>(LoginState.Logout)
    private var logoutState = mutableStateOf<LoginState>(LogoutState.Login)
    private var validateState = mutableStateOf<ValidateState>(ValidateState.None)
    private var reporteState = mutableStateOf<ReporteState>(ReporteState.CerrarReporte)
    private var logError = false

    private val getImageLauncher = registerForActivityResult(GetContent()) { uri ->
        uri?.let { imageState.value = ImageState.ImageSelected(it)
            imageStateCvc.value = ImageStateCvc.Initial}
    }

    private val getImageLauncherCvc = registerForActivityResult(GetContent()) { uri ->
        uri?.let { imageStateCvc.value = ImageStateCvc.ImageSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAmplify()

        setContent {
            val scrollState = rememberScrollState()
            LaunchedEffect(Unit) { scrollState.animateScrollTo(10000) }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    //.verticalScroll(scrollState)
                    .background(Color.White)

            ) {

                if(!"".equals(nombreUsuario)){
                    Text(text = ("!Hola "+ nombreUsuario + "!"))
                }
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

                        // Ver Reporte
                        when (val state = reporteState.value) {

                            is ReporteState.CerrarReporte -> {
                                when (val state = imageState.value) {
                                    // Show Open Gallery Button
                                    is ImageState.Initial -> {
                                        Button(onClick = { getImageLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                            Text(text = "Seleccionar Tarjeta")
                                        }
                                    }

                                    // Show Upload Button
                                    is ImageState.ImageSelected -> {
                                        Text(text = "")

                                        Image(
                                            painter = rememberImagePainter(
                                                data  = state.imageUri  // or ht
                                            ),
                                            /*contentDescription = null,
                                            modifier = Modifier.fillMaxWidth()*/
                                            contentDescription = "Localized description",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .clip(shape = RoundedCornerShape(16.dp))
                                                .size(250.dp, 150.dp)
                                        )

                                        when (val stateCvc = imageStateCvc.value) {

                                            is ImageStateCvc.Initial -> {
                                                Button(onClick = { getImageLauncherCvc.launch("image/*") }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                                    Text(text = "Seleccionar CVC")
                                                }
                                            }

                                            is ImageStateCvc.ImageSelected -> {
                                                Text(text = "")

                                                Image(
                                                    painter = rememberImagePainter(
                                                        data  = stateCvc.imageUri  // or ht
                                                    ),
                                                    /*contentDescription = null,
                                                    modifier = Modifier.fillMaxWidth()*/
                                                    contentDescription = "Localized description",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .clip(shape = RoundedCornerShape(16.dp))
                                                        .size(250.dp, 150.dp)
                                                )

                                                Button(onClick = { uploadPhoto(state.imageUri, stateCvc.imageUri) }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                                    Text(text = "Validar tarjeta"
                                                    )
                                                }
                                            }

                                        }

                                    }

                                    // Show Download Button
                                    is ImageState.ImageUploaded -> {

                                        val postR = PostResponse("","","1","1","","", JSONArray(), "")
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
                                        Text(text = "CVC: "+posts.value.nCvc)
                                        Text(text = "% CVC: "+posts.value.pCvc)

                                        Text(text = "")

                                        when (val state = validateState.value) {
                                            is ValidateState.Success -> {
                                                Text(text = mensajeVal, color = Color.Green)
                                            }
                                            is ValidateState.Error -> {
                                                Text(text = mensajeVal, color = Rojo)
                                            }
                                        }

                                        if("".equals(posts.value.nTarjeta)){
                                            Text(text = "")
                                        }else{
                                            Button(onClick = {validaTarjeta(posts.value.nTarjeta,posts.value.fExpira, posts.value.nCvc, posts.value.nVal, posts.value.fVal, posts.value.pCvc, posts.value.stringsValues)}, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                                Text(text = "Validar Datos Tarjeta")
                                            }
                                        }

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
                                Button(onClick = { verReporte() }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                    Text(text = "Ver Reporte")
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
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

                            is ReporteState.VerReporte -> {

                                Demo_Table()

                                Button(onClick = { cerrarReporte() }, colors = ButtonDefaults.buttonColors(backgroundColor = Rojo, contentColor = Color.White)) {
                                    Text(text = "Atras")
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
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



                        // Ver Reporte
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

    private fun uploadPhoto(imageUri: Uri,imageUriCvc: Uri) {
        val stream = contentResolver.openInputStream(imageUri)!!
        mensajeVal = ""
        validateState.value = ValidateState.None

        Amplify.Storage.uploadInputStream(
            PHOTO_KEY,
            stream,
            //{ imageState.value = ImageState.ImageUploaded },
            { uploadPhotoCvc(imageUriCvc) },
            { error -> Log.e("kilo", "Failed upload", error) }
        )
    }

    private fun uploadPhotoCvc(imageUri: Uri) {
        val stream = contentResolver.openInputStream(imageUri)!!
        mensajeVal = ""
        validateState.value = ValidateState.None

        Amplify.Storage.uploadInputStream(
            PHOTO_KEY_CVC,
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
                        nombreUsuario = jsonObject.getString("nombre")
                        loginState.value = LoginState.Login
                        logoutState.value = LogoutState.Login
                        reporteState.value = ReporteState.CerrarReporte
                    }else{
                        Log.d("unsuccess :", "0")
                        logoutState.value = LogoutState.LogError
                        reporteState.value = ReporteState.CerrarReporte
                    }

                } else {

                    Log.e("RETROFIT_ERROR", response.code().toString())

                }
            }
        }
    }

    private fun validaTarjeta(pan: String,fechaVen: String, cvc: String, panPorc: String, fecPorc: String, cvcPorc: String, nombres: JSONArray) {

        Log.e("pan", pan)
        Log.e("fechaVen", fechaVen)
        mensajeVal = ""
        validateState.value = ValidateState.None

        var panPorcSp = panPorc.split(".")[0]
        var fecPorcSp = fecPorc.split(".")[0]
        var cvcPorcSp = cvcPorc.split(".")[0]


        val retrofit = Retrofit.Builder()
            .baseUrl("https://f7ytj1daw8.execute-api.us-east-1.amazonaws.com/dev/tarjeta/")
            .build()

        val service = retrofit.create(SimpleApi::class.java)

        val pan2 = pan.replace(' ','-');
        val panEnmas = "XXXXXXXXXXXX-"+pan2.split('-')[3]

        // Create JSON using JSONObject
        val jsonObject = JSONObject()
        val tarjeta =
            JSONObject("""{"pan":""""+pan2+"""", "fechaVenc":""""+fechaVen+"""", "cvc":""""+cvc+"""", "panPorc":""""+panPorcSp+"""", "fecPorc":""""+fecPorcSp+"""", "cvcPorc":""""+cvcPorcSp+""""}""")
        jsonObject.put("tarjeta", tarjeta)
        jsonObject.put("nombres", nombres)

        // Convert JSONObject to String
        val jsonObjectString = jsonObject.toString()

        // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            val response = service.pushValidar(requestBody)

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
                    Log.d(">>>>", jsonObject.getString("code").toString())

                    if(jsonObject.getString("code").equals("success")){
                        Log.d("success :", jsonObject.getString("message"))
                        mensajeVal = jsonObject.getString("message")
                        validateState.value = ValidateState.Success

                        val reporte = Reporte((listaReporte.size+1),panEnmas, Status("success",jsonObject.getString("message")))
                        listaReporte.add(reporte)

                    }else{
                        Log.d("error :", jsonObject.getString("message"))
                        mensajeVal = jsonObject.getString("message")
                        validateState.value = ValidateState.Error

                        val reporte = Reporte((listaReporte.size+1),panEnmas, Status("error",jsonObject.getString("message")))
                        listaReporte.add(reporte)
                    }

                } else {

                    Log.e("RETROFIT_ERROR", response.code().toString())


                }
            }
        }
    }

    private fun logout() {

        mensajeVal = ""
        nombreUsuario = ""
        validateState.value = ValidateState.None
        loginState.value = LoginState.Logout
        listaReporte = mutableListOf()
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

    private fun verReporte(){
        for( rep in listaReporte){
            Log.e("rep.id",rep.id.toString())
            Log.e("rep.pan",rep.pan)
            Log.e("rep.status.code",rep.status.code)
            Log.e("rep.status.message",rep.status.message)
        }
        reporteState.value = ReporteState.VerReporte
    }

    private fun cerrarReporte(){
        reporteState.value = ReporteState.CerrarReporte
    }

    @Composable
    fun <T> Table(
        columnCount: Int,
        cellWidth: (index: Int) -> Dp,
        data: List<T>,
        modifier: Modifier = Modifier,
        headerCellContent: @Composable (index: Int) -> Unit,
        cellContent: @Composable (index: Int, item: T) -> Unit,
    ) {
        Surface(
            modifier = modifier
        ) {
            LazyRow(
                modifier = Modifier.padding(12.dp)
            ) {
                items((0 until columnCount).toList()) { columnIndex ->
                    Column {
                        (0..data.size).forEach { index ->
                            Surface(
                                border = BorderStroke(1.dp, Color.LightGray),
                                contentColor = Color.Transparent,
                                modifier = Modifier.width(cellWidth(columnIndex))
                            ) {
                                if (index == 0) {
                                    headerCellContent(columnIndex)
                                } else {
                                    cellContent(columnIndex, data[index - 1])
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Demo_Table() {

        val cellWidth: (Int) -> Dp = { index ->
            when (index) {
                2 -> 100.dp
                3 -> 150.dp
                else -> 100.dp
            }
        }
        val headerCellTitle: @Composable (Int) -> Unit = { index ->
            val value = when (index) {
                0 -> "Id"
                1 -> "Pan"
                2 -> "Código Resultado"
                3 -> "Resultado"
                else -> ""
            }

            Text(
                text = value,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(2.dp).background(Rojo),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Black,
                textDecoration = TextDecoration.Underline,
                color = Color.White

            )
        }
        val cellText: @Composable (Int, Reporte) -> Unit = { index, item ->
            val value = when (index) {
                0 -> item.id
                1 -> item.pan
                2 -> item.status.code
                3 -> item.status.message
                else -> ""
            }

            var colorText = Purple700
            if("error".equals(value.toString())){
                colorText = Rojo
            }else if("success".equals(value.toString())){
                colorText = Verde
            }else{
                colorText = Negro
            }

            Text(
                text = value.toString(),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colorText
            )
        }

        Table(
            columnCount = 4,
            cellWidth = cellWidth,
            data = listaReporte,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            headerCellContent = headerCellTitle,
            cellContent = cellText
        )
    }

}