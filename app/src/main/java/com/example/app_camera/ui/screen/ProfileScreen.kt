package com.example.app_camera.ui.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_camera.components.ImagenInteligente
import com.example.app_camera.viewmodel.PerfilViewModel
import java.io.File
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

//Funcion para crear nuestra URI temporal
fun crearImagenUri(context: Context): Uri {
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}

//Funcion para copiar nuestra URI temporal a la Galería den nuestro telefono
fun guardarImagenEnGaleria(context: Context, uriTemporal: Uri): Uri? {
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "perfil_app_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // Indicar la carpeta de fotos (Pictures)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TuAppNombre")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    //Obtener la URI pública donde se guardará
    val galeriaUri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    if (galeriaUri != null) {
        try {
            //Copiar los datos de la URI temporal a la URI pública
            resolver.openOutputStream(galeriaUri).use { outputStream ->
                resolver.openInputStream(uriTemporal).use { inputStream ->
                    inputStream?.copyTo(outputStream!!)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(galeriaUri, contentValues, null, null)
            }
            return galeriaUri

        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla, borrar la entrada vacía
            resolver.delete(galeriaUri, null, null)
            return null
        }
    }
    return null
}

@Composable
fun PerfilScreen(vm: PerfilViewModel = viewModel()) {

    val context = LocalContext.current

    val imagenUri by vm.imagenUri.collectAsState()

    // --- Launchers para Actividades (Galería y Cámara) ---

    val launcherGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        vm.onImagenSeleccionada(uri)
    }

    //Necesitamos un Uri temporal donde la cámara guardara la foto
    var uriCamaraTemporal by remember { mutableStateOf<Uri?>(null) }

    val launcherCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito: Boolean ->
        if (exito && uriCamaraTemporal != null) {
            //Enviar el resultado al ViewModel
            vm.onImagenSeleccionada(uriCamaraTemporal)

            guardarImagenEnGaleria(context, uriCamaraTemporal!!)
        }
    }

    //Launcher para pedir permiso de Camara
    val launcherPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido: Boolean ->
        if (concedido) {
            // Si el permiso es concedido, creamos el Uri y lanzamos la cámara
            val nuevaUri = crearImagenUri(context)
            uriCamaraTemporal = nuevaUri
            launcherCamara.launch(nuevaUri)
        } else {
        }
    }

    //--- Interfaz de Usuario ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ImagenInteligente(uri = imagenUri)

        Spacer(modifier = Modifier.height(32.dp))

        //Boton para Galería
        Button(onClick = {
            launcherGaleria.launch("image/*")
        }) {
            Text("Abrir Galería")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para Camara
        Button(onClick = {
            launcherPermisoCamara.launch(Manifest.permission.CAMERA)
        }) {
            Text("Tomar Foto")
        }
    }
}