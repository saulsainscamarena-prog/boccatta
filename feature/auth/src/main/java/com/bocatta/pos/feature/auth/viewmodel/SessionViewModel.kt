package com.bocatta.pos.feature.auth.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.bocatta.pos.core.ui.viewmodel.BaseAndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.usecase.AuthorizationManager

class SessionViewModel(
    application: Application,
    private val authManager: AuthorizationManager
) : BaseAndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestoreProvider.db
    private val prefs = application.getSharedPreferences("bocatta_session", android.content.Context.MODE_PRIVATE)

    private val connectivityManager = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    var estaOnline by mutableStateOf(true)
        private set

    private var listenerDevoluciones: ListenerRegistration? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    var usuario by mutableStateOf<Usuario?>(null)
        private set
    var cargandoSesion by mutableStateOf(false)
        private set
    var errorSesion by mutableStateOf<String?>(null)
        private set

    var esAtlixco by mutableStateOf(prefs.getBoolean("es_atlixco_manual", com.bocatta.pos.core.ui.utils.BocattaUtilsV2.sucursalSugeridaPorDia() == "Atlixco"))
    var sucursalManual by mutableStateOf(prefs.getBoolean("sucursal_manual", false))
    var modoOscuroManual by mutableStateOf<Boolean?>(if (prefs.contains("modo_oscuro")) prefs.getBoolean("modo_oscuro", false) else null)

    // Nueva propiedad para soportar múltiples sucursales
    private var _sucursalActual by mutableStateOf(prefs.getString("sucursal_actual", if (esAtlixco) "Atlixco" else "Metepec") ?: "Atlixco")
    var sucursalActual: String
        get() = _sucursalActual
        set(value) {
            _sucursalActual = value
            prefs.edit().putString("sucursal_actual", value).apply()
            esAtlixco = value == "Atlixco"
        }

    fun toggleModoOscuro(oscuro: Boolean?) {
        modoOscuroManual = oscuro
        if (oscuro == null) prefs.edit().remove("modo_oscuro").apply()
        else prefs.edit().putBoolean("modo_oscuro", oscuro).apply()
    }

    fun cambiarSucursalManual(suc: String) {
        sucursalActual = suc
        sucursalManual = true
        prefs.edit().putBoolean("sucursal_manual", true).apply()
    }

    val rol: Rol get() = usuario?.rol ?: Rol.VENDEDOR
    val esAdmin: Boolean get() = rol == Rol.ADMIN || rol == Rol.DUEÑO
    val esDueno: Boolean get() = rol == Rol.DUEÑO
    val uid: String get() = auth.currentUser?.uid ?: ""
    val nombreUsuario: String get() = usuario?.nombre ?: "Usuario"

    var devolucionesPendientes by mutableIntStateOf(0)
        private set

    init {
        auth.currentUser?.uid?.let {
            cargarUsuarioCache(it)
            cargarUsuario(it)
        }
        iniciarMonitoreoConexion()
    }

    private fun iniciarMonitoreoConexion() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        estaOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                     capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                estaOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                             capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            override fun onLost(network: Network) { estaOnline = false }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(request, callback)
    }

    fun cargarUsuario(uid: String) {
        viewModelScope.launch {
            cargandoSesion = true
            try {
                val doc = db.collection(FirestoreCollections.USUARIOS).document(uid).get().await()
                if (doc.exists()) {
                    val usuarioRemoto = doc.toObject(Usuario::class.java)?.copy(uid = uid)
                    if (usuarioRemoto != null) {
                        usuario = usuarioRemoto
                        errorSesion = null
                        guardarUsuarioCache(usuarioRemoto)
                        escucharDevolucionesPendientes()
                    } else if (usuario == null) {
                        errorSesion = "No se pudo interpretar el perfil de usuario."
                    }
                } else if (usuario == null) {
                    errorSesion = "Usuario autenticado sin perfil operativo en Firestore."
                }
            } catch (e: Exception) {
                if (usuario == null) {
                    errorSesion = "No se pudo cargar la sesión operativa: ${e.message}"
                }
            } finally {
                cargandoSesion = false
            }
        }
    }

    private fun cargarUsuarioCache(uid: String) {
        val cachedUid = prefs.getString("usuario_uid", null) ?: return
        if (cachedUid != uid) return
        val rolCache = prefs.getString("usuario_rol", null) ?: return
        val rol = runCatching { Rol.valueOf(rolCache) }.getOrNull() ?: return
        usuario = Usuario(
            uid = cachedUid,
            nombre = prefs.getString("usuario_nombre", "") ?: "",
            correo = prefs.getString("usuario_correo", "") ?: "",
            rol = rol
        )
    }

    private fun guardarUsuarioCache(usuario: Usuario) {
        prefs.edit()
            .putString("usuario_uid", usuario.uid)
            .putString("usuario_nombre", usuario.nombre)
            .putString("usuario_correo", usuario.correo)
            .putString("usuario_rol", usuario.rol.name)
            .apply()
    }

    private fun escucharDevolucionesPendientes() {
        listenerDevoluciones?.remove()
        listenerDevoluciones = db.collection(FirestoreCollections.DEVOLUCIONES)
            .whereEqualTo("estado", "pendiente")
            .addSnapshotListener { snap, _ ->
                devolucionesPendientes = snap?.size() ?: 0
            }
    }

    fun validarPinAdmin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val esValido = authManager.validarPinAdmin(pin)
            onResult(esValido)
        }
    }

    fun validarPinDesbloqueo(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(authManager.validarPinDesbloqueo(pin, usuario, sucursalActual))
        }
    }

    fun cerrarSesion() {
        auth.signOut()
        listenerDevoluciones?.remove()
        listenerDevoluciones = null
        usuario = null
        errorSesion = null
        devolucionesPendientes = 0
        sucursalManual = false
        prefs.edit().clear().apply()
    }

    override fun onCleared() {
        super.onCleared()
        listenerDevoluciones?.remove()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }
}



