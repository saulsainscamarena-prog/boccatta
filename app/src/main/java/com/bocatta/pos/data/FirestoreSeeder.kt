package com.bocatta.pos.data

import android.util.Log
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections

object FirestoreSeeder {
    fun seedV2Collections() {
        val db = FirebaseFirestoreProvider.db
        
        // 1. Insumos Base (Para inventario)
        val insumos = listOf(
            mapOf("id" to "masa_crepa", "nombre" to "Masa para Crepas", "unit" to "kg"),
            mapOf("id" to "fresa", "nombre" to "Fresas Naturales", "unit" to "g"),
            mapOf("id" to "nutella", "nombre" to "Nutella", "unit" to "g"),
            mapOf("id" to "philadelphia", "nombre" to "Queso Philadelphia", "unit" to "g"),
            mapOf("id" to "jamon", "nombre" to "Jamón de Pavo", "unit" to "g"),
            mapOf("id" to "queso_manchego", "nombre" to "Queso Manchego", "unit" to "g")
        )

        insumos.forEach { insumo ->
            db.collection(FirestoreCollections.INSUMOS).document(insumo["id"] as String).set(insumo)
        }

        // 2. Productos Terminados (Para Ventas)
        val productos = listOf(
            mapOf(
                "nombre" to "Crepa Salada Tradicional",
                "emoji" to "🥪",
                "categoria" to "CREPAS SALADAS",
                "precioVenta" to mapOf("atlixco" to 65.0, "metepec" to 75.0),
                "esCombo" to false,
                "toppingsIncluidos" to 2,
                "costoToppingExtra" to 15.0
            ),
            mapOf(
                "nombre" to "Crepa Dulce Premium",
                "emoji" to "🍓",
                "categoria" to "CREPAS DULCES",
                "precioVenta" to mapOf("atlixco" to 55.0, "metepec" to 65.0),
                "esCombo" to false,
                "toppingsIncluidos" to 2,
                "costoToppingExtra" to 10.0
            ),
            mapOf(
                "nombre" to "Combo Pareja (2 Saladas)",
                "emoji" to "🎁",
                "categoria" to "COMBOS",
                "precioVenta" to mapOf("atlixco" to 120.0, "metepec" to 140.0),
                "esCombo" to true,
                "productosCombo" to listOf("Crepa Salada", "Crepa Salada")
            ),
            mapOf(
                "nombre" to "Refresco 600ml",
                "emoji" to "🥤",
                "categoria" to "BEBIDAS",
                "precioVenta" to mapOf("atlixco" to 25.0, "metepec" to 28.0),
                "esCombo" to false
            ),
            mapOf(
                "nombre" to "Malteada Vainilla",
                "emoji" to "🍦",
                "categoria" to "BEBIDAS",
                "precioVenta" to mapOf("atlixco" to 45.0, "metepec" to 55.0),
                "esCombo" to false
            )
        )

        productos.forEach { prod ->
            val docId = prod["nombre"].toString().lowercase().replace(" ", "_")
            db.collection(FirestoreCollections.PRODUCTOS).document(docId).set(prod)
                .addOnSuccessListener { Log.d("Seeder", "Producto ${prod["nombre"]} seeded") }
        }

        // 3. Inventario Inicial (Para evitar 'Stock insuficiente')
        val sucursales = listOf("atlixco", "metepec")
        sucursales.forEach { suc ->
            insumos.forEach { insumo ->
                val invDoc = mapOf(
                    "insumoId" to insumo["id"],
                    "cantidadDisponible" to 100.0,
                    "unidad" to insumo["unit"]
                )
                db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${suc}_${insumo["id"]}").set(invDoc)
            }
        }
    }
}
