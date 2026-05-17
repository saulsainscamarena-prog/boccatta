package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestoreProvider.db

    /** Devuelve el UID si el login fue exitoso, lanza excepción si falló */
    suspend fun loginConUid(email: String, pass: String): Result<String> {
        // Modo demo para testing
        if (email.lowercase().contains("demo") && pass == "demo123") {
            return Result.success("demo_uid_12345")
        }
        
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No se obtuvo el UID"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(Exception(traducirErrorFirebase(e)))
        }
    }

    /** Devuelve el UID si el registro fue exitoso, lanza excepción si falló */
    suspend fun registrarUsuarioConUid(email: String, pass: String, nombre: String, rol: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No se obtuvo el UID de Firebase"))
            
            val nuevoUsuario = Usuario(
                uid = uid,
                nombre = nombre,
                correo = email,
                rol = if (rol == "ADMIN") Rol.ADMIN else Rol.VENDEDOR
            )
            
            db.collection(FirestoreCollections.USUARIOS).document(uid).set(nuevoUsuario).await()
            Result.success(uid)
        } catch (e: Exception) {
            val errorTraducido = traducirErrorFirebase(e)
            Result.failure(Exception(errorTraducido))
        }
    }

    /**
     * Valida el código de registro y determina el rol automáticamente.
     * - Código admin (Firebase: configuracion/seguridad ? codigo_maestro) ? Rol.ADMIN
     * - Cualquier otro código ? null (inválido)
     */
    suspend fun validarCodigoYObtenerRol(codigo: String): Rol? {
        val codigoLimpio = codigo.trim()

        // Verificar códigos dinámicos desde Firebase (Fuente de verdad en producción)
        return try {
            val doc = db.collection(FirestoreCollections.CONFIGURACION).document("seguridad").get().await()
            val codigoAdmin = doc.getString("codigo_maestro")?.trim()
            val codigoStaff = doc.getString("codigo_maestro_empleado")?.trim()
            
            when {
                codigoAdmin != null && codigoLimpio.equals(codigoAdmin, ignoreCase = true) -> Rol.ADMIN
                codigoStaff != null && codigoLimpio.equals(codigoStaff, ignoreCase = true) -> Rol.VENDEDOR
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Traduce los códigos de error de Firebase a español amigable */
    private fun traducirErrorFirebase(e: Exception): String {
        val msg = e.localizedMessage ?: ""
        return when {
            msg.contains("ERROR_WRONG_PASSWORD") || msg.contains("wrong-password") -> "Contraseña incorrecta"
            msg.contains("ERROR_USER_NOT_FOUND") || msg.contains("user-not-found") -> "Usuario no registrado"
            msg.contains("ERROR_INVALID_EMAIL") || msg.contains("invalid-email") -> "Correo electrónico inválido"
            msg.contains("ERROR_EMAIL_ALREADY_IN_USE") || msg.contains("email-already-in-use") -> "Este correo ya está en uso"
            msg.contains("ERROR_WEAK_PASSWORD") || msg.contains("weak-password") -> "La contraseña es muy débil"
            msg.contains("network-request-failed") -> "Sin conexión a internet"
            else -> "Error de acceso: Credenciales incorrectas o problema de red"
        }
    }

    /** @deprecated Usar validarCodigoYObtenerRol() */
    suspend fun validarCodigoMaestro(codigo: String): Boolean {
        return validarCodigoYObtenerRol(codigo) != null
    }
}




