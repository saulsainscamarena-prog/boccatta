package com.bocatta.pos.feature.auth.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import com.bocatta.pos.data.repository.AuthRepository
import com.bocatta.pos.domain.usecase.AuthorizationManager
import com.google.firebase.auth.FirebaseAuth
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel de AutenticaciÑn V2.
 * Maneja el acceso al sistema y la sincronizaciÑn inicial de sesiÑn.
 */
class AuthViewModelV2(private val repoAuth: AuthRepository = AuthRepository()) : BaseViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestoreProvider.db

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

    fun setupInitialNip(pin: String, onNipSet: () -> Unit) {
        viewModelScope.launch(safeHandler) {
            mensajeError = null
            cargando = true

            val uid = auth.currentUser?.uid ?: run {
                mensajeError = "No hay sesión activa"
                cargando = false
                return@launch
            }
            val email = auth.currentUser?.email ?: ""
            val pinHash = AuthorizationManager.hashPinForStorage(pin)

            // Checkear si ya existe un PIN para este usuario
            val existing = db.collection(FirestoreCollections.PIN_AUTHORIZATIONS)
                .whereEqualTo("userId", uid)
                .get()
                .await()

            if (!existing.isEmpty) {
                mensajeError = "Ya tienes un NIP configurado. Usa Cambiar NIP."
                cargando = false
                return@launch
            }

            db.collection(FirestoreCollections.PIN_AUTHORIZATIONS)
                .document(pinHash)
                .set(
                    mapOf(
                        "pinHash" to pinHash,
                        "userId" to uid,
                        "displayName" to email.substringBefore("@"),
                        "role" to "ADMIN",
                        "active" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            cargando = false
            onNipSet()
        }
    }

    fun changeNip(oldPin: String, newPin: String, onSuccess: () -> Unit) {
        viewModelScope.launch(safeHandler) {
            mensajeError = null
            cargando = true

            val oldHash = AuthorizationManager.hashPinForStorage(oldPin)
            val newHash = AuthorizationManager.hashPinForStorage(newPin)

            if (oldHash == newHash) {
                mensajeError = "El nuevo NIP no puede ser igual al actual"
                cargando = false
                return@launch
            }

            val docRef = db.collection(FirestoreCollections.PIN_AUTHORIZATIONS).document(oldHash)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                mensajeError = "NIP actual incorrecto"
                cargando = false
                return@launch
            }

            val data = doc.data ?: run {
                mensajeError = "Error al leer datos del NIP"
                cargando = false
                return@launch
            }

            // Crear nuevo doc con el mismo userId y datos, luego eliminar el viejo
            val batch = db.batch()
            batch.set(
                db.collection(FirestoreCollections.PIN_AUTHORIZATIONS).document(newHash),
                data + mapOf("pinHash" to newHash, "updatedAt" to System.currentTimeMillis())
            )
            batch.delete(docRef)
            batch.commit().await()

            cargando = false
            onSuccess()
        }
    }
}




