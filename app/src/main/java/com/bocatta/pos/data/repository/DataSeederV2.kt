package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * DataSeederV2 — Sistema de Blueprint de Negocio
 *
 * NO es solo un inicializador de datos de ejemplo.
 * Es el "ADN" del negocio Bocatta: catálogo de insumos, productos,
 * recetas y configuración base que permite abrir una nueva sucursal
 * en segundos con un solo método: [inicializarSucursal].
 *
 * Flujo correcto:
 * 1. Primera vez (base de datos vacía) → [inicializarTodoV2] crea todo el catálogo
 * 2. Nueva sucursal → [inicializarSucursal] crea el inventario local a cero
 * 3. Datos ya existentes → no hace nada, protege producción
 *
 * Para agregar/editar productos en producción usar [ProductoRepository].
 */
class DataSeederV2(
    private val productoRepo: ProductoRepository = ProductoRepository()
) {
    private val db = FirebaseFirestoreProvider.db

    // ── ENTRY POINT SEGURO ────────────────────────────────────────────────────

    /**
     * Punto de entrada principal. Solo inicializa si la base de datos está vacía.
     * Seguro para llamar en cada arranque de la app — no destruye datos reales.
     */
    suspend fun inicializarSiNecesario(): Result<Boolean> {
        return try {
            val yaExiste = productoRepo.existenProductos()
            if (yaExiste) {
                Timber.tag("SEEDER").i("Catálogo ya existe, omitiendo inicialización")
                return Result.success(false) // false = no fue necesario inicializar
            }
            Timber.tag("SEEDER").i("Base vacía detectada — inicializando blueprint Bocatta...")
            inicializarTodoV2().map { true }
        } catch (e: Exception) {
            Timber.tag("SEEDER").e(e, "Error en inicializarSiNecesario")
            Result.failure(e)
        }
    }

    /**
     * Inicializa una nueva sucursal: crea los documentos de inventario local
     * (branch_portions) con stock = 0 para todos los insumos conocidos.
     * No toca productos ni recetas globales.
     * Tiempo estimado: < 3 segundos.
     */
    suspend fun inicializarSucursal(sucursalId: String): Result<Unit> {
        return try {
            Timber.tag("SEEDER").i("Inicializando sucursal: $sucursalId")
            val insumos = db.collection(FirestoreCollections.INSUMOS).get().await()
            if (insumos.isEmpty) {
                return Result.failure(Exception("No hay insumos maestros. Ejecuta inicializarTodoV2 primero."))
            }
            // Batches de máximo 500 operaciones por límite de Firestore
            val chunks = insumos.documents.chunked(400)
            chunks.forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    val insumoId = doc.id
                    val ref = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                        .document("${sucursalId}_$insumoId")
                    // SetOptions.merge() — no sobreescribe si ya tiene stock real
                    batch.set(ref, mapOf(
                        "id" to "${sucursalId}_$insumoId",
                        "insumoId" to insumoId,
                        "sucursal" to sucursalId,
                        "cantidadEnBase" to 0.0,
                        "cantidadDisponible" to 0.0,
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ), SetOptions.merge())
                }
                batch.commit().await()
            }
            // Crear contador de tickets para la sucursal
            db.collection(FirestoreCollections.CONFIGURACION)
                .document("contadores_$sucursalId")
                .set(mapOf(
                    "ultimo_ticket" to 0L,
                    "sucursal" to sucursalId,
                    "creado" to System.currentTimeMillis()
                ), SetOptions.merge()).await()

            Timber.tag("SEEDER").i("Sucursal $sucursalId inicializada con ${insumos.size()} insumos")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SEEDER").e(e, "Error inicializando sucursal $sucursalId")
            Result.failure(e)
        }
    }

    // ── INICIALIZACIÓN COMPLETA ───────────────────────────────────────────────

    /**
     * Inicializa el catálogo completo: insumos, productos, recetas de producción
     * y recetas de venta. Usar solo en primera configuración o en modo desarrollo.
     * En producción, preferir [inicializarSiNecesario].
     */
    suspend fun inicializarTodoV2(): Result<Unit> {
        return try {
            Timber.tag("SEEDER").i("Iniciando inicialización completa V2...")
            inicializarInsumos()
            inicializarProductosYRecetasVenta()
            inicializarRecetasProduccion()
            Timber.tag("SEEDER").i("Inicialización completa V2 finalizada ✓")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SEEDER").e(e, "Error en inicializarTodoV2")
            Result.failure(e)
        }
    }

    // ── INSUMOS MAESTROS ──────────────────────────────────────────────────────

    private suspend fun inicializarInsumos() {
        val insumos = buildInsumos()
        val chunks = insumos.chunked(400)
        chunks.forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { insumo ->
                batch.set(
                    db.collection(FirestoreCollections.INSUMOS).document(insumo.id),
                    insumo
                )
                batch.set(
                    db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumo.id),
                    mapOf(
                        "id" to insumo.id,
                        "insumoId" to insumo.id,
                        "nombre" to insumo.nombre,
                        "cantidadEnBase" to insumo.cantidadEnBase,
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ), SetOptions.merge()
                )
            }
            batch.commit().await()
        }
        Timber.tag("SEEDER").i("${insumos.size} insumos inicializados")
    }

    private suspend fun inicializarProductosYRecetasVenta() {
        val pares = buildProductosConRecetas()
        pares.forEach { (producto, receta) ->
            productoRepo.guardarProductoConReceta(producto, receta)
        }
        Timber.tag("SEEDER").i("${pares.size} productos con recetas inicializados")
    }

    private suspend fun inicializarRecetasProduccion() {
        val recetas = buildRecetasProduccion()
        val batch = db.batch()
        recetas.forEach { receta ->
            batch.set(
                db.collection(FirestoreCollections.RECETAS_PRODUCCION).document(receta.id),
                receta
            )
        }
        batch.commit().await()
        Timber.tag("SEEDER").i("${recetas.size} recetas de producción inicializadas")
    }

    // ── RESET DE INVENTARIO ───────────────────────────────────────────────────

    /**
     * Resetea solo el inventario de una sucursal a cero.
     * No toca productos ni recetas. Útil para cierres de período.
     */
    suspend fun resetearInventarioSucursal(sucursal: String): Result<Unit> {
        return try {
            val sucursalId = sucursal.lowercase()
            val insumosIds = db.collection(FirestoreCollections.INSUMOS).get().await()
                .documents.map { it.id }
            val chunks = insumosIds.chunked(400)
            chunks.forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.set(
                        db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                            .document("${sucursalId}_$id"),
                        mapOf(
                            "id" to "${sucursalId}_$id",
                            "insumoId" to id,
                            "sucursal" to sucursalId,
                            "cantidadEnBase" to 0.0,
                            "ultimaActualizacion" to System.currentTimeMillis()
                        ), SetOptions.merge()
                    )
                }
                batch.commit().await()
            }
            Timber.tag("SEEDER").i("Inventario de $sucursalId reseteado a cero")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Carga un stock base de emergencia para que la sucursal pueda operar.
     */
    suspend fun cargarStockEmergencia(sucursal: String): Result<Unit> {
        return try {
            val sucursalId = sucursal.lowercase()
            val criticalItems = listOf("masa_crepa", "carlota_unidad", "tiramisu_unidad", "fresas_crema_unidad", "duraznos_crema_unidad")
            val batch = db.batch()
            
            criticalItems.forEach { id ->
                val ref = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                    .document("${sucursalId}_$id")
                batch.set(ref, mapOf(
                    "cantidadEnBase" to 10.0,
                    "ultimaActualizacion" to System.currentTimeMillis()
                ), SetOptions.merge())
            }
            batch.commit().await()
            Timber.tag("SEEDER").i("Carga de emergencia (10 unidades) completada para $sucursalId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── BUILDERS DE DATOS ─────────────────────────────────────────────────────

    private fun buildInsumos(): List<InsumoV2> = listOf(
        // Materia Prima
        InsumoV2("harina_kg", "Harina de Trigo", "Materia Prima", "g", 0.020, 5000.0, 500.0,
            listOf(PresentacionInsumo("Saco 5kg", "kg", 5000.0, 1.0, 100.0))),
        InsumoV2("leche_lt", "Leche Entera", "Materia Prima", "ml", 0.024, 3000.0, 500.0),
        InsumoV2("mantequilla_kg", "Mantequilla", "Materia Prima", "g", 0.160, 1000.0, 100.0),
        InsumoV2("huevo_pz", "Huevo", "Materia Prima", "pz", 3.0, 100.0, 12.0),
        InsumoV2("polvora_hornear", "Polvo de Hornear", "Materia Prima", "g", 0.150, 200.0, 20.0),
        InsumoV2("bicarbonato", "Bicarbonato", "Materia Prima", "g", 0.050, 200.0, 20.0),
        InsumoV2("azucar", "Azúcar", "Materia Prima", "g", 0.020, 5000.0, 500.0),
        InsumoV2("azucar_glas", "Azúcar Glass", "Materia Prima", "g", 0.040, 1000.0, 100.0),
        InsumoV2("hielo", "Hielo", "Bases", "g", 0.010, 5000.0, 500.0),
        // Bases
        InsumoV2("nutella_kg", "Nutella", "Bases", "g", 0.190, 1000.0, 100.0),
        InsumoV2("queso_crema_kg", "Queso Crema", "Bases", "g", 0.150, 1000.0, 100.0),
        InsumoV2("lechera_kg", "Lechera", "Bases", "g", 0.095, 1000.0, 100.0),
        InsumoV2("media_crema", "Media Crema", "Bases", "g", 0.060, 1000.0, 100.0),
        InsumoV2("leche_evaporada", "Leche Evaporada", "Bases", "ml", 0.080, 1000.0, 100.0),
        InsumoV2("crema_batir", "Crema para Batir", "Bases", "ml", 0.070, 1000.0, 100.0),
        InsumoV2("jugo_limon", "Jugo de Limón", "Bases", "ml", 0.040, 500.0, 50.0),
        InsumoV2("cafe", "Café", "Bases", "ml", 0.060, 500.0, 50.0),
        InsumoV2("cocoa", "Cocoa en Polvo", "Bases", "g", 0.080, 500.0, 50.0),
        InsumoV2("salsa_tomate_lt", "Salsa de Tomate", "Salado", "ml", 0.050, 1000.0, 100.0),
        InsumoV2("zarzamora_kg", "Zarzamora", "Bases", "g", 0.030, 500.0, 50.0),
        InsumoV2("mermelada_fresa", "Mermelada de Fresa", "Bases", "g", 0.060, 500.0, 50.0),
        // Aderezos
        InsumoV2("bbq", "Salsa BBQ", "Aderezo", "g", 0.050, 500.0, 50.0),
        InsumoV2("buffalo", "Salsa Buffalo", "Aderezo", "g", 0.050, 500.0, 50.0),
        InsumoV2("blue_cheese", "Blue Cheese", "Aderezo", "g", 0.080, 500.0, 50.0),
        InsumoV2("valentina", "Valentina", "Aderezo", "g", 0.030, 500.0, 50.0),
        InsumoV2("queso_amarillo", "Queso Amarillo", "Aderezo", "g", 0.070, 500.0, 50.0),
        InsumoV2("catsup", "Catsup", "Aderezo", "g", 0.030, 500.0, 50.0),
        InsumoV2("mayonesa", "Mayonesa", "Aderezo", "g", 0.040, 500.0, 50.0),
        // Salados
        InsumoV2("queso_mozzarella_kg", "Queso Mozzarella", "Salado", "g", 0.180, 1000.0, 100.0),
        InsumoV2("jamon_kg", "Jamón de Pierna", "Salado", "g", 0.120, 1000.0, 100.0),
        InsumoV2("peperoni_kg", "Peperoni", "Salado", "g", 0.240, 500.0, 50.0),
        InsumoV2("chorizo_kg", "Chorizo", "Salado", "g", 0.115, 500.0, 50.0),
        InsumoV2("pina_kg", "Piña", "Salado", "g", 0.040, 500.0, 50.0),
        // Toppings
        InsumoV2("fresas", "Fresas", "Topping", "g", 0.050, 500.0, 50.0),
        InsumoV2("durazno_kg", "Duraznos", "Topping", "g", 0.050, 500.0, 50.0),
        InsumoV2("coco_rayado", "Coco Rayado", "Topping", "g", 0.080, 300.0, 30.0),
        InsumoV2("granillo_chocolate", "Granillo Chocolate", "Topping", "g", 0.100, 300.0, 30.0),
        InsumoV2("granillo_colores", "Granillo Colores", "Topping", "g", 0.080, 300.0, 30.0),
        InsumoV2("bombon_kg", "Bombón", "Topping", "g", 0.150, 300.0, 30.0),
        InsumoV2("nuez_kg", "Nuez", "Topping", "g", 0.200, 300.0, 30.0),
        // Snacks
        InsumoV2("boneless", "Boneless", "Snacks", "g", 0.080, 5000.0, 500.0,
            listOf(PresentacionInsumo("Bolsa 2.6kg", "kg", 2600.0, 10.0, 208.0))),
        InsumoV2("nuggets", "Nuggets", "Snacks", "g", 0.070, 5000.0, 500.0,
            listOf(PresentacionInsumo("Bolsa 2.6kg", "kg", 2600.0, 10.0, 182.0))),
        InsumoV2("papas", "Papas", "Snacks", "g", 0.040, 5000.0, 500.0,
            listOf(PresentacionInsumo("Bolsa 2.3kg", "kg", 2300.0, 10.0, 92.0))),
        // Galletas
        InsumoV2("oreo", "Galleta Oreo", "Topping", "pz", 0.50, 294.0, 28.0,
            listOf(PresentacionInsumo("Caja (21 paquetes)", "box", 294.0, 5.0, 147.0),
                PresentacionInsumo("Paquete (14 pz)", "pkg", 14.0, 50.0, 7.0))),
        InsumoV2("galletas_maria", "Galletas María", "Bases", "pz", 0.035, 200.0, 40.0,
            listOf(PresentacionInsumo("Paquete (40 pz)", "pkg", 40.0, 20.0, 15.0))),
        InsumoV2("galletas_mexicana", "Galletas Mexicanas", "Bases", "pz", 0.025, 180.0, 60.0,
            listOf(PresentacionInsumo("Paquete 5u (60 pz)", "pkg", 60.0, 15.0, 45.0))),
        // Producción (insumos intermedios fabricados en sucursal)
        InsumoV2("masa_crepa", "Masa de Crepa Preparada", "Producción", "pz", 5.0, 60.0, 12.0),
        InsumoV2("carlota_unidad", "Carlota de Limón", "Producción", "pz", 15.0, 0.0, 4.0),
        InsumoV2("tiramisu_unidad", "Tiramisú", "Producción", "pz", 22.0, 0.0, 4.0),
        InsumoV2("fresas_crema_unidad", "Fresas con Crema", "Producción", "pz", 18.0, 0.0, 4.0),
        InsumoV2("duraznos_crema_unidad", "Duraznos con Crema", "Producción", "pz", 18.0, 0.0, 4.0),
        // Consumibles
        InsumoV2("charola", "Charola Unicel", "Consumible", "pz", 1.60, 200.0, 50.0,
            listOf(PresentacionInsumo("Bolsa (50 pz)", "pz", 1.0, 200.0, 80.0))),
        InsumoV2("tenedor", "Tenedor Plástico", "Consumible", "pz", 0.50, 200.0, 50.0,
            listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0))),
        InsumoV2("servilletas", "Servilletas", "Consumible", "pz", 0.25, 600.0, 100.0,
            listOf(PresentacionInsumo("Paquete (200 pz)", "pz", 1.0, 600.0, 45.0))),
        InsumoV2("vaso", "Vaso 16oz", "Consumible", "pz", 0.50, 200.0, 50.0,
            listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0))),
        InsumoV2("domo", "Domo", "Consumible", "pz", 0.50, 200.0, 50.0,
            listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0))),
        InsumoV2("papel_hamburguesero", "Papel Hamburguesero", "Consumible", "pz", 0.30, 300.0, 50.0,
            listOf(PresentacionInsumo("Paquete (100 pz)", "pz", 1.0, 300.0, 30.0))),
        InsumoV2("cuchara", "Cuchara", "Consumible", "pz", 0.50, 200.0, 50.0,
            listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)))
    )

    private fun ing(id: String, nombre: String, cantidad: Double, unidad: String = "g") =
        IngredienteReceta(id, nombre, cantidad, unidad)

    private fun consumible(id: String, cantidad: Double = 1.0) =
        ConsumibleRequerido(id, cantidad, "pz")

    private fun buildProductosConRecetas(): List<Pair<SalesInventoryProductV2, RecetaV2>> {
        val consumiblesCrepa = listOf(consumible("charola"), consumible("tenedor"), consumible("servilletas"), consumible("papel_hamburguesero"))
        val consumiblesPostre = listOf(consumible("vaso"), consumible("domo"), consumible("cuchara"), consumible("servilletas"))
        val consumiblesSnack = listOf(consumible("charola"), consumible("tenedor"), consumible("servilletas"), consumible("papel_hamburguesero"))

        return listOf(
            // ── CREPAS ──────────────────────────────────────────────────────
            SalesInventoryProductV2("cr_dulce", "Crepa Individual Dulce", "🥞", "CREPAS_DULCES",
                mapOf("atlixco" to 25.0, "metepec" to 25.0), recetaId = "receta_cr_dulce",
                consumiblesAsociados = consumiblesCrepa) to
            RecetaV2("receta_cr_dulce", "Crepa Dulce Individual", "cr_dulce", listOf(
                ing("masa_crepa", "Masa de Crepa", 1.0, "pz"),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("cr_salada", "Crepa Individual Salada", "🥪", "CREPAS_SALADAS",
                mapOf("atlixco" to 35.0, "metepec" to 35.0), recetaId = "receta_cr_salada",
                consumiblesAsociados = consumiblesCrepa) to
            RecetaV2("receta_cr_salada", "Crepa Salada Individual", "cr_salada", listOf(
                ing("masa_crepa", "Masa de Crepa", 1.0, "pz"),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            // ── COMBOS ──────────────────────────────────────────────────────
            SalesInventoryProductV2("combo_2d", "Combo 2 Crepas Dulces", "🥞🥞", "Combos",
                mapOf("atlixco" to 50.0, "metepec" to 40.0), esCombo = true,
                recetaId = "receta_combo_2d", consumiblesAsociados = listOf(
                    consumible("charola"), consumible("tenedor", 2.0),
                    consumible("servilletas", 2.0), consumible("papel_hamburguesero"))) to
            RecetaV2("receta_combo_2d", "Combo 2 Dulces", "combo_2d", listOf(
                ing("masa_crepa", "Masa de Crepa", 2.0, "pz"),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 2.0, "pz"),
                ing("servilletas", "Servilletas", 2.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("combo_2s", "Combo 2 Crepas Saladas", "🥪🥪", "Combos",
                mapOf("atlixco" to 70.0, "metepec" to 60.0), esCombo = true,
                recetaId = "receta_combo_2s", consumiblesAsociados = listOf(
                    consumible("charola"), consumible("tenedor", 2.0),
                    consumible("servilletas", 2.0), consumible("papel_hamburguesero"))) to
            RecetaV2("receta_combo_2s", "Combo 2 Saladas", "combo_2s", listOf(
                ing("masa_crepa", "Masa de Crepa", 2.0, "pz"),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 2.0, "pz"),
                ing("servilletas", "Servilletas", 2.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("combo_duo", "Combo Dulce y Salada", "🥞🥪", "Combos",
                mapOf("atlixco" to 65.0, "metepec" to 55.0), esCombo = true,
                recetaId = "receta_combo_duo", consumiblesAsociados = listOf(
                    consumible("charola"), consumible("tenedor", 2.0),
                    consumible("servilletas", 2.0), consumible("papel_hamburguesero"))) to
            RecetaV2("receta_combo_duo", "Combo Dulce y Salada", "combo_duo", listOf(
                ing("masa_crepa", "Masa de Crepa", 2.0, "pz"),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 2.0, "pz"),
                ing("servilletas", "Servilletas", 2.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            // ── POSTRES ─────────────────────────────────────────────────────
            SalesInventoryProductV2("p_carlota", "Carlota de Limón", "🍰", "POSTRES",
                mapOf("atlixco" to 40.0, "metepec" to 40.0), recetaId = "receta_p_carlota",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_carlota", "Carlota de Limón Porción", "p_carlota", listOf(
                ing("carlota_unidad", "Carlota de Limón", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            SalesInventoryProductV2("p_tiramisu", "Tiramisú", "☕", "POSTRES",
                mapOf("atlixco" to 45.0, "metepec" to 45.0), recetaId = "receta_p_tiramisu",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_tiramisu", "Tiramisú Porción", "p_tiramisu", listOf(
                ing("tiramisu_unidad", "Tiramisú", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            SalesInventoryProductV2("p_fresas", "Fresas con Crema", "🍓", "POSTRES",
                mapOf("atlixco" to 50.0, "metepec" to 50.0), recetaId = "receta_p_fresas",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_fresas", "Fresas con Crema Porción", "p_fresas", listOf(
                ing("fresas_crema_unidad", "Fresas con Crema", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            SalesInventoryProductV2("p_duraznos", "Duraznos con Crema", "🍑", "POSTRES",
                mapOf("atlixco" to 50.0, "metepec" to 50.0), recetaId = "receta_p_duraznos",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_duraznos", "Duraznos con Crema Porción", "p_duraznos", listOf(
                ing("duraznos_crema_unidad", "Duraznos con Crema", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            // ── SNACKS ──────────────────────────────────────────────────────
            SalesInventoryProductV2("s_papas_senc", "Papas Fritas", "🍟", "SNACKS",
                mapOf("atlixco" to 35.0, "metepec" to 35.0), recetaId = "receta_papas_sencillas",
                consumiblesAsociados = listOf(consumible("charola"), consumible("tenedor"), consumible("servilletas"))) to
            RecetaV2("receta_papas_sencillas", "Papas Sencillas", "s_papas_senc", listOf(
                ing("papas", "Papas", 200.0),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz")
            )),

            SalesInventoryProductV2("s_papas_chor", "Papas con Chorizo", "🍟", "SNACKS",
                mapOf("atlixco" to 60.0, "metepec" to 60.0), recetaId = "receta_papas_chorizo",
                consumiblesAsociados = consumiblesSnack) to
            RecetaV2("receta_papas_chorizo", "Papas con Chorizo", "s_papas_chor", listOf(
                ing("papas", "Papas", 200.0),
                ing("chorizo_kg", "Chorizo", 125.0),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("s_boneless", "Boneless", "🍗", "SNACKS",
                mapOf("atlixco" to 90.0, "metepec" to 90.0), recetaId = "receta_s_boneless",
                consumiblesAsociados = consumiblesSnack) to
            RecetaV2("receta_s_boneless", "Boneless Porción", "s_boneless", listOf(
                ing("boneless", "Boneless", 250.0),
                ing("papas", "Papas", 200.0),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("s_nuggets", "Nuggets", "🍗", "SNACKS",
                mapOf("atlixco" to 80.0, "metepec" to 70.0), recetaId = "receta_s_nuggets",
                consumiblesAsociados = consumiblesSnack) to
            RecetaV2("receta_s_nuggets", "Nuggets Porción", "s_nuggets", listOf(
                ing("nuggets", "Nuggets", 200.0),
                ing("papas", "Papas", 200.0),
                ing("charola", "Charola", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            // ── BEBIDAS / FRAPPES ────────────────────────────────────────────
            SalesInventoryProductV2("frappe_oreo", "Frappe Oreo", "🥤", "BEBIDAS",
                mapOf("atlixco" to 35.0, "metepec" to 35.0), toppingsIncluidos = 1,
                recetaId = "receta_frappe_oreo",
                consumiblesAsociados = listOf(consumible("vaso"), consumible("domo"))) to
            RecetaV2("receta_frappe_oreo", "Frappe Oreo", "frappe_oreo", listOf(
                ing("nutella_kg", "Nutella", 20.0),
                ing("hielo", "Hielo", 150.0),
                ing("oreo", "Galleta Oreo", 6.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz")
            )),

            SalesInventoryProductV2("frappe_cocoa", "Frappe Cocoa", "🥤", "BEBIDAS",
                mapOf("atlixco" to 30.0, "metepec" to 30.0), recetaId = "receta_frappe_cocoa",
                consumiblesAsociados = listOf(consumible("vaso"), consumible("domo"))) to
            RecetaV2("receta_frappe_cocoa", "Frappe Cocoa", "frappe_cocoa", listOf(
                ing("nutella_kg", "Nutella", 20.0),
                ing("cocoa", "Cocoa", 40.0),
                ing("hielo", "Hielo", 150.0),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz")
            )),

            SalesInventoryProductV2("frappe_fresa", "Frappe Fresa", "🥤", "BEBIDAS",
                mapOf("atlixco" to 30.0, "metepec" to 30.0), recetaId = "receta_frappe_fresa",
                consumiblesAsociados = listOf(consumible("vaso"), consumible("domo"))) to
            RecetaV2("receta_frappe_fresa", "Frappe Fresa", "frappe_fresa", listOf(
                ing("nutella_kg", "Nutella", 20.0),
                ing("fresas", "Fresas", 50.0),
                ing("hielo", "Hielo", 150.0),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz")
            )),

            SalesInventoryProductV2("frappe_fresa_cocoa", "Frappe Fresa Cocoa", "🥤", "BEBIDAS",
                mapOf("atlixco" to 30.0, "metepec" to 30.0), recetaId = "receta_frappe_fresa_cocoa",
                consumiblesAsociados = listOf(consumible("vaso"), consumible("domo"))) to
            RecetaV2("receta_frappe_fresa_cocoa", "Frappe Fresa Cocoa", "frappe_fresa_cocoa", listOf(
                ing("nutella_kg", "Nutella", 20.0),
                ing("fresas", "Fresas", 50.0),
                ing("cocoa", "Cocoa", 20.0),
                ing("hielo", "Hielo", 150.0),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz")
            ))
        )
    }

    private fun buildRecetasProduccion(): List<RecetaV2> = listOf(
        RecetaV2("receta_masa_crepa", "Masa de Crepa (Tanda 60 pz)", rendimientoPorcion = 60.0,
            ingredientes = listOf(
                ing("harina_kg", "Harina de Trigo", 2000.0),
                ing("leche_lt", "Leche Entera", 3000.0, "ml"),
                ing("mantequilla_kg", "Mantequilla", 125.0),
                ing("huevo_pz", "Huevo", 14.0, "pz"),
                ing("polvora_hornear", "Polvo de Hornear", 5.0),
                ing("bicarbonato", "Bicarbonato", 5.0)
            )),
        RecetaV2("receta_carlota", "Carlota de Limón (4 porciones)", rendimientoPorcion = 4.0,
            ingredientes = listOf(
                ing("leche_evaporada", "Leche Evaporada", 365.0, "ml"),
                ing("lechera_kg", "Lechera", 375.0),
                ing("media_crema", "Media Crema", 225.0),
                ing("jugo_limon", "Jugo de Limón", 150.0, "ml"),
                ing("galletas_maria", "Galletas María", 6.0, "pz")
            )),
        RecetaV2("receta_tiramisu", "Tiramisú (4 porciones)", rendimientoPorcion = 4.0,
            ingredientes = listOf(
                ing("queso_crema_kg", "Queso Crema", 180.0),
                ing("azucar", "Azúcar", 160.0),
                ing("media_crema", "Media Crema", 125.0),
                ing("crema_batir", "Crema para Batir", 100.0, "ml"),
                ing("cafe", "Café", 375.0, "ml"),
                ing("galletas_maria", "Galletas María", 6.0, "pz")
            )),
        RecetaV2("receta_fresas_crema", "Fresas con Crema (4 porciones)", rendimientoPorcion = 4.0,
            ingredientes = listOf(
                ing("crema_batir", "Crema para Batir", 500.0, "ml"),
                ing("azucar_glas", "Azúcar Glass", 40.0),
                ing("queso_crema_kg", "Queso Crema", 45.0),
                ing("fresas", "Fresas", 100.0)
            )),
        RecetaV2("receta_duraznos_crema", "Duraznos con Crema (4 porciones)", rendimientoPorcion = 4.0,
            ingredientes = listOf(
                ing("crema_batir", "Crema para Batir", 500.0, "ml"),
                ing("azucar_glas", "Azúcar Glass", 40.0),
                ing("queso_crema_kg", "Queso Crema", 45.0),
                ing("durazno_kg", "Duraznos", 100.0)
            ))
    )
}
