package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import android.util.Log

abstract class BaseViewModel : ViewModel() {
    protected val safeHandler = CoroutineExceptionHandler { _, exception ->
        mensajeError = "Error: ${exception.localizedMessage}"
        Log.e(javaClass.simpleName, "Error CrÑtico", exception)
    }

    var cargando by mutableStateOf(false)
    var mensajeExito by mutableStateOf<String?>(null)
    var mensajeError by mutableStateOf<String?>(null)

    open fun limpiarMensajes() {
        mensajeExito = null
        mensajeError = null
    }
}

