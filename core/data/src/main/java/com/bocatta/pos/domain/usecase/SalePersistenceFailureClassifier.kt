package com.bocatta.pos.domain.usecase

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import kotlinx.serialization.SerializationException

enum class SalePersistenceFailureKind {
    TRANSIENT,
    PERMANENT,
    BUSINESS
}

object SalePersistenceFailureClassifier {

    fun classify(error: Throwable): SalePersistenceFailureKind {
        if (error is SerializationException || error is IllegalArgumentException) {
            return SalePersistenceFailureKind.PERMANENT
        }

        if (error is IllegalStateException && error.isStockRejection()) {
            return SalePersistenceFailureKind.BUSINESS
        }

        if (error is IOException) {
            return SalePersistenceFailureKind.TRANSIENT
        }

        val transientCodes = setOf(
            "UNAVAILABLE",
            "DEADLINE_EXCEEDED",
            "ABORTED",
            "CANCELLED",
            "RESOURCE_EXHAUSTED",
            "INTERNAL",
            "UNKNOWN",
            "ALREADY_EXISTS"
        )
        return if (error.firestoreCodeName() in transientCodes) {
            SalePersistenceFailureKind.TRANSIENT
        } else {
            SalePersistenceFailureKind.PERMANENT
        }
    }

    fun operatorMessage(
        error: Throwable,
        kind: SalePersistenceFailureKind
    ): String {
        if (kind == SalePersistenceFailureKind.BUSINESS) {
            return error.message?.takeIf { it.isNotBlank() }
                ?: "La venta no cumple las condiciones de inventario."
        }
        if (kind == SalePersistenceFailureKind.TRANSIENT) {
            return "No hay comunicacion estable con el servidor. La venta se guardara localmente."
        }

        return if (error.firestoreCodeName() == "PERMISSION_DENIED") {
            "La cuenta no tiene permiso para registrar ventas. Solicita revision de un administrador."
        } else if (error.firestoreCodeName() == "UNAUTHENTICATED") {
            "La sesion ya no es valida. Inicia sesion nuevamente antes de cobrar."
        } else if (error is SerializationException || error is IllegalArgumentException) {
            "Los datos de la venta no son validos. Revisa la orden antes de intentar de nuevo."
        } else {
            "No se pudo registrar la venta. Conserva la orden y solicita revision."
        }
    }

    private fun Throwable.isStockRejection(): Boolean {
        val normalized = message.orEmpty().lowercase()
        return normalized.contains("stock insuficiente") ||
            normalized.contains("sin stock") ||
            normalized.contains("inventario insuficiente")
    }

    private fun Throwable.firestoreCodeName(): String? {
        findCause<FirebaseFirestoreException>()?.let { firestoreError ->
            runCatching { firestoreError.code.name }.getOrNull()?.let { return it }
        }

        val knownCodes = listOf(
            "PERMISSION_DENIED",
            "UNAUTHENTICATED",
            "UNAVAILABLE",
            "DEADLINE_EXCEEDED",
            "ABORTED",
            "CANCELLED",
            "RESOURCE_EXHAUSTED",
            "INTERNAL",
            "UNKNOWN",
            "ALREADY_EXISTS",
            "INVALID_ARGUMENT",
            "FAILED_PRECONDITION",
            "NOT_FOUND",
            "OUT_OF_RANGE",
            "UNIMPLEMENTED",
            "DATA_LOSS"
        )
        var current: Throwable? = this
        while (current != null) {
            val normalized = current.message.orEmpty().uppercase()
            knownCodes.firstOrNull(normalized::contains)?.let { return it }
            current = current.cause
        }
        return null
    }

    private inline fun <reified T : Throwable> Throwable.findCause(): T? {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }
}
