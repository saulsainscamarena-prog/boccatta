package com.bocatta.pos.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import android.util.Log

abstract class BaseAndroidViewModel(application: Application) : AndroidViewModel(application) {
    protected val appContext: android.content.Context = application.applicationContext

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
