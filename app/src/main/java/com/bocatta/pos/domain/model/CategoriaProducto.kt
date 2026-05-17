package com.bocatta.pos.domain.model

data class CategoriaProducto(
    val id: String = "",
    val nombre: String = "",
    val subcategorias: List<String> = emptyList()
)

