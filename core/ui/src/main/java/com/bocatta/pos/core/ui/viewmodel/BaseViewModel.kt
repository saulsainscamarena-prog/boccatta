package com.bocatta.pos.core.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.util.Log

abstract class BaseViewModel : ViewModel() {
    protected val safeHandler = CoroutineExceptionHandler { _, exception ->
        mensajeError = "Error: ${exception.localizedMessage}"
        Log.e(javaClass.simpleName, "Error Cr\u00edtico", exception)
    }

    var cargando by mutableStateOf(false)
    var mensajeExito by mutableStateOf<String?>(null)
    var mensajeError by mutableStateOf<String?>(null)

    /**
     * Lanza una corutina en [Dispatchers.IO] dentro del [viewModelScope].
     * Útil para mover trabajo bloqueante (DB, red) fuera del hilo principal
     * sin tener que repetir `viewModelScope.launch(Dispatchers.IO)`.
     */
    protected fun launchIO(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO) { block() }
    }

    open fun limpiarMensajes() {
        mensajeExito = null
        mensajeError = null
    }
}


