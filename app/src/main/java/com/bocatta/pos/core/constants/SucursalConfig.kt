package com.bocatta.pos.core.constants

/**
 * Fuente de verdad para la configuración de todas las sucursales de Bocatta.
 *
 * Para agregar una nueva sucursal, solo se agrega una entrada al mapa SUCURSALES.
 * No es necesario modificar ningún otro archivo de lógica de negocio.
 */
object SucursalConfig {

    private const val SEPARATOR = "_"

    /** Mapa de ID de sucursal (lowercase) -> información de la sucursal */
    val SUCURSALES: Map<String, SucursalInfo> = mapOf(
        "atlixco" to SucursalInfo(id = "atlixco", prefijo = "ATL", displayName = "Atlixco"),
        "metepec" to SucursalInfo(id = "metepec", prefijo = "MT", displayName = "Metepec")
    )

    /**
     * Retorna el prefijo de ticket para una sucursal dada.
     * Ej: "atlixco" -> "ATL", "metepec" -> "MT", cualquier otra -> "BC"
     */
    fun prefijoPorSucursal(sucursalId: String): String =
        SUCURSALES[sucursalId.lowercase()]?.prefijo ?: "BC"

    /**
     * Extrae el ID del insumo desde un ID de documento de inventario por sucursal.
     * Formato del documento: "{sucursalId}_{insumoId}"
     * Ej: "atlixco_crema_batida" -> "crema_batida"
     */
    fun extraerInsumoIdDeDocId(docId: String): String {
        val sucursalMatch = SUCURSALES.keys.firstOrNull { docId.startsWith("${it}$SEPARATOR") }
        return if (sucursalMatch != null) {
            docId.removePrefix("${sucursalMatch}$SEPARATOR")
        } else {
            docId
        }
    }

    /**
     * Construye el ID de documento de inventario por sucursal.
     * Ej: ("atlixco", "crema_batida") -> "atlixco_crema_batida"
     */
    fun buildDocId(sucursalId: String, insumoId: String): String =
        "${sucursalId.lowercase()}${SEPARATOR}${insumoId}"
}

data class SucursalInfo(
    val id: String,
    val prefijo: String,
    val displayName: String
)
