package com.bocatta.pos.domain.util

inline fun <reified T : Enum<T>> fromFirestoreKey(key: String?, default: T): T {
    if (key == null) return default
    return enumValues<T>().firstOrNull { enumVal ->
        if (enumVal.name.equals(key, ignoreCase = true)) return@firstOrNull true
        try {
            val getter = enumVal.javaClass.getMethod("getFirestoreKey")
            (getter.invoke(enumVal) as? String)?.equals(key, ignoreCase = true) ?: false
        } catch (_: NoSuchMethodException) { false }
    } ?: default
}
