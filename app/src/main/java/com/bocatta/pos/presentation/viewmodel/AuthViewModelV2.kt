package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ViewModel de AutenticaciÑn V2.
 * Maneja el acceso al sistema y la sincronizaciÑn inicial de sesiÑn.
 */
class AuthViewModelV2(private val repoAuth: AuthRepository = AuthRepository()) : BaseViewModel() {
    private val auth = FirebaseAuth.getInstance()

    var estaLogueado by mutableStateOf(auth.currentUser != null)
        private set

    fun intentarLogin(email: String, pass: String, onSuccess: (uid: String) -> Unit) {
        viewModelScope.launch(safeHandler) {
            mensajeError = null
            cargando = true
            
            val result = repoAuth.loginConUid(email, pass)
            result.onSuccess { uid ->
                estaLogueado = true
                onSuccess(uid)
            }.onFailure { e ->
                mensajeError = e.localizedMessage ?: "Credenciales incorrectas"
            }
            cargando = false
        }
    }

    fun intentarRegistro(email: String, pass: String, nombre: String, codigo: String, rolIgnorado: String, onSuccess: (uid: String) -> Unit) {
        viewModelScope.launch(safeHandler) {
            mensajeError = null
            cargando = true
            
            if (email.isBlank() || pass.isBlank() || nombre.isBlank() || codigo.isBlank()) {
                mensajeError = "Todos los campos son obligatorios"
                cargando = false
                return@launch
            }

            val rolDeterminado = repoAuth.validarCodigoYObtenerRol(codigo)
            if (rolDeterminado == null) {
                mensajeError = "CÑdigo de seguridad invÑlido"
                cargando = false
                return@launch
            }
            
            val result = repoAuth.registrarUsuarioConUid(email, pass, nombre, rolDeterminado.name)
            result.onSuccess { uid ->
                estaLogueado = true
                onSuccess(uid)
            }.onFailure { e ->
                mensajeError = e.localizedMessage
            }
            cargando = false
        }
    }

    fun logout() {
        auth.signOut()
        estaLogueado = false
    }

    fun limpiarError() {
        mensajeError = null
    }
}

