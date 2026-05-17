package com.bocatta.pos.domain.util

/**
 * Helper para deserializar enums desde Firestore de forma segura.
 * Todos los enums del proyecto almacenan su valor como string via [firestoreKey].
 * Esto evita corrupción de datos si se reordenan los valores del enum.
 */
inline fun <reified T : Enum<T>> fromFirestoreKey(key: String?, default: T): T {
    if (key == null) return default
    return enumValues<T>().find { it.name.equals(key, ignoreCase = true) || 
        (it is FirestoreKeyProvider && (it as FirestoreKeyProvider).firestoreKey.equals(key, ignoreCase = true)) } ?: default
}

interface FirestoreKeyProvider {
    val firestoreKey: String
}

