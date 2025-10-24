package com.example.app_camera.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PerfilViewModel : ViewModel() {

    //Variable para guardar el URI de la imagen
    private val _imagenUri = MutableStateFlow<Uri?>(null)
    val imagenUri = _imagenUri.asStateFlow()

    //Funcion para actualizar la imagen (desde galeria o camara)
    fun onImagenSeleccionada(uri: Uri?) {
        _imagenUri.update { uri }
    }
}