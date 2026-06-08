package com.bocatta.pos.data.repository

import android.content.Context

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
    private val context: Context,
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
            val branchId = sucursalId.trim().lowercase(java.util.Locale.getDefault()).replace(" ", "_")
            Timber.tag("SEEDER").i("Inicializando sucursal: $branchId")
            val insumos = db.collection(FirestoreCollections.INSUMOS).get().await()
            if (insumos.isEmpty) {
                return Result.failure(Exception("No hay insumos maestros. Ejecuta inicializarTodoV2 primero."))
            }
            val sucursalRef = db.collection(FirestoreCollections.SUCURSALES).document(branchId)
            if (!sucursalRef.get().await().exists()) {
                sucursalRef.set(mapOf(
                    "id" to branchId,
                    "nombre" to branchId.replaceFirstChar { it.uppercase(java.util.Locale.getDefault()) },
                    "activa" to true,
                    "creadaEn" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
            }
            val configSucursalRef = db.collection(FirestoreCollections.SUCURSAL_CONFIG).document(branchId)
            if (!configSucursalRef.get().await().exists()) {
                configSucursalRef.set(mapOf(
                    "abierta" to false,
                    "turnoActivoId" to "",
                    "ultimaActualizacion" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
            }
            // Batches de máximo 500 operaciones por límite de Firestore
            val inventarioExistente = cargarInventarioSucursalExistente(branchId)
            val insumosParaInicializar = insumos.documents.filter { doc ->
                "${branchId}_${doc.id}" !in inventarioExistente
            }
            val chunks = insumosParaInicializar.chunked(400)
            chunks.forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    val insumoId = doc.id
                    val ref = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                        .document("${branchId}_$insumoId")
                    // Solo se crean insumos faltantes; los existentes conservan su stock real.
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
            val contadorRef = db.collection(FirestoreCollections.CONFIGURACION)
                .document("contadores_$branchId")
            if (!contadorRef.get().await().exists()) {
                contadorRef.set(mapOf(
                    "ultimo_ticket" to 0L,
                    "sucursal" to branchId,
                    "creado" to System.currentTimeMillis()
                ), SetOptions.merge()).await()
            }

            Timber.tag("SEEDER").i("Sucursal $branchId inicializada con ${insumosParaInicializar.size} insumos nuevos")
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
    private suspend fun cargarInventarioSucursalExistente(branchId: String): Set<String> {
        val collection = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
        val porSucursal = collection.whereEqualTo("sucursal", branchId).get().await().documents
        val porBranch = collection.whereEqualTo("branchId", branchId).get().await().documents
        return (porSucursal + porBranch).map { it.id }.toSet()
    }

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

    suspend fun inicializarTenantNuevo(tenantId: String, businessType: String, sucursalId: String): Result<Unit> {
        return try {
            val fileName = when (businessType) {
                "RETAIL" -> "template_retail.json"
                "SERVICES" -> "template_servicios.json" // si no existe, luego haremos fallback
                else -> "template_restaurante.json"
            }
            
            Timber.tag("SEEDER").i("Iniciando seeder para tenant $tenantId con plantilla $fileName...")
            
            val jsonString = try {
                context.assets.open("templates/$fileName").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                // Fallback a restaurante si no existe el de servicios
                context.assets.open("templates/template_restaurante.json").bufferedReader().use { it.readText() }
            }
            
            val json = org.json.JSONObject(jsonString)
            val productosArray = json.optJSONArray("productos")
            
            if (productosArray != null) {
                val batch = db.batch()
                for (i in 0 until productosArray.length()) {
                    val prodObj = productosArray.getJSONObject(i)
                    val nombre = prodObj.optString("nombre", "")
                    val id = prodObj.optString("id", nombre.lowercase(java.util.Locale.getDefault()).replace(" ", "_"))
                    val cat = prodObj.optString("categoria", "GENERAL")
                    val precio = prodObj.optDouble("precio", 0.0)
                    val reqStock = prodObj.optBoolean("requiresStock", false)
                    val hasVar = prodObj.optBoolean("hasVariants", false)
                    
                    val p = SalesInventoryProductV2(
                        id = id,
                        tenantId = tenantId,
                        businessType = businessType,
                        nombre = nombre,
                        categoria = cat,
                        precioVenta = mapOf(sucursalId.lowercase(java.util.Locale.getDefault()) to precio),
                        requiresStock = reqStock,
                        hasVariants = hasVar,
                        activo = true
                    )
                    
                    val ref = db.collection(FirestoreCollections.PRODUCTOS).document(id)
                    batch.set(ref, p)
                }
                batch.commit().await()
                Timber.tag("SEEDER").i("Seeding completado con ${productosArray.length()} productos para $tenantId.")
            }
            
            // Inicializar la sucursal vacia
            inicializarSucursal(sucursalId).getOrThrow()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SEEDER").e(e, "Error al inicializar tenant nuevo: $tenantId")
            Result.failure(e)
        }
    }

    // ── INSUMOS MAESTROS ──────────────────────────────────────────────────────

    private suspend fun inicializarInsumos() {
        val existentes = db.collection(FirestoreCollections.INSUMOS)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()
        val insumos = buildInsumos().filterNot { it.id in existentes }
        if (insumos.isEmpty()) {
            Timber.tag("SEEDER").i("Insumos maestros existentes preservados")
            return
        }
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
            productoRepo.crearProductoConRecetaSiFalta(producto, receta)
        }
        Timber.tag("SEEDER").i("${pares.size} productos con recetas inicializados")
    }

    private suspend fun inicializarRecetasProduccion() {
        val existentes = db.collection(FirestoreCollections.RECETAS_PRODUCCION)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()
        val recetas = buildRecetasProduccion().filterNot { it.id in existentes }
        if (recetas.isEmpty()) {
            Timber.tag("SEEDER").i("Recetas de produccion existentes preservadas")
            return
        }
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
        val existentes = db.collection(FirestoreCollections.CATEGORIAS)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()
        val categorias = listOf("CREPAS_DULCES", "CREPAS_SALADAS", "COMBOS", "POSTRES", "SNACKS", "BEBIDAS")
            .filterNot { it in existentes }
        if (categorias.isEmpty()) {
            Timber.tag("SEEDER").i("Categorias existentes preservadas")
            return
        }
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
        val existentes = db.collection(FirestoreCollections.CATALOGO_OPCIONES)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()
        val opciones = buildCatalogoOpciones().filterNot { it.id in existentes }
        if (opciones.isEmpty()) {
            Timber.tag("SEEDER").i("Opciones de catalogo existentes preservadas")
            return
        }
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
            val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
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
            val sucursalId = sucursal.lowercase(java.util.Locale.getDefault())
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
        InsumoV2("harina_kg", "tenant_pionero", "RESTAURANT", "Harina de Trigo", "Materia Prima", "g", 0.020, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Saco 5kg", "kg", 5000.0, 1.0, 100.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 5000.0, 20.0, 10.0, 60.0),
                pres("Bolsa 5 kg", "5 kg", 5000.0, 2500.0, 10000.0, 100.0, 50.0, 300.0)
            )),
        InsumoV2("leche_lt", "tenant_pionero", "RESTAURANT", "Leche Entera", "Materia Prima", "ml", 0.024, 3000.0, 500.0,
            presentacionesCompra = listOf(
                pres("Litro", "1 L", 1000.0, 500.0, 2000.0, 24.0, 15.0, 35.0),
                pres("Garrafón 4 L", "4 L", 4000.0, 2000.0, 6000.0, 96.0, 50.0, 150.0)
            )),
        InsumoV2("mantequilla_kg", "tenant_pionero", "RESTAURANT", "Mantequilla", "Materia Prima", "g", 0.160, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Barra 125 g", "125 g", 125.0, 100.0, 250.0, 20.0, 12.0, 40.0),
                pres("Barra 500 g", "500 g", 500.0, 250.0, 1000.0, 80.0, 40.0, 160.0)
            )),
        InsumoV2("huevo_pz", "tenant_pionero", "RESTAURANT", "Huevo", "Materia Prima", "pz", 3.0, 100.0, 12.0,
            presentacionesCompra = listOf(
                pres("Pieza", "1 pieza", 1.0, 1.0, 1.0, 3.0, 2.0, 5.0),
                pres("Cartón 12", "12 piezas", 12.0, 6.0, 24.0, 36.0, 20.0, 70.0),
                pres("Cartón 30", "30 piezas", 30.0, 15.0, 36.0, 90.0, 50.0, 150.0)
            )),
        InsumoV2("polvora_hornear", "tenant_pionero", "RESTAURANT", "Polvo de Hornear", "Materia Prima", "g", 0.150, 200.0, 20.0,
            presentacionesCompra = listOf(
                pres("Sobre 20 g", "20 g", 20.0, 10.0, 50.0, 3.0, 1.0, 10.0),
                pres("Bolsa 100 g", "100 g", 100.0, 50.0, 200.0, 15.0, 8.0, 30.0)
            )),
        InsumoV2("bicarbonato", "tenant_pionero", "RESTAURANT", "Bicarbonato", "Materia Prima", "g", 0.050, 200.0, 20.0,
            presentacionesCompra = listOf(
                pres("Sobre 20 g", "20 g", 20.0, 10.0, 50.0, 1.0, 0.5, 5.0),
                pres("Bolsa 100 g", "100 g", 100.0, 50.0, 200.0, 5.0, 2.0, 15.0)
            )),
        InsumoV2("azucar", "tenant_pionero", "RESTAURANT", "Azúcar", "Materia Prima", "g", 0.020, 5000.0, 500.0,
            presentacionesCompra = listOf(
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 5000.0, 20.0, 10.0, 60.0),
                pres("Bolsa 5 kg", "5 kg", 5000.0, 2500.0, 10000.0, 100.0, 50.0, 300.0)
            )),
        InsumoV2("azucar_glas", "tenant_pionero", "RESTAURANT", "Azúcar Glass", "Materia Prima", "g", 0.040, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 500.0, 8.0, 4.0, 20.0),
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 2000.0, 40.0, 20.0, 100.0)
            )),
        InsumoV2("hielo", "tenant_pionero", "RESTAURANT", "Hielo", "Bases", "g", 0.010, 5000.0, 500.0,
            presentacionesCompra = listOf(
                pres("Bolsa 2 kg", "2 kg", 2000.0, 500.0, 5000.0, 20.0, 10.0, 40.0)
            )),
        InsumoV2("agua", "tenant_pionero", "RESTAURANT", "Agua", "Materia Prima", "ml", 0.002, 10000.0, 1000.0,
            presentacionesCompra = listOf(
                pres("Litro", "1 L", 1000.0, 500.0, 5000.0, 2.0, 1.0, 10.0),
                pres("Garrafon 20 L", "20 L", 20000.0, 5000.0, 40000.0, 40.0, 20.0, 80.0)
            )),
        InsumoV2("vainilla", "tenant_pionero", "RESTAURANT", "Vainilla", "Materia Prima", "ml", 0.080, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 120 ml", "120 ml", 120.0, 40.0, 500.0, 10.0, 5.0, 30.0),
                pres("Botella 500 ml", "500 ml", 500.0, 100.0, 1000.0, 40.0, 20.0, 80.0)
            )),
        // Bases
        InsumoV2("nutella_kg", "tenant_pionero", "RESTAURANT", "Nutella", "Bases", "g", 0.190, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Bote 200 g", "200 g", 200.0, 100.0, 400.0, 38.0, 20.0, 80.0),
                pres("Bote 400 g", "400 g", 400.0, 200.0, 800.0, 76.0, 40.0, 160.0),
                pres("Bote 750 g", "750 g", 750.0, 400.0, 1000.0, 142.0, 80.0, 250.0)
            )),
        InsumoV2("queso_crema_kg", "tenant_pionero", "RESTAURANT", "Queso Crema", "Bases", "g", 0.150, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Paquete 190 g", "190 g", 190.0, 100.0, 400.0, 28.0, 15.0, 60.0),
                pres("Paquete 400 g", "400 g", 400.0, 200.0, 800.0, 60.0, 30.0, 120.0)
            )),
        InsumoV2("lechera_kg", "tenant_pionero", "RESTAURANT", "Lechera", "Bases", "g", 0.095, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Lata 385 g", "385 g", 385.0, 200.0, 500.0, 36.0, 20.0, 60.0)
            )),
        InsumoV2("media_crema", "tenant_pionero", "RESTAURANT", "Media Crema", "Bases", "g", 0.060, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Lata 220 g", "220 g", 220.0, 100.0, 400.0, 13.0, 8.0, 30.0),
                pres("Lata 360 g", "360 g", 360.0, 200.0, 500.0, 21.0, 12.0, 40.0)
            )),
        InsumoV2("leche_evaporada", "tenant_pionero", "RESTAURANT", "Leche Evaporada", "Bases", "ml", 0.080, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Lata 360 ml", "360 ml", 360.0, 200.0, 500.0, 28.0, 15.0, 50.0)
            )),
        InsumoV2("crema_batir", "tenant_pionero", "RESTAURANT", "Crema para Batir", "Bases", "ml", 0.070, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Caja 250 ml", "250 ml", 250.0, 200.0, 400.0, 17.0, 10.0, 35.0),
                pres("Caja 500 ml", "500 ml", 500.0, 300.0, 1000.0, 35.0, 20.0, 70.0)
            )),
        InsumoV2("jugo_limon", "tenant_pionero", "RESTAURANT", "Jugo de Limón", "Bases", "ml", 0.040, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 250 ml", "250 ml", 250.0, 100.0, 500.0, 10.0, 5.0, 25.0),
                pres("Botella 500 ml", "500 ml", 500.0, 200.0, 1000.0, 20.0, 10.0, 50.0)
            )),
        InsumoV2("cafe", "tenant_pionero", "RESTAURANT", "Café", "Bases", "ml", 0.060, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Frasco 200 ml", "200 ml", 200.0, 100.0, 400.0, 12.0, 6.0, 30.0),
                pres("Frasco 500 ml", "500 ml", 500.0, 200.0, 1000.0, 30.0, 15.0, 70.0)
            )),
        InsumoV2("cocoa", "tenant_pionero", "RESTAURANT", "Cocoa en Polvo", "Bases", "g", 0.080, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 16.0, 8.0, 40.0),
                pres("Bolsa 500 g", "500 g", 500.0, 250.0, 1000.0, 40.0, 20.0, 100.0)
            )),
        InsumoV2("salsa_tomate_lt", "tenant_pionero", "RESTAURANT", "Salsa de Tomate", "Salado", "ml", 0.050, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Botella 250 ml", "250 ml", 250.0, 100.0, 500.0, 12.0, 6.0, 30.0),
                pres("Botella 500 ml", "500 ml", 500.0, 200.0, 1000.0, 25.0, 12.0, 60.0)
            )),
        InsumoV2("zarzamora_kg", "tenant_pionero", "RESTAURANT", "Zarzamora", "Bases", "g", 0.030, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bolsa 500 g", "500 g", 500.0, 200.0, 1000.0, 15.0, 8.0, 40.0)
            )),
        InsumoV2("mermelada_fresa", "tenant_pionero", "RESTAURANT", "Mermelada de Fresa", "Bases", "g", 0.060, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bote 400 g", "400 g", 400.0, 200.0, 600.0, 24.0, 12.0, 50.0)
            )),
        // Aderezos
        InsumoV2("bbq", "tenant_pionero", "RESTAURANT", "Salsa BBQ", "Aderezo", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 500 g", "500 g", 500.0, 200.0, 1000.0, 25.0, 12.0, 60.0)
            )),
        InsumoV2("buffalo", "tenant_pionero", "RESTAURANT", "Salsa Buffalo", "Aderezo", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 500 g", "500 g", 500.0, 200.0, 1000.0, 25.0, 12.0, 60.0)
            )),
        InsumoV2("blue_cheese", "tenant_pionero", "RESTAURANT", "Blue Cheese", "Aderezo", "g", 0.080, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 400 g", "400 g", 400.0, 200.0, 800.0, 32.0, 15.0, 80.0)
            )),
        InsumoV2("valentina", "tenant_pionero", "RESTAURANT", "Valentina", "Aderezo", "g", 0.030, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 350 ml", "350 ml", 350.0, 200.0, 500.0, 10.0, 5.0, 25.0)
            )),
        InsumoV2("queso_amarillo", "tenant_pionero", "RESTAURANT", "Queso Amarillo", "Aderezo", "g", 0.070, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Bolsa 400 g", "400 g", 400.0, 200.0, 800.0, 28.0, 15.0, 60.0)
            )),
        InsumoV2("catsup", "tenant_pionero", "RESTAURANT", "Catsup", "Aderezo", "g", 0.030, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 500 g", "500 g", 500.0, 200.0, 1000.0, 15.0, 8.0, 40.0)
            )),
        InsumoV2("mayonesa", "tenant_pionero", "RESTAURANT", "Mayonesa", "Aderezo", "g", 0.040, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Botella 400 g", "400 g", 400.0, 200.0, 800.0, 16.0, 8.0, 40.0)
            )),
        // Salados
        InsumoV2("queso_mozzarella_kg", "tenant_pionero", "RESTAURANT", "Queso Mozzarella", "Salado", "g", 0.180, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Bolsa 500 g", "500 g", 500.0, 200.0, 1000.0, 90.0, 40.0, 200.0),
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 2000.0, 180.0, 80.0, 400.0)
            )),
        InsumoV2("jamon_kg", "tenant_pionero", "RESTAURANT", "Jamón de Pierna", "Salado", "g", 0.120, 1000.0, 100.0,
            presentacionesCompra = listOf(
                pres("Paquete 200 g", "200 g", 200.0, 100.0, 500.0, 24.0, 12.0, 60.0),
                pres("Paquete 500 g", "500 g", 500.0, 200.0, 1000.0, 60.0, 30.0, 150.0)
            )),
        InsumoV2("peperoni_kg", "tenant_pionero", "RESTAURANT", "Peperoni", "Salado", "g", 0.240, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Paquete 250 g", "250 g", 250.0, 100.0, 500.0, 60.0, 30.0, 140.0),
                pres("Paquete 500 g", "500 g", 500.0, 200.0, 1000.0, 120.0, 60.0, 280.0)
            )),
        InsumoV2("chorizo_kg", "tenant_pionero", "RESTAURANT", "Chorizo", "Salado", "g", 0.115, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Paquete 250 g", "250 g", 250.0, 100.0, 500.0, 28.0, 15.0, 60.0),
                pres("Paquete 500 g", "500 g", 500.0, 200.0, 1000.0, 57.0, 30.0, 140.0)
            )),
        InsumoV2("pina_kg", "tenant_pionero", "RESTAURANT", "Piña", "Salado", "g", 0.040, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Pieza entera ~1 kg", "~1 kg", 1000.0, 500.0, 2000.0, 40.0, 20.0, 100.0),
                pres("Bolsa 500 g (precortada)", "500 g", 500.0, 200.0, 1000.0, 20.0, 10.0, 50.0)
            )),
        // Toppings
        InsumoV2("fresas", "tenant_pionero", "RESTAURANT", "Fresas", "Topping", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Domo 496 g", "Domo 496 g", 496.0, 200.0, 600.0, 25.0, 15.0, 60.0),
                pres("Granel por kg", "1 kg", 1000.0, 250.0, 5000.0, 50.0, 20.0, 150.0),
                pres("1/2 kg", "500 g", 500.0, 400.0, 600.0, 25.0, 10.0, 80.0),
                pres("1/4 kg", "250 g", 250.0, 200.0, 300.0, 12.0, 5.0, 40.0)
            )),
        InsumoV2("durazno_kg", "tenant_pionero", "RESTAURANT", "Duraznos", "Topping", "g", 0.050, 500.0, 50.0,
            presentacionesCompra = listOf(
                pres("Lata 450 g", "450 g", 450.0, 200.0, 600.0, 22.0, 12.0, 50.0),
                pres("Granel por kg", "1 kg", 1000.0, 250.0, 5000.0, 50.0, 20.0, 150.0)
            )),
        InsumoV2("coco_rayado", "tenant_pionero", "RESTAURANT", "Coco Rayado", "Topping", "g", 0.080, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 100 g", "100 g", 100.0, 50.0, 200.0, 8.0, 4.0, 20.0),
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 16.0, 8.0, 40.0)
            )),
        InsumoV2("granillo_chocolate", "tenant_pionero", "RESTAURANT", "Granillo Chocolate", "Topping", "g", 0.100, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 20.0, 10.0, 50.0)
            )),
        InsumoV2("granillo_colores", "tenant_pionero", "RESTAURANT", "Granillo Colores", "Topping", "g", 0.080, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 16.0, 8.0, 40.0)
            )),
        InsumoV2("bombon_kg", "tenant_pionero", "RESTAURANT", "Bombón", "Topping", "g", 0.150, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 200 g", "200 g", 200.0, 100.0, 400.0, 30.0, 15.0, 80.0)
            )),
        InsumoV2("nuez_kg", "tenant_pionero", "RESTAURANT", "Nuez", "Topping", "g", 0.200, 300.0, 30.0,
            presentacionesCompra = listOf(
                pres("Bolsa 150 g", "150 g", 150.0, 50.0, 300.0, 30.0, 15.0, 80.0),
                pres("Bolsa 300 g", "300 g", 300.0, 150.0, 500.0, 60.0, 30.0, 150.0)
            )),
        // Snacks
        InsumoV2("boneless", "tenant_pionero", "RESTAURANT", "Boneless", "Snacks", "g", 0.080, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa 2.6kg", "kg", 2600.0, 10.0, 208.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 2.6 kg", "2.6 kg", 2600.0, 1500.0, 5000.0, 208.0, 100.0, 400.0),
                pres("Porción 250 g", "250 g", 250.0, 200.0, 300.0, 20.0, 10.0, 40.0)
            )),
        InsumoV2("nuggets", "tenant_pionero", "RESTAURANT", "Nuggets", "Snacks", "g", 0.070, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa 2.6kg", "kg", 2600.0, 10.0, 182.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 2.6 kg", "2.6 kg", 2600.0, 1500.0, 5000.0, 182.0, 100.0, 400.0),
                pres("Porción 200 g", "~7-8 pz", 200.0, 150.0, 250.0, 14.0, 8.0, 30.0)
            )),
        InsumoV2("papas", "tenant_pionero", "RESTAURANT", "Papas", "Snacks", "g", 0.040, 5000.0, 500.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa 2.3kg", "kg", 2300.0, 10.0, 92.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 2.3 kg", "2.3 kg", 2300.0, 1500.0, 5000.0, 92.0, 50.0, 200.0),
                pres("Bolsa 1 kg", "1 kg", 1000.0, 500.0, 2000.0, 40.0, 20.0, 100.0)
            )),
        // Galletas
        InsumoV2("oreo", "tenant_pionero", "RESTAURANT", "Galleta Oreo", "Topping", "pz", 0.50, 294.0, 28.0,
            presentaciones = listOf(
                PresentacionInsumo("Caja (21 paquetes)", "box", 294.0, 5.0, 147.0),
                PresentacionInsumo("Paquete (14 pz)", "pkg", 14.0, 50.0, 7.0)),
            presentacionesCompra = listOf(
                pres("Caja", "21 paquetes × 14 galletas", 294.0, 200.0, 400.0, 147.0, 80.0, 250.0),
                pres("Paquete individual 14", "14 galletas", 14.0, 10.0, 16.0, 7.0, 4.0, 20.0),
                pres("Paquete individual 4", "4 galletas", 4.0, 4.0, 6.0, 2.0, 1.0, 8.0)
            )),
        InsumoV2("galletas_maria", "tenant_pionero", "RESTAURANT", "Galletas María", "Bases", "pz", 0.035, 200.0, 40.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (40 pz)", "pkg", 40.0, 20.0, 15.0)),
            presentacionesCompra = listOf(
                pres("Paquete 3 unid", "3 × 170 g ~35-40 pz c/u", 120.0, 80.0, 160.0, 15.0, 8.0, 30.0),
                pres("Paquete individual", "170 g ~35-40 pz", 40.0, 30.0, 50.0, 5.0, 3.0, 12.0)
            )),
        InsumoV2("galletas_mexicana", "tenant_pionero", "RESTAURANT", "Galletas Mexicanas", "Bases", "pz", 0.025, 180.0, 60.0,
            presentaciones = listOf(PresentacionInsumo("Paquete 5u (60 pz)", "pkg", 60.0, 15.0, 45.0)),
            presentacionesCompra = listOf(
                pres("Paquete 5 unid", "5 × 135 g ~12-15 pz c/u", 75.0, 50.0, 100.0, 45.0, 25.0, 80.0),
                pres("Paquete individual", "135 g ~12-15 pz", 15.0, 10.0, 20.0, 9.0, 5.0, 20.0)
            )),
        // Producción
        InsumoV2("masa_crepa", "tenant_pionero", "RESTAURANT", "Masa de Crepa Preparada", "Producción", "pz", 5.0, 60.0, 12.0,
            presentacionesCompra = listOf(
                pres("Tanda (60 crepas)", "60 piezas", 60.0, 30.0, 120.0, 300.0, 150.0, 600.0)
            )),
        InsumoV2("carlota_unidad", "tenant_pionero", "RESTAURANT", "Carlota de Limón", "Producción", "pz", 15.0, 0.0, 4.0),
        InsumoV2("tiramisu_unidad", "tenant_pionero", "RESTAURANT", "Tiramisú", "Producción", "pz", 22.0, 0.0, 4.0),
        InsumoV2("fresas_crema_unidad", "tenant_pionero", "RESTAURANT", "Fresas con Crema", "Producción", "pz", 18.0, 0.0, 4.0),
        InsumoV2("duraznos_crema_unidad", "tenant_pionero", "RESTAURANT", "Duraznos con Crema", "Producción", "pz", 18.0, 0.0, 4.0),
        // Consumibles
        InsumoV2("charola", "tenant_pionero", "RESTAURANT", "Charola Unicel", "Consumible", "pz", 1.60, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Bolsa (50 pz)", "pz", 1.0, 200.0, 80.0)),
            presentacionesCompra = listOf(
                pres("Bolsa 50 pz", "50 piezas", 50.0, 25.0, 100.0, 80.0, 40.0, 150.0)
            )),
        InsumoV2("tenedor", "tenant_pionero", "RESTAURANT", "Tenedor Plástico", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            )),
        InsumoV2("servilletas", "tenant_pionero", "RESTAURANT", "Servilletas", "Consumible", "pz", 0.25, 600.0, 100.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (200 pz)", "pz", 1.0, 600.0, 45.0)),
            presentacionesCompra = listOf(
                pres("Paquete 200 pz", "200 piezas", 200.0, 100.0, 400.0, 45.0, 25.0, 80.0)
            )),
        InsumoV2("vaso", "tenant_pionero", "RESTAURANT", "Vaso 16oz", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            )),
        InsumoV2("domo", "tenant_pionero", "RESTAURANT", "Domo", "Consumible", "pz", 0.50, 200.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (50 pz)", "pz", 1.0, 200.0, 25.0)),
            presentacionesCompra = listOf(
                pres("Paquete 50 pz", "50 piezas", 50.0, 25.0, 100.0, 25.0, 12.0, 50.0)
            )),
        InsumoV2("papel_hamburguesero", "tenant_pionero", "RESTAURANT", "Papel Hamburguesero", "Consumible", "pz", 0.30, 300.0, 50.0,
            presentaciones = listOf(PresentacionInsumo("Paquete (100 pz)", "pz", 1.0, 300.0, 30.0)),
            presentacionesCompra = listOf(
                pres("Paquete 100 pz", "100 piezas", 100.0, 50.0, 200.0, 30.0, 15.0, 60.0)
            )),
        InsumoV2("cuchara", "tenant_pionero", "RESTAURANT", "Cuchara", "Consumible", "pz", 0.50, 200.0, 50.0,
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
            SalesInventoryProductV2("cr_dulce", "tenant_pionero", "RESTAURANT", "Crepa Individual Dulce", "🥞", "CREPAS_DULCES",
                mapOf("atlixco" to 25.0, "metepec" to 25.0), recetaId = "receta_cr_dulce",
                consumiblesAsociados = consumiblesCrepa) to
            RecetaV2("receta_cr_dulce", "Crepa Dulce Individual", "cr_dulce", listOf(
                ing("masa_crepa", "Masa de Crepa", 1.0, "pz"),
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz"),
                ing("papel_hamburguesero", "Papel", 1.0, "pz")
            )),

            SalesInventoryProductV2("cr_salada", "tenant_pionero", "RESTAURANT", "Crepa Individual Salada", "🥪", "CREPAS_SALADAS",
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
            SalesInventoryProductV2("combo_2d", "tenant_pionero", "RESTAURANT", "Combo 2 Crepas Dulces", "🥞🥞", "Combos",
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

            SalesInventoryProductV2("combo_2s", "tenant_pionero", "RESTAURANT", "Combo 2 Crepas Saladas", "🥪🥪", "Combos",
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

            SalesInventoryProductV2("combo_duo", "tenant_pionero", "RESTAURANT", "Combo Dulce y Salada", "🥞🥪", "Combos",
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
            SalesInventoryProductV2("p_carlota", "tenant_pionero", "RESTAURANT", "Carlota de Limón", "🍰", "POSTRES",
                mapOf("atlixco" to 40.0, "metepec" to 40.0), recetaId = "receta_p_carlota",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_carlota", "Carlota de Limón Porción", "p_carlota", listOf(
                ing("carlota_unidad", "Carlota de Limón", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            SalesInventoryProductV2("p_tiramisu", "tenant_pionero", "RESTAURANT", "Tiramisú", "☕", "POSTRES",
                mapOf("atlixco" to 45.0, "metepec" to 45.0), recetaId = "receta_p_tiramisu",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_tiramisu", "Tiramisú Porción", "p_tiramisu", listOf(
                ing("tiramisu_unidad", "Tiramisú", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            SalesInventoryProductV2("p_fresas", "tenant_pionero", "RESTAURANT", "Fresas con Crema", "🍓", "POSTRES",
                mapOf("atlixco" to 50.0, "metepec" to 50.0), recetaId = "receta_p_fresas",
                consumiblesAsociados = consumiblesPostre) to
            RecetaV2("receta_p_fresas", "Fresas con Crema Porción", "p_fresas", listOf(
                ing("fresas_crema_unidad", "Fresas con Crema", 1.0, "pz"),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz"),
                ing("cuchara", "Cuchara", 1.0, "pz"),
                ing("servilletas", "Servilleta", 1.0, "pz")
            )),

            SalesInventoryProductV2("p_duraznos", "tenant_pionero", "RESTAURANT", "Duraznos con Crema", "🍑", "POSTRES",
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
            SalesInventoryProductV2("s_papas_senc", "tenant_pionero", "RESTAURANT", "Papas Fritas", "🍟", "SNACKS",
                mapOf("atlixco" to 35.0, "metepec" to 35.0), recetaId = "receta_papas_sencillas",
                consumiblesAsociados = listOf(consumible("charola"), consumible("tenedor"), consumible("servilletas"))) to
            RecetaV2("receta_papas_sencillas", "Papas Sencillas", "s_papas_senc", listOf(
                ing("papas", "Papas", 200.0),
                ing("charola", "Charola Unicel", 1.0, "pz"),
                ing("tenedor", "Tenedor", 1.0, "pz"),
                ing("servilletas", "Servilletas", 1.0, "pz")
            )),

            SalesInventoryProductV2("s_papas_chor", "tenant_pionero", "RESTAURANT", "Papas con Chorizo", "🍟", "SNACKS",
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

            SalesInventoryProductV2("s_boneless", "tenant_pionero", "RESTAURANT", "Boneless", "🍗", "SNACKS",
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

            SalesInventoryProductV2("s_nuggets", "tenant_pionero", "RESTAURANT", "Nuggets", "🍗", "SNACKS",
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
            SalesInventoryProductV2("frappe_oreo", "tenant_pionero", "RESTAURANT", "Frappe Oreo", "🥤", "BEBIDAS",
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

            SalesInventoryProductV2("frappe_cocoa", "tenant_pionero", "RESTAURANT", "Frappe Cocoa", "🥤", "BEBIDAS",
                mapOf("atlixco" to 30.0, "metepec" to 30.0), recetaId = "receta_frappe_cocoa",
                consumiblesAsociados = listOf(consumible("vaso"), consumible("domo"))) to
            RecetaV2("receta_frappe_cocoa", "Frappe Cocoa", "frappe_cocoa", listOf(
                ing("nutella_kg", "Nutella", 20.0),
                ing("cocoa", "Cocoa", 40.0),
                ing("hielo", "Hielo", 150.0),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz")
            )),

            SalesInventoryProductV2("frappe_fresa", "tenant_pionero", "RESTAURANT", "Frappe Fresa", "🥤", "BEBIDAS",
                mapOf("atlixco" to 30.0, "metepec" to 30.0), recetaId = "receta_frappe_fresa",
                consumiblesAsociados = listOf(consumible("vaso"), consumible("domo"))) to
            RecetaV2("receta_frappe_fresa", "Frappe Fresa", "frappe_fresa", listOf(
                ing("nutella_kg", "Nutella", 20.0),
                ing("fresas", "Fresas", 50.0),
                ing("hielo", "Hielo", 150.0),
                ing("vaso", "Vaso", 1.0, "pz"),
                ing("domo", "Domo", 1.0, "pz")
            )),

            SalesInventoryProductV2("frappe_fresa_cocoa", "tenant_pionero", "RESTAURANT", "Frappe Fresa Cocoa", "🥤", "BEBIDAS",
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

