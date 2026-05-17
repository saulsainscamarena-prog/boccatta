package com.bocatta.pos.domain.model

enum class ModoAsignacion { GENERAL, POR_CATEGORIA, PERSONALIZADO }

data class GrupoConfiguracionGlobal(
    val id: String = "",
    val key: String = "",
    val title: String = "",
    val type: ConfigFieldType = ConfigFieldType.SINGLE_CHIP,
    val modoAsignacion: ModoAsignacion = ModoAsignacion.GENERAL,
    val categorias: List<String> = emptyList(),
    val productos: List<String> = emptyList(),
    val excluirProductos: List<String> = emptyList(),
    val opciones: List<String> = emptyList(),
    val source: String = "MANUAL",
    val catalogo: String? = null,
    val required: Boolean = false,
    val multiMax: Int? = null,
    val min: Int = 0,
    val max: Int = 0,
    val defaultValue: String? = null,
    val activo: Boolean = true,
    val creadoEn: Long = 0L,
    val actualizadoEn: Long = 0L
) {
    fun aplicaA(productoId: String, categoriaProducto: String): Boolean {
        if (excluirProductos.contains(productoId)) return false
        return when (modoAsignacion) {
            ModoAsignacion.GENERAL -> true
            ModoAsignacion.POR_CATEGORIA -> categorias.contains(categoriaProducto)
            ModoAsignacion.PERSONALIZADO -> productos.contains(productoId)
        }
    }
}

data class OverrideGrupos(
    val ocultarGrupos: List<String> = emptyList(),
    val forzarGrupos: List<String> = emptyList(),
    val modificarGrupos: Map<String, ConfigOptionGroup> = emptyMap()
)

data class CambioGrupoConfig(
    val id: String = "",
    val grupoId: String = "",
    val accion: String = "",
    val cambios: String = "",
    val usuario: String = "",
    val fecha: Long = 0L,
    val revertido: Boolean = false
)
