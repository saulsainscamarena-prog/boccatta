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
            val branchId = sucursalId.trim().lowercase().replace(" ", "_")
            Timber.tag("SEEDER").i("Inicializando sucursal: $branchId")
            val insumos = db.collection(FirestoreCollections.INSUMOS).get().await()
            if (insumos.isEmpty) {
                return Result.failure(Exception("No hay insumos maestros. Ejecuta inicializarTodoV2 primero."))
            }
            db.collection(FirestoreCollections.SUCURSALES).document(branchId)
                .set(mapOf(
                    "id" to branchId,
                    "nombre" to branchId.replaceFirstChar { it.uppercase() },
                    "activa" to true,
                    "creadaEn" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
            db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(branchId)
                .set(mapOf(
                    "abierta" to false,
                    "turnoActivoId" to "",
                    "ultimaActualizacion" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
            // Batches de máximo 500 operaciones por límite de Firestore
            val chunks = insumos.documents.chunked(400)
            chunks.forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    val insumoId = doc.id
                    val ref = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                        .document("${branchId}_$insumoId")
                    // SetOptions.merge() — no sobreescribe si ya tiene stock real
                    batch.set(ref, mapOf(
                        "id" to "${branchId}_$insumoId",
                        "insumoId" to insumoId,
                        "sucursal" to branchId,
                        "cantidadEnBase" to 0.0,
                        "cantidadDisponible" to 0.0,
                        "ultimaActualizacion" to System.currentTimeMillis()
                    ), SetOptions.merge())
                }
                batch.commit().await()
            }
            // Crear contador de tickets para la sucursal
            db.collection(FirestoreCollections.CONFIGURACION)
                .document("contadores_$branchId")
                .set(mapOf(
                    "ultimo_ticket" to 0L,
                    "sucursal" to branchId,
                    "creado" to System.currentTimeMillis()
                ), SetOptions.merge()).await()

            Timber.tag("SEEDER").i("Sucursal $branchId inicializada con ${insumos.size()} insumos")
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
            inicializarCategorias()
            inicializarCatalogoOpciones()
            inicializarRecetasProduccion()
            inicializarSucursal("atlixco").getOrThrow()
            inicializarSucursal("metepec").getOrThrow()
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

    private suspend fun inicializarCategorias() {
        val categorias = listOf("CREPAS_DULCES", "CREPAS_SALADAS", "COMBOS", "POSTRES", "SNACKS", "BEBIDAS")
        val batch = db.batch()
        categorias.forEach { categoria ->
            batch.set(
                db.collection(FirestoreCollections.CATEGORIAS).document(categoria),
                mapOf(
                    "id" to categoria,
                    "nombre" to categoria,
                    "activa" to true,
                    "ultimaActualizacion" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
        }
        batch.commit().await()
        Timber.tag("SEEDER").i("${categorias.size} categorias inicializadas")
    }

    private fun buildCatalogoOpciones(): List<OpcionCatalogo> {
        fun op(
            id: String,
            nombre: String,
            tipo: TipoCatalogo,
            premium: Boolean = false,
            extra: Double = 0.0
        ) = OpcionCatalogo(
            id = id,
            nombre = nombre,
            tipo = tipo,
            esPremium = premium,
            costoExtra = extra,
            activo = true,
            defecto = true,
            creadoEn = System.currentTimeMillis()
        )

        return listOf(
            op("base_nutella", "Nutella", TipoCatalogo.BASE_UNTABLE),
            op("base_lechera", "Lechera", TipoCatalogo.BASE_UNTABLE),
            op("base_zarzamora", "Zarzamora", TipoCatalogo.BASE_UNTABLE),
            op("base_mermelada_fresa", "Mermelada de Fresa", TipoCatalogo.BASE_UNTABLE),
            op("base_philadelphia", "Philadelphia", TipoCatalogo.BASE_UNTABLE),
            op("top_durazno", "Durazno", TipoCatalogo.TOPPING),
            op("top_fresa", "Fresa Natural", TipoCatalogo.TOPPING),
            op("top_coco", "Coco Rayado", TipoCatalogo.TOPPING),
            op("top_granillo_chocolate", "Granillo Chocolate", TipoCatalogo.TOPPING),
            op("top_granillo_colores", "Granillo Colores", TipoCatalogo.TOPPING),
            op("premium_oreo", "Oreo", TipoCatalogo.TOPPING_PREMIUM, premium = true, extra = 10.0),
            op("premium_bombon", "Bombon", TipoCatalogo.TOPPING_PREMIUM, premium = true, extra = 10.0),
            op("premium_nuez", "Nuez", TipoCatalogo.TOPPING_PREMIUM, premium = true, extra = 10.0),
            op("aderezo_bbq", "BBQ", TipoCatalogo.ADEREZO),
            op("aderezo_buffalo", "Buffalo", TipoCatalogo.ADEREZO),
            op("aderezo_blue_cheese", "Blue Cheese", TipoCatalogo.ADEREZO),
            op("aderezo_valentina", "Valentina", TipoCatalogo.ADEREZO),
            op("aderezo_queso_amarillo", "Queso Amarillo", TipoCatalogo.ADEREZO),
            op("aderezo_catsup", "Catsup", TipoCatalogo.ADEREZO),
            op("aderezo_mayonesa", "Mayonesa", TipoCatalogo.ADEREZO),
            op("frappe_cocoa", "Cocoa", TipoCatalogo.SABOR_FRAPPE),
            op("frappe_fresa", "Fresa", TipoCatalogo.SABOR_FRAPPE),
            op("frappe_fresa_cocoa", "Fresa con Cocoa", TipoCatalogo.SABOR_FRAPPE),
            op("frappe_oreo", "Oreo", TipoCatalogo.SABOR_FRAPPE),
            op("pres_juntas", "Juntas", TipoCatalogo.PRESENTACION),
            op("pres_separadas", "Separadas", TipoCatalogo.PRESENTACION)
        )
    }

    private suspend fun inicializarCatalogoOpciones() {
        val opciones = buildCatalogoOpciones()
        val batch = db.batch()
        opciones.forEach { opcion ->
            batch.set(
                db.collection(FirestoreCollections.CATALOGO_OPCIONES).document(opcion.id),
                opcion,
                SetOptions.merge()
            )
        }
        batch.commit().await()
        Timber.tag("SEEDER").i("${opciones.size} opciones de catalogo inicializadas")
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
            presentaciones = listOf(PresentacionInsumo("Saco 5kg", "kg", 5000.0, 1.0, 100.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 5000.0, 20.0, 10.0, 60.0),
                pres("Bolsa 5 kg", "5 kg", 5000.0, 2500.0, 10000.0, 100.0, 50.0, 300.0)
            )),
        InsumoV2("leche_lt", "Leche Entera", "Materia Prima", "ml", 0.024, 3000.0, 500.0,
            presentacionesCompra = listOf(
                pres("Litro", "1 L", 1000.0, 500.0, 2000.0, 24.0, 15.0, 35.0),
                pres("Garrafón 4 L", "4 L", 4000.0, 2000.0, 6000.0, 96.0, 50.0, 150.0)
            )),
        InsumoV2("mantequilla_kg", "Mantequilla", "Materia Prima", "g", 0.160, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Barra 125 g", "125 g", 125.0, 100.0, 250.0, 20.0, 12.0, 40.0),
                pres("Barra 500 g", "500 g", 500.0, 250.0, 1000.0, 80.0, 40.0, 160.0)
            )),
        InsumoV2("huevo_pz", "Huevo", "Materia Prima", "pz", 3.0, 100.0, 12.0,
            presentacionesCompra = listOf(
                pres("Pieza", "1 pieza", 1.0, 1.0, 1.0, 3.0, 2.0, 5.0),
                pres("Cartón 12", "12 piezas", 12.0, 6.0, 24.0, 36.0, 20.0, 70.0),
                pres("Cartón 30", "30 piezas", 30.0, 15.0, 36.0, 90.0, 50.0, 150.0)
            )),
        InsumoV2("polvora_hornear", "Polvo de Hornear", "Materia Prima", "g", 0.150, 200.0, 20.0,
            presentacionesCompra = listOf(
                pres("Sobre 20 g", "20 g", 20.0, 10.0, 50.0, 3.0, 1.0, 10.0),
                pres("Bolsa 100 g", "100 g", 100.0, 50.0, 200.0, 15.0, 8.0, 30.0)
            )),
        InsumoV2("bicarbonato", "Bicarbonato", "Materia Prima", "g", 0.050, 200.0, 20.0,
            presentacionesCompra = listOf(
                pres("Sobre 20 g", "20 g", 20.0, 10.0, 50.0, 1.0, 0.5, 5.0),
                pres("Bolsa 100 g", "100 g", 100.0, 50.0, 200.0, 5.0, 2.0, 15.0)
            )),
        InsumoV2("azucar", "Azúcar", "Materia Prima", "g", 0.020, 5000.0, 500.0,
            presentacionesCompra = listOf(
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 5000.0, 20.0, 10.0, 60.0),
                pres("Bolsa 5 kg", "5 kg", 5000.0, 2500.0, 10000.0, 100.0, 50.0, 300.0)
            )),
        InsumoV2("azucar_glas", "Azúcar Glass", "Materia Prima", "g", 0.040, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 500.0, 8.0, 4.0, 20.0),
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 2000.0, 40.0, 20.0, 100.0)
            )),
        InsumoV2("hielo", "Hielo", "Bases", "g", 0.010, 5000.0, 500.0,
            presentacionesCompra = listOf(
                pres("Bolsa 2 kg", "2 kg", 2000.0, 500.0, 5000.0, 20.0, 10.0, 40.0)
            )),
        InsumoV2("agua", "Agua", "Materia Prima", "ml", 0.002, 10000.0, 1000.0,
            presentacionesCompra = listOf(
                pres("Litro", "1 L", 1000.0, 500.0, 5000.0, 2.0, 1.0, 10.0),
                pres("Garrafon 20 L", "20 L", 20000.0, 5000.0, 40000.0, 40.0, 20.0, 80.0)
            )),
        InsumoV2("vainilla", "Vainilla", "Materia Prima", "ml", 0.080, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 120 ml", "120 ml", 120.0, 40.0, 500.0, 10.0, 5.0, 30.0),
                pres("Botella 500 ml", "500 ml", 500.0, 100.0, 1000.0, 40.0, 20.0, 80.0)
            )),
        // Bases
        InsumoV2("nutella_kg", "Nutella", "Bases", "g", 0.190, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Bote 200 g", "200 g", 200.0, 100.0, 400.0, 38.0, 20.0, 80.0),
                pres("Bote 400 g", "400 g", 400.0, 200.0, 800.0, 76.0, 40.0, 160.0),
                pres("Bote 750 g", "750 g", 750.0, 400.0, 1000.0, 142.0, 80.0, 250.0)
            )),
        InsumoV2("queso_crema_kg", "Queso Crema", "Bases", "g", 0.150, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Paquete 190 g", "190 g", 190.0, 100.0, 400.0, 28.0, 15.0, 60.0),
                pres("Paquete 400 g", "400 g", 400.0, 200.0, 800.0, 60.0, 30.0, 120.0)
            )),
        InsumoV2("lechera_kg", "Lechera", "Bases", "g", 0.095, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Lata 385 g", "385 g", 385.0, 200.0, 500.0, 36.0, 20.0, 60.0)
            )),
        InsumoV2("media_crema", "Media Crema", "Bases", "g", 0.060, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Lata 220 g", "220 g", 220.0, 100.0, 400.0, 13.0, 8.0, 30.0),
                pres("Lata 360 g", "360 g", 360.0, 200.0, 500.0, 21.0, 12.0, 40.0)
            )),
        InsumoV2("leche_evaporada", "Leche Evaporada", "Bases", "ml", 0.080, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Lata 360 ml", "360 ml", 360.0, 200.0, 500.0, 28.0, 15.0, 50.0)
            )),
        InsumoV2("crema_batir", "Crema para Batir", "Bases", "ml", 0.070, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Caja 250 ml", "250 ml", 250.0, 200.0, 400.0, 17.0, 10.0, 35.0),
                pres("Caja 500 ml", "500 ml", 500.0, 300.0, 1000.0, 35.0, 20.0, 70.0)
            )),
        InsumoV2("jugo_limon", "Jugo de Limón", "Bases", "ml", 0.040, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 250 ml", "250 ml", 250.0, 100.0, 500.0, 10.0, 5.0, 25.0),
                pres("Botella 500 ml", "500 ml", 500.0, 200.0, 1000.0, 20.0, 10.0, 50.0)
            )),
        InsumoV2("cafe", "Café", "Bases", "ml", 0.060, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Frasco 200 ml", "200 ml", 200.0, 100.0, 400.0, 12.0, 6.0, 30.0),
                pres("Frasco 500 ml", "500 ml", 500.0, 200.0, 1000.0, 30.0, 15.0, 70.0)
            )),
        InsumoV2("cocoa", "Cocoa en Polvo", "Bases", "g", 0.080, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 16.0, 8.0, 40.0),
                pres("Bolsa 500 g", "500 g", 500.0, 250.0, 1000.0, 40.0, 20.0, 100.0)
            )),
        InsumoV2("salsa_tomate_lt", "Salsa de Tomate", "Salado", "ml", 0.050, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Botella 250 ml", "250 ml", 250.0, 100.0, 500.0, 12.0, 6.0, 30.0),
                pres("Botella 500 ml", "500 ml", 500.0, 200.0, 1000.0, 25.0, 12.0, 60.0)
            )),
        InsumoV2("zarzamora_kg", "Zarzamora", "Bases", "g", 0.030, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bolsa 500 g", "500 g", 500.0, 200.0, 1000.0, 15.0, 8.0, 40.0)
            )),
        InsumoV2("mermelada_fresa", "Mermelada de Fresa", "Bases", "g", 0.060, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bote 400 g", "400 g", 400.0, 200.0, 600.0, 24.0, 12.0, 50.0)
            )),
        // Aderezos
        InsumoV2("bbq", "Salsa BBQ", "Aderezo", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 500 g", "500 g", 500.0, 200.0, 1000.0, 25.0, 12.0, 60.0)
            )),
        InsumoV2("buffalo", "Salsa Buffalo", "Aderezo", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 500 g", "500 g", 500.0, 200.0, 1000.0, 25.0, 12.0, 60.0)
            )),
        InsumoV2("blue_cheese", "Blue Cheese", "Aderezo", "g", 0.080, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 400 g", "400 g", 400.0, 200.0, 800.0, 32.0, 15.0, 80.0)
            )),
        InsumoV2("valentina", "Valentina", "Aderezo", "g", 0.030, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 350 ml", "350 ml", 350.0, 200.0, 500.0, 10.0, 5.0, 25.0)
            )),
        InsumoV2("queso_amarillo", "Queso Amarillo", "Aderezo", "g", 0.070, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bolsa 400 g", "400 g", 400.0, 200.0, 800.0, 28.0, 15.0, 60.0)
            )),
        InsumoV2("catsup", "Catsup", "Aderezo", "g", 0.030, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 500 g", "500 g", 500.0, 200.0, 1000.0, 15.0, 8.0, 40.0)
            )),
        InsumoV2("mayonesa", "Mayonesa", "Aderezo", "g", 0.040, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 400 g", "400 g", 400.0, 200.0, 800.0, 16.0, 8.0, 40.0)
            )),
        // Salados
        InsumoV2("queso_mozzarella_kg", "Queso Mozzarella", "Salado", "g", 0.180, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Bolsa 500 g", "500 g", 500.0, 200.0, 1000.0, 90.0, 40.0, 200.0),
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 2000.0, 180.0, 80.0, 400.0)
            )),
        InsumoV2("jamon_kg", "Jamón de Pierna", "Salado", "g", 0.120, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Paquete 200 g", "200 g", 200.0, 100.0, 500.0, 24.0, 12.0, 60.0),
                pres("Paquete 500 g", "500 g", 500.0, 200.0, 1000.0, 60.0, 30.0, 150.0)
            )),
        InsumoV2("peperoni_kg", "Peperoni", "Salado", "g", 0.240, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Paquete 250 g", "250 g", 250.0, 100.0, 500.0, 60.0, 30.0, 140.0),
                pres("Paquete 500 g", "500 g", 500.0, 200.0, 1000.0, 120.0, 60.0, 280.0)
            )),
        InsumoV2("chorizo_kg", "Chorizo", "Salado", "g", 0.115, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Paquete 250 g", "250 g", 250.0, 100.0, 500.0, 28.0, 15.0, 60.0),
                pres("Paquete 500 g", "500 g", 500.0, 200.0, 1000.0, 57.0, 30.0, 140.0)
            )),
        InsumoV2("pina_kg", "Piña", "Salado", "g", 0.040, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Pieza entera ~1 kg", "~1 kg", 1000.0, 500.0, 2000.0, 40.0, 20.0, 100.0),
                pres("Bolsa 500 g (precortada)", "500 g", 500.0, 200.0, 1000.0, 20.0, 10.0, 50.0)
            )),
        // Toppings
        InsumoV2("fresas", "Fresas", "Topping", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Domo 496 g", "Domo 496 g", 496.0, 200.0, 600.0, 25.0, 15.0, 60.0),
                pres("Granel por kg", "1 kg", 1000.0, 250.0, 5000.0, 50.0, 20.0, 150.0),
                pres("1/2 kg", "500 g", 500.0, 400.0, 600.0, 25.0, 10.0, 80.0),
                pres("1/4 kg", "250 g", 250.0, 200.0, 300.0, 12.0, 5.0, 40.0)
            )),
        InsumoV2("durazno_kg", "Duraznos", "Topping", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Lata 450 g", "450 g", 450.0, 200.0, 600.0, 22.0, 12.0, 50.0),
                pres("Granel por kg", "1 kg", 1000.0, 250.0, 5000.0, 50.0, 20.0, 150.0)
            )),
        InsumoV2("coco_rayado", "Coco Rayado", "Topping", "g", 0.080, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 100 g", "100 g", 100.0, 50.0, 200.0, 8.0, 4.0, 20.0),
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 16.0, 8.0, 40.0)
            )),
        InsumoV2("granillo_chocolate", "Granillo Chocolate", "Topping", "g", 0.100, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 20.0, 10.0, 50.0)
            )),
        InsumoV2("granillo_colores", "Granillo Colores", "Topping", "g", 0.080, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 16.0, 8.0, 40.0)
            )),
        InsumoV2("bombon_kg", "Bombón", "Topping", "g", 0.150, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 30.0, 15.0, 80.0)
            )),
        InsumoV2("nuez_kg", "Nuez", "Topping", "g", 0.200, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 150 g", "150 g", 150.0, 50.0, 300.0, 30.0, 15.0, 80.0),
                pres("Bolsa 300 g", "300 g", 300.0, 150.0, 500.0, 60.0, 30.0, 150.0)
            )),
        // Snacks
        InsumoV2("boneless", "Boneless", "Snacks", "g", 0.080, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa 2.6kg", "kg", 2600.0, 10.0, 208.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 2.6 kg", "2.6 kg", 2600.0, 1500.0, 5000.0, 208.0, 100.0, 400.0),
                pres("Porción 250 g", "250 g", 250.0, 200.0, 300.0, 20.0, 10.0, 40.0)
            )),
        InsumoV2("nuggets", "Nuggets", "Snacks", "g", 0.070, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa 2.6kg", "kg", 2600.0, 10.0, 182.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 2.6 kg", "2.6 kg", 2600.0, 1500.0, 5000.0, 182.0, 100.0, 400.0),
                pres("Porción 200 g", "~7-8 pz", 200.0, 150.0, 250.0, 14.0, 8.0, 30.0)
            )),
        InsumoV2("papas", "Papas", "Snacks", "g", 0.040, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa 2.3kg", "kg", 2300.0, 10.0, 92.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 2.3 kg", "2.3 kg", 2300.0, 1500.0, 5000.0, 92.0, 50.0, 200.0),
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 2000.0, 40.0, 20.0, 100.0)
            )),
        // Galletas
        InsumoV2("oreo", "Galleta Oreo", "Topping", "pz", 0.50, 294.0, 28.0,
            presentaciones = listOf(
                PresentacionInsumo("Caja (21 paquetes)", "box", 294.0, 5.0, 147.0),
                PresentacionInsumo("Paquete (14 pz)", "pkg", 14.0, 50.0, 7.0)),
            presentacionesCompra = listOf(
                pres("Caja", "21 paquetes × 14 galletas", 294.0, 200.0, 400.0, 147.0, 80.0, 250.0),
                pres("Paquete individual 14", "14 galletas", 14.0, 10.0, 16.0, 7.0, 4.0, 20.0),
                pres("Paquete individual 4", "4 galletas", 4.0, 4.0, 6.0, 2.0, 1.0, 8.0)
            )),
        InsumoV2("galletas_maria", "Galletas María", "Bases", "pz", 0.035, 200.0, 40.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (40 pz)", "pkg", 40.0, 20.0, 15.0)),
            presentacionesCompra = listOf(
                pres("Paquete 3 unid", "3 × 170 g ~35-40 pz c/u", 120.0, 80.0, 160.0, 15.0, 8.0, 30.0),
                pres("Paquete individual", "170 g ~35-40 pz", 40.0, 30.0, 50.0, 5.0, 3.0, 12.0)
            )),
        InsumoV2("galletas_mexicana", "Galletas Mexicanas", "Bases", "pz", 0.025, 180.0, 60.0,
            presentaciones = listOf(PresentacionInsumo("Paquete 5u (60 pz)", "pkg", 60.0, 15.0, 45.0)),
            presentacionesCompra = listOf(
                pres("Paquete 5 unid", "5 × 135 g ~12-15 pz c/u", 75.0, 50.0, 100.0, 45.0, 25.0, 80.0),
                pres("Paquete individual", "135 g ~12-15 pz", 15.0, 10.0, 20.0, 9.0, 5.0, 20.0)
            )),
        // Producción
        InsumoV2("masa_crepa", "Masa de Crepa Preparada", "Producción", "pz", 5.0, 60.0, 12.0,
            presentacionesCompra = listOf(
                pres("Tanda (60 crepas)", "60 piezas", 60.0, 30.0, 120.0, 300.0, 150.0, 600.0)
            )),
        InsumoV2("carlota_unidad", "Carlota de Limón", "Producción", "pz", 15.0, 0.0, 4.0),
        InsumoV2("tiramisu_unidad", "Tiramisú", "Producción", "pz", 22.0, 0.0, 4.0),
        InsumoV2("fresas_crema_unidad", "Fresas con Crema", "Producción", "pz", 18.0, 0.0, 4.0),
        InsumoV2("duraznos_crema_unidad", "Duraznos con Crema", "Producción", "pz", 18.0, 0.0, 4.0),
        // Consumibles
        InsumoV2("charola", "Charola Unicel", "Consumible", "pz", 1.60, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa (50 pz)", "pz", 1.0, 200.0, 80.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 50 pz", "50 piezas", 50.0, 25.0, 100.0, 80.0, 40.0, 150.0)
            )),
        InsumoV2("tenedor", "Tenedor Plástico", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            )),
        InsumoV2("servilletas", "Servilletas", "Consumible", "pz", 0.25, 600.0, 100.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (200 pz)", "pz", 1.0, 600.0, 45.0)),
            presentacionesCompra = listOf(
                pres("Paquete 200 pz", "200 piezas", 200.0, 100.0, 400.0, 45.0, 25.0, 80.0)
            )),
        InsumoV2("vaso", "Vaso 16oz", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            )),
        InsumoV2("domo", "Domo", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            )),
        InsumoV2("papel_hamburguesero", "Papel Hamburguesero", "Consumible", "pz", 0.30, 300.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (100 pz)", "pz", 1.0, 300.0, 30.0)),
            presentacionesCompra = listOf(
                pres("Paquete 100 pz", "100 piezas", 100.0, 50.0, 200.0, 30.0, 15.0, 60.0)
            )),
        InsumoV2("cuchara", "Cuchara", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            ))
    )

    private fun ing(id: String, nombre: String, cantidad: Double, unidad: String = "g") =
        IngredienteReceta(id, nombre, cantidad, unidad)

    private fun consumible(id: String, cantidad: Double = 1.0) =
        ConsumibleRequerido(id, cantidad, "pz")

    private fun pres(
        nombre: String, descripcion: String = "",
        contenidoSugerido: Double, contenidoMin: Double, contenidoMax: Double,
        precioSugerido: Double, precioMin: Double, precioMax: Double
    ) = PresentacionCompraPreview(nombre, descripcion, contenidoSugerido, contenidoMin, contenidoMax, precioSugerido, precioMin, precioMax)

    private fun cfg(key: String, title: String, type: ConfigFieldType, options: List<String>, required: Boolean = false, multiMax: Int? = null, preciosExtra: Map<String, Double> = emptyMap()) =
        ConfigOptionGroup(key, title, type, options, required, multiMax, preciosExtra = preciosExtra)

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
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("cr_salada", "Crepa Individual Salada", "🥪", "CREPAS_SALADAS",
                mapOf("atlixco" to 35.0, "metepec" to 35.0), recetaId = "receta_cr_salada",
                consumiblesAsociados = consumiblesCrepa) to
            RecetaV2("receta_cr_salada", "Crepa Salada Individual", "cr_salada", listOf(
                ing("masa_crepa", "Masa de Crepa", 1.0, "pz"),
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            // ── COMBOS ──────────────────────────────────────────────────────
            SalesInventoryProductV2("combo_2d", "Combo 2 Crepas Dulces", "🥞🥞", "Combos",
                mapOf("atlixco" to 50.0, "metepec" to 50.0), esCombo = true,
                recetaId = "receta_combo_2d", consumiblesAsociados = listOf(
                    consumible("charola"), consumible("tenedor", 2.0),
                    consumible("servilletas", 2.0), consumible("papel_hamburguesero"))) to
            RecetaV2("receta_combo_2d", "Combo 2 Dulces", "combo_2d", listOf(
                ing("masa_crepa", "Masa de Crepa", 2.0, "pz"),
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 2.0, "pz"),
                ing("servilletas", "Servilletas", 2.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("combo_2s", "Combo 2 Crepas Saladas", "🥪🥪", "Combos",
                mapOf("atlixco" to 70.0, "metepec" to 70.0), esCombo = true,
                recetaId = "receta_combo_2s", consumiblesAsociados = listOf(
                    consumible("charola"), consumible("tenedor", 2.0),
                    consumible("servilletas", 2.0), consumible("papel_hamburguesero"))) to
            RecetaV2("receta_combo_2s", "Combo 2 Saladas", "combo_2s", listOf(
                ing("masa_crepa", "Masa de Crepa", 2.0, "pz"),
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 2.0, "pz"),
                ing("servilletas", "Servilletas", 2.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("combo_duo", "Combo Dulce y Salada", "🥞🥪", "Combos",
                mapOf("atlixco" to 65.0, "metepec" to 65.0), esCombo = true,
                recetaId = "receta_combo_duo", consumiblesAsociados = listOf(
                    consumible("charola"), consumible("tenedor", 2.0),
                    consumible("servilletas", 2.0), consumible("papel_hamburguesero")),
                configSchema = listOf(
                    cfg("base_dulce", "Base crepa dulce", ConfigFieldType.SINGLE_CHIP, listOf("Nutella", "Philadelphia", "Lechera", "Zarzamora", "Mermelada de Fresa"), required = true),
                    cfg("toppings_dulces", "Toppings dulces (hasta 3)", ConfigFieldType.MULTI_CHIP, listOf("Fresa Natural", "Durazno", "Coco Rayado", "Granillo Chocolate", "Granillo Colores"), multiMax = 3),
                    cfg("toppings_premium", "Toppings premium (+$10)", ConfigFieldType.MULTI_CHIP, listOf("Oreo", "Nuez", "Bombón"), preciosExtra = mapOf("Oreo" to 10.0, "Nuez" to 10.0, "Bombón" to 10.0)),
                    cfg("tipo_salada", "Tipo crepa salada", ConfigFieldType.SINGLE_CHIP, listOf("Hawaiana", "Peperoni", "Jamón y Queso"), required = true),
                    cfg("base_salada", "Base crepa salada", ConfigFieldType.SINGLE_CHIP, listOf("Tomate", "Philadelphia"), required = true),
                    cfg("toppings_salados", "Toppings salados", ConfigFieldType.MULTI_CHIP, listOf("Jamón", "Peperoni", "Piña")),
                    cfg("aderezos", "Aderezos", ConfigFieldType.MULTI_CHECKBOX, listOf("BBQ", "Buffalo", "Blue Cheese", "Queso Amarillo", "Cátsup", "Mayonesa", "Valentina")),
                    cfg("presentacion", "Presentación", ConfigFieldType.SINGLE_CHIP, listOf("Juntas", "Separadas"), required = true)
                )) to
            RecetaV2("receta_combo_duo", "Combo Dulce y Salada", "combo_duo", listOf(
                ing("masa_crepa", "Masa de Crepa", 2.0, "pz"),
                ing("charola", "Charola Unicel", 1.0, "pz"),
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
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz")
            )),

            SalesInventoryProductV2("s_papas_chor", "Papas con Chorizo", "🍟", "SNACKS",
                mapOf("atlixco" to 60.0, "metepec" to 60.0), recetaId = "receta_papas_chorizo",
                consumiblesAsociados = consumiblesSnack) to
            RecetaV2("receta_papas_chorizo", "Papas con Chorizo", "s_papas_chor", listOf(
                ing("papas", "Papas", 200.0),
                ing("chorizo_kg", "Chorizo", 125.0),
                ing("charola", "Charola Unicel", 1.0, "pz"),
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
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("s_nuggets", "Nuggets", "🍗", "SNACKS",
                mapOf("atlixco" to 80.0, "metepec" to 80.0), recetaId = "receta_s_nuggets",
                consumiblesAsociados = consumiblesSnack) to
            RecetaV2("receta_s_nuggets", "Nuggets Porción", "s_nuggets", listOf(
                ing("nuggets", "Nuggets", 200.0),
                ing("papas", "Papas", 200.0),
                ing("charola", "Charola Unicel", 1.0, "pz"),
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
                ing("agua", "Agua", 1000.0, "ml"),
                ing("mantequilla_kg", "Mantequilla", 125.0),
                ing("huevo_pz", "Huevo", 14.0, "pz"),
                ing("polvora_hornear", "Polvo de Hornear", 5.0),
                ing("bicarbonato", "Bicarbonato", 5.0),
                ing("vainilla", "Vainilla", 40.0, "ml")
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

