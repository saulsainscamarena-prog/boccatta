package com.bocatta.pos.domain.model

object OpcionesProducto {
    // --- CATEGORÍAS ---
    val categorias = listOf("CREPAS_DULCES", "CREPAS_SALADAS", "POSTRES", "BEBIDAS", "SNACKS", "CONSUMIBLES")

    // --- BASES DE CREPAS DULCES ---
    val basesDulces = listOf("Nutella", "Philadelphia", "Lechera", "Zarzamora", "Mermelada de Fresa")
    
    // --- TOPPINGS DULCES ---
    val toppingsDulces = listOf("Fresa Natural", "Durazno", "Coco Rayado", "Granillo Chocolate", "Granillo Colores")
    val toppingsPremium = listOf("Oreo", "Nuez", "Bombón")
    val espolvoreados = listOf("Azúcar Glass", "Cocoa Espolvoreada")

    // --- BASES DE CREPAS SALADAS ---
    val basesSaladas = listOf("Tomate", "Philadelphia") // Tomate base para pizzas, Philly para jamón
    val toppingsSalados = listOf("Peperoni", "Jamón", "Chorizo", "Piña")
    val tiposCrepaSalada = listOf("Hawaiana", "Peperoni", "Jamón y Queso")
    
    // --- SNACKS Y ADEREZOS ---
    val aderezosSnacks = listOf("Valentina", "BBQ", "Buffalo", "Blue Cheese", "Queso Amarillo", "Cátsup", "Mayonesa")
    
    // --- CONSUMIBLES (Despacho) ---
    // Nota: El usuario indicó que es "Charola de Unicel", no de cartón.
    val consumiblesDisponibles = listOf(
        "Vaso 16oz", "Domo", "Charola Unicel", "Tenedor", "Cuchara", "Servilleta", "Papel Hamburguesero"
    )

    // --- POSTRES (Recetas complejas) ---
    val postresList = listOf("Carlota de Limón", "Tiramisú", "Fresas con Crema", "Duraznos con Crema")

    // --- FRAPPES ---
    val frappesList = listOf("Oreo", "Cocoa", "Fresa", "Fresa y Cocoa")
    
    val motivosCancelacion = listOf("Cliente cambió opinión", "Error de captura", "Prueba de sistema", "Mantenimiento", "Otro")
}


