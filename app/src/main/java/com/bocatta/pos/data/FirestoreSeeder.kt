package com.bocatta.pos.data

import android.util.Log
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections

object FirestoreSeeder {
    fun seedV2Collections() {
        val db = FirebaseFirestoreProvider.db
        
        // ============ INSUMOS BÁSICOS ============
        val insumos = listOf(
            mapOf("id" to "masa_crepa", "nombre" to "Masa para Crepas", "unit" to "pza", "rendimiento" to 60),
            mapOf("id" to "carlota_base", "nombre" to "Carlota de Limón (tanda)", "unit" to "pza", "rendimiento" to 5),
            mapOf("id" to "tiramisu_base", "nombre" to "Tiramisú (tanda)", "unit" to "pza", "rendimiento" to 5),
            mapOf("id" to "fresa_crema_base", "nombre" to "Fresas con Crema (tanda)", "unit" to "pza", "rendimiento" to 5),
            mapOf("id" to "boneless_bolsa", "nombre" to "Boneless (bolsa 2.6kg)", "unit" to "kg"),
            mapOf("id" to "nuggets_bolsa", "nombre" to "Nuggets (bolsa)", "unit" to "kg"),
            mapOf("id" to "papas_bolsa", "nombre" to "Papas Fritas (bolsa 2.3kg)", "unit" to "kg"),
            mapOf("id" to "nutella", "nombre" to "Nutella", "unit" to "g"),
            mapOf("id" to "lechera", "nombre" to "Lechera", "unit" to "g"),
            mapOf("id" to "philadelphia", "nombre" to "Queso Philadelphia", "unit" to "g"),
            mapOf("id" to "mermelada_fresa", "nombre" to "Mermelada de Fresa", "unit" to "g"),
            mapOf("id" to "jamon", "nombre" to "Jamón de Pavo", "unit" to "g"),
            mapOf("id" to "peperoni", "nombre" to "Peperoni", "unit" to "g"),
            mapOf("id" to "piña", "nombre" to "Piña en Almíbar", "unit" to "g"),
            mapOf("id" to "chocolate_cocoa", "nombre" to "Cocoa en polvo", "unit" to "g"),
            mapOf("id" to "galleta_oreo", "nombre" to "Galleta Oreo", "unit" to "pza"),
            mapOf("id" to "galleta_maria", "nombre" to "Galleta María", "unit" to "pza"),
            mapOf("id" to "galleta_mexicana", "nombre" to "Galleta Mexicana", "unit" to "pza"),
            mapOf("id" to "crema_batir", "nombre" to "Crema para Batir", "unit" to "ml"),
            mapOf("id" to "queso_crema", "nombre" to "Queso Crema", "unit" to "g"),
            mapOf("id" to "charola", "nombre" to "Charola", "unit" to "pza"),
            mapOf("id" to "tenedor", "nombre" to "Tenedor", "unit" to "pza"),
            mapOf("id" to "servilleta", "nombre" to "Servilleta", "unit" to "pza"),
            mapOf("id" to "vaso", "nombre" to "Vaso", "unit" to "pza"),
            mapOf("id" to "domo", "nombre" to "Domo", "unit" to "pza"),
            mapOf("id" to "cuchara", "nombre" to "Cuchara", "unit" to "pza"),
            mapOf("id" to "papel_hamburguesa", "nombre" to "Papel Hamburguesero", "unit" to "pza")
        )

        insumos.forEach { insumo ->
            db.collection(FirestoreCollections.INSUMOS).document(insumo["id"] as String)
                .set(insumo)
                .addOnSuccessListener { Log.d("Seeder", "Insumo ${insumo["id"]} seeded") }
        }

        // ============ PRODUCTOS TERMINADOS ============
        // Helper para crear configuración de crepa
        fun baseCrepaOptions(dulce: Boolean) = listOf(
            mapOf(
                "key" to "base",
                "title" to "BASE UNTABLE",
                "type" to "SINGLE_CHIP",
                "options" to if (dulce) listOf("Nutella", "Lechera", "Zarzamora", "Mermelada Fresa", "Philadelphia")
                            else listOf("Tomate", "Philadelphia"),
                "required" to true,
                "defaultValue" to if (dulce) "Nutella" else "Philadelphia"
            ),
            mapOf(
                "key" to "toppings",
                "title" to "TOPPINGS",
                "type" to if (dulce) "MULTI_CHECKBOX" else "SINGLE_CHIP",
                "options" to if (dulce) listOf(
                    "Durazno", "Fresa Natural", "Coco Rayado",
                    "Granillo Chocolate", "Granillo Colores",
                    "Oreo (Premium)", "Bombón (Premium)", "Nuez (Premium)"
                ) else listOf("Jamón", "Piña", "Peperoni"),
                "multiMax" to if (dulce) 6 else null
            ),
            mapOf(
                "key" to "aderezos",
                "title" to "ADEREZOS (opcional)",
                "type" to "MULTI_CHIP",
                "options" to listOf("Valentina", "Catsup", "Mayonesa", "Mostaza", "Chipotle", "Ranch", "BBQ", "Buffalo", "Blue Cheese", "Queso Amarillo")
            )
        )

        val productos = listOf(
            // === CREPAS DULCES ===
            mapOf(
                "nombre" to "Crepa Dulce Individual",
                "emoji" to "🥞",
                "categoria" to "CREPAS DULCES",
                "precioVenta" to mapOf("atlixco" to 25.0, "metepec" to 25.0),
                "esCombo" to false,
                "configSchema" to baseCrepaOptions(dulce = true),
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "charola", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "tenedor", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "papel_hamburguesa", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            mapOf(
                "nombre" to "Crepa Dulce 2x",
                "emoji" to "🥞🥞",
                "categoria" to "CREPAS DULCES",
                "precioVenta" to mapOf("atlixco" to 50.0, "metepec" to 40.0),
                "esCombo" to true,
                "productosCombo" to listOf("Crepa Dulce Individual", "Crepa Dulce Individual"),
                "configSchema" to baseCrepaOptions(dulce = true),
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "charola", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "tenedor", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "papel_hamburguesa", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            // === CREPAS SALADAS ===
            mapOf(
                "nombre" to "Crepa Salada Individual",
                "emoji" to "🥪",
                "categoria" to "CREPAS SALADAS",
                "precioVenta" to mapOf("atlixco" to 35.0, "metepec" to 35.0),
                "esCombo" to false,
                "configSchema" to baseCrepaOptions(dulce = false),
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "charola", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "tenedor", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "papel_hamburguesa", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            mapOf(
                "nombre" to "Crepa Salada 2x",
                "emoji" to "🥪🥪",
                "categoria" to "CREPAS SALADAS",
                "precioVenta" to mapOf("atlixco" to 70.0, "metepec" to 60.0),
                "esCombo" to true,
                "productosCombo" to listOf("Crepa Salada Individual", "Crepa Salada Individual"),
                "configSchema" to baseCrepaOptions(dulce = false),
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "charola", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "tenedor", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "papel_hamburguesa", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            // === POSTRES ===
            mapOf(
                "nombre" to "Carlota de Limón",
                "emoji" to "🍋",
                "categoria" to "POSTRES",
                "precioVenta" to mapOf("atlixco" to 40.0, "metepec" to 40.0),
                "esCombo" to false,
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "vaso", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "domo", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "cuchara", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            mapOf(
                "nombre" to "Tiramisú",
                "emoji" to "🍰",
                "categoria" to "POSTRES",
                "precioVenta" to mapOf("atlixco" to 45.0, "metepec" to 45.0),
                "esCombo" to false,
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "vaso", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "domo", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "cuchara", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            mapOf(
                "nombre" to "Fresas con Crema",
                "emoji" to "🍓",
                "categoria" to "POSTRES",
                "precioVenta" to mapOf("atlixco" to 50.0, "metepec" to 50.0),
                "esCombo" to false,
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "vaso", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "domo", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "cuchara", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            mapOf(
                "nombre" to "Duraznos con Crema",
                "emoji" to "🍑",
                "categoria" to "POSTRES",
                "precioVenta" to mapOf("atlixco" to 50.0, "metepec" to 50.0),
                "esCombo" to false,
                "consumiblesAsociados" to listOf(
                    mapOf("consumibleId" to "vaso", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "domo", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "cuchara", "cantidad" to 1.0, "unidad" to "pz"),
                    mapOf("consumibleId" to "servilleta", "cantidad" to 1.0, "unidad" to "pz")
                )
            ),
            // === FRAPPES ===
            mapOf(
                "nombre" to "Frappé Cocoa",
                "emoji" to "🍫",
                "categoria" to "FRAPPES",
                "precioVenta" to mapOf("atlixco" to 30.0, "metepec" to 30.0),
                "esCombo" to false,
                "configSchema" to listOf(
                    mapOf(
                        "key" to "sabor",
                        "title" to "SABOR",
                        "type" to "SINGLE_CHIP",
                        "options" to listOf("Cocoa", "Fresa", "Fresa con Cocoa", "Oreo"),
                        "required" to true,
                        "defaultValue" to "Cocoa"
                    ),
                    mapOf(
                        "key" to "base",
                        "title" to "BASE LÁCTEA (opcional)",
                        "type" to "SINGLE_CHIP",
                        "options" to listOf("Sin base", "Nutella", "Lechera"),
                        "defaultValue" to "Sin base"
                    )
                )
            ),
            mapOf(
                "nombre" to "Frappé Fresa",
                "emoji" to "🍓",
                "categoria" to "FRAPPES",
                "precioVenta" to mapOf("atlixco" to 30.0, "metepec" to 30.0),
                "esCombo" to false,
                "configSchema" to listOf(
                    mapOf(
                        "key" to "sabor",
                        "title" to "SABOR",
                        "type" to "SINGLE_CHIP",
                        "options" to listOf("Fresa", "Fresa con Cocoa", "Oreo"),
                        "required" to true,
                        "defaultValue" to "Fresa"
                    ),
                    mapOf(
                        "key" to "base",
                        "title" to "BASE LÁCTEA (opcional)",
                        "type" to "SINGLE_CHIP",
                        "options" to listOf("Sin base", "Nutella", "Lechera"),
                        "defaultValue" to "Sin base"
                    )
                )
            ),
            // === BOTANAS ===
            mapOf(
                "nombre" to "Papas Fritas",
                "emoji" to "🍟",
                "categoria" to "BOTANAS",
                "precioVenta" to mapOf("atlixco" to 35.0, "metepec" to 35.0),
                "esCombo" to false
            ),
            mapOf(
                "nombre" to "Papas con Chorizo",
                "emoji" to "🌭",
                "categoria" to "BOTANAS",
                "precioVenta" to mapOf("atlixco" to 60.0, "metepec" to 60.0),
                "esCombo" to false
            ),
            mapOf(
                "nombre" to "Boneless",
                "emoji" to "🍗",
                "categoria" to "BOTANAS",
                "precioVenta" to mapOf("atlixco" to 90.0, "metepec" to 90.0),
                "esCombo" to false,
                "nota" to "Incluye orden de papas"
            ),
            mapOf(
                "nombre" to "Nuggets",
                "emoji" to "🐔",
                "categoria" to "BOTANAS",
                "precioVenta" to mapOf("atlixco" to 80.0, "metepec" to 70.0),
                "esCombo" to false,
                "nota" to "Incluye orden de papas"
            )
        )

        productos.forEach { prod ->
            val docId = prod["nombre"].toString().lowercase().replace(" ", "_")
            db.collection(FirestoreCollections.PRODUCTOS).document(docId)
                .set(prod)
                .addOnSuccessListener { Log.d("Seeder", "Producto ${prod["nombre"]} seeded") }
        }

        // ============ INVENTARIO INICIAL ============
        val sucursales = listOf("atlixco", "metepec")
        sucursales.forEach { suc ->
            insumos.forEach { insumo ->
                val cantidad = when (insumo["id"]) {
                    "masa_crepa" -> 30.0
                    "charola", "tenedor", "servilleta", "vaso", "domo", "cuchara", "papel_hamburguesa" -> 500.0
                    "galleta_oreo", "galleta_maria", "galleta_mexicana" -> 200.0
                    "boneless_bolsa", "nuggets_bolsa", "papas_bolsa" -> 5.0
                    else -> 100.0
                }
                val invDoc = mapOf(
                    "insumoId" to insumo["id"],
                    "cantidadDisponible" to cantidad,
                    "unidad" to insumo["unit"]
                )
                db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                    .document("${suc}_${insumo["id"]}")
                    .set(invDoc)
                    .addOnSuccessListener { Log.d("Seeder", "Inventario $suc/${insumo["id"]} seeded") }
            }
        }

        Log.d("Seeder", "✅ Seed completado")
    }
}
