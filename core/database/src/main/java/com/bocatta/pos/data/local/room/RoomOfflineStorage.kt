package com.bocatta.pos.data.local.room

import com.bocatta.pos.data.local.room.dao.*
import com.bocatta.pos.data.local.room.entity.*
import com.bocatta.pos.domain.model.ConsumibleV2
import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.domain.model.OperacionOffline
import com.bocatta.pos.domain.model.RecetaV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.TurnoContingenciaLocal
import com.bocatta.pos.domain.model.VentaOffline
import com.bocatta.pos.domain.storage.OfflineStorage
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Legacy compatible storage implementation that delegates to Room DAOs.
 * All public methods match the historic `OfflineDatabase` API while internally
 * operating on Room entities. Conversions are kept straightforward to avoid
 * side‑effects; any business logic (e.g. stock reservation) should happen
 * before calling these methods.
 */
class RoomOfflineStorage(
    private val db: BocattaOfflineDatabase,
    private val ventaDao: VentaPendienteDao = db.ventaPendienteDao(),
    private val operacionDao: OperacionPendienteDao = db.operacionPendienteDao(),
    private val folioDao: FolioDao = db.folioDao(),
    private val turnoDao: TurnoContingenciaDao = db.turnoContingenciaDao(),
    private val productoDao: ProductoDao = db.productoDao()
) : OfflineStorage {

    // ---------------------------------------------------------------------
    // Helper conversion functions
    // ---------------------------------------------------------------------
    private fun VentaOffline.toEntity() = VentaPendienteEntity(
        id = id,
        tenantId = tenantId,
        ticket = ticket,
        codigoTicket = codigoTicket,
        total = total,
        descuentoLealtad = descuentoLealtad,
        descuentoPromociones = descuentoPromociones,
        descuentoManual = descuentoManual,
        propina = propina,
        notaOrden = notaOrden,
        fecha = fecha,
        sucursal = sucursal,
        atendio = atendio,
        metodoPago = metodoPago,
        esConsumoEmpleado = esConsumoEmpleado,
        clienteId = clienteId,
        carritoJson = carritoJson,
        estado = estado,
        intentos = intentos,
        ultimoIntento = ultimoIntento
    )

    private fun VentaPendienteEntity.toDomain() = VentaOffline(
        id = id,
        tenantId = tenantId,
        ticket = ticket,
        codigoTicket = codigoTicket,
        total = total,
        descuentoLealtad = descuentoLealtad,
        descuentoPromociones = descuentoPromociones,
        descuentoManual = descuentoManual,
        propina = propina,
        notaOrden = notaOrden,
        fecha = fecha,
        sucursal = sucursal,
        atendio = atendio,
        metodoPago = metodoPago,
        esConsumoEmpleado = esConsumoEmpleado,
        clienteId = clienteId,
        carritoJson = carritoJson,
        estado = estado,
        intentos = intentos,
        ultimoIntento = ultimoIntento
    )

    private fun OperacionOffline.toEntity() = OperacionPendienteEntity(
        id = id,
        tenantId = tenantId,
        tipo = tipo,
        ventaId = ventaId,
        motivo = motivo,
        usuarioId = usuarioId,
        sucursal = sucursal,
        fecha = fecha,
        requiereAprobacion = requiereAprobacion,
        dataJson = dataJson,
        estado = estado,
        intentos = intentos
    )

    private fun OperacionPendienteEntity.toDomain() = OperacionOffline(
        id = id,
        tenantId = tenantId,
        tipo = tipo,
        ventaId = ventaId,
        motivo = motivo,
        usuarioId = usuarioId,
        sucursal = sucursal,
        fecha = fecha,
        requiereAprobacion = requiereAprobacion,
        dataJson = dataJson,
        estado = estado,
        intentos = intentos
    )

    private fun TurnoContingenciaLocal.toEntity() = TurnoContingenciaEntity(
        id = id,
        tenantId = tenantId,
        sucursal = sucursal,
        usuarioId = usuarioId,
        usuarioNombre = usuarioNombre,
        rol = rol,
        fondoInicial = fondoInicial,
        fechaApertura = fechaApertura,
        fechaCierre = fechaCierre,
        estado = estado,
        efectivoContado = efectivoContado,
        tarjetaContada = tarjetaContada,
        syncPendiente = if (syncPendiente) 1 else 0
    )

    private fun TurnoContingenciaEntity.toDomain() = TurnoContingenciaLocal(
        id = id,
        tenantId = tenantId,
        sucursal = sucursal,
        usuarioId = usuarioId,
        usuarioNombre = usuarioNombre,
        rol = rol,
        fondoInicial = fondoInicial,
        fechaApertura = fechaApertura,
        fechaCierre = fechaCierre,
        estado = estado,
        efectivoContado = efectivoContado,
        tarjetaContada = tarjetaContada,
        syncPendiente = syncPendiente == 1
    )

    private fun InsumoV2.toEntity() = InsumoV2Entity(
        id = id,
        nombre = nombre,
        categoria = categoria.takeIf { it.isNotBlank() },
        unidadBase = unidadBase,
        costoUnitarioBase = costoUnitarioBase,
        cantidadEnBase = cantidadEnBase,
        stockMinimo = stockMinimo
    )

    private fun InsumoV2Entity.toDomain() = InsumoV2(
        id = id,
        tenantId = "",
        businessType = "RESTAURANT",
        nombre = nombre,
        categoria = categoria ?: "",
        unidadBase = unidadBase,
        costoUnitarioBase = costoUnitarioBase,
        cantidadEnBase = cantidadEnBase,
        stockMinimo = stockMinimo
    )

    private fun ConsumibleV2.toEntity() = ConsumibleV2Entity(
        id = id,
        nombre = nombre,
        unidadBase = unidadBase,
        stockActual = stockActual,
        stockMinimo = stockMinimo
    )

    private fun ConsumibleV2Entity.toDomain() = ConsumibleV2(
        id = id,
        nombre = nombre,
        unidadBase = unidadBase,
        stockActual = stockActual,
        stockMinimo = stockMinimo
    )

    private fun SalesInventoryProductV2.toEntity() = ProductoV2Entity(
        id = id,
        tenantId = tenantId,
        businessType = businessType,
        nombre = nombre,
        emoji = emoji,
        categoria = categoria.takeIf { it.isNotBlank() },
        precioVenta = if (precioVenta.isNotEmpty()) {
            JSONObject().apply { precioVenta.forEach { (k, v) -> put(k, v) } }.toString()
        } else null,
        esCombo = if (esCombo) 1 else 0,
        recetaId = recetaId,
        toppingsIncluidos = toppingsIncluidos,
        costoToppingExtra = costoToppingExtra,
        esProductoTopping = if (esProductoTopping) 1 else 0,
        consumiblesJson = if (consumiblesAsociados.isNotEmpty()) {
            JSONObject().apply {
                put("items", consumiblesAsociados.map {
                    JSONObject().apply {
                        put("consumibleId", it.consumibleId)
                        put("cantidad", it.cantidad)
                        put("unidad", it.unidad)
                    }
                })
            }.toString()
        } else null,
        requiresStock = if (requiresStock) 1 else 0,
        hasVariants = if (hasVariants) 1 else 0,
        barcode = barcode.takeIf { it.isNotBlank() },
        activo = if (activo) 1 else 0
    )

    private fun ProductoV2Entity.toDomain() = SalesInventoryProductV2(
        id = id,
        tenantId = tenantId,
        businessType = businessType,
        nombre = nombre,
        emoji = emoji ?: "\uD83C\uDF7D",
        categoria = categoria ?: "",
        precioVenta = precioVenta?.let {
            runCatching {
                val obj = JSONObject(it)
                obj.keys().asSequence().associateWith { key -> obj.getDouble(key) }
            }.getOrDefault(emptyMap())
        } ?: emptyMap(),
        esCombo = esCombo == 1,
        recetaId = recetaId,
        toppingsIncluidos = toppingsIncluidos,
        costoToppingExtra = costoToppingExtra,
        esProductoTopping = esProductoTopping == 1,
        activo = activo == 1,
        requiresStock = requiresStock == 1,
        hasVariants = hasVariants == 1,
        barcode = barcode ?: ""
    )

    private fun RecetaV2.toEntity() = RecetaV2Entity(
        id = id,
        nombre = nombre,
        productoId = productoId,
        rendimientoPorcion = rendimientoPorcion
    )

    private fun RecetaV2Entity.toDomain(ingredientes: List<IngredienteReceta>) = RecetaV2(
        id = id,
        nombre = nombre,
        productoId = productoId,
        rendimientoPorcion = rendimientoPorcion,
        ingredientes = ingredientes
    )

    private fun IngredienteReceta.toEntity(recetaId: String) = IngredienteRecetaEntity(
        id = "$recetaId:$insumoId",
        recetaId = recetaId,
        insumoId = insumoId,
        nombreInsumo = nombreInsumo.takeIf { it.isNotBlank() },
        cantidad = cantidad,
        unidad = unidad
    )

    private fun IngredienteRecetaEntity.toDomain() = IngredienteReceta(
        insumoId = insumoId,
        nombreInsumo = nombreInsumo ?: "",
        cantidad = cantidad,
        unidad = unidad
    )

    // ---------------------------------------------------------------------
    // OfflineStorage interface implementation (2 methods)
    // ---------------------------------------------------------------------
    override fun guardarVentaYDescontarStockReservandoFolio(
        ventaBase: VentaOffline,
        deducciones: Map<String, Double>,
        legacyUltimoTicket: Long
    ): VentaOffline {
        val entity = ventaBase.copy(ticket = if (legacyUltimoTicket != 0L) legacyUltimoTicket else ventaBase.ticket)
            .toEntity()
        runBlocking { ventaDao.insertar(entity) }
        return ventaBase
    }

    override fun guardarOperacion(op: OperacionOffline) {
        runBlocking { operacionDao.insertar(op.toEntity()) }
    }

    // ---------------------------------------------------------------------
    // Legacy API methods – each delegates to the appropriate DAO
    // ---------------------------------------------------------------------
    // Ventas -------------------------------------------------------------
    fun guardarVenta(venta: VentaOffline) {
        runBlocking { ventaDao.insertar(venta.toEntity()) }
    }

    fun obtenerVentasPendientes(): List<VentaOffline> {
        return runBlocking { ventaDao.obtenerPendientes().map { it.toDomain() } }
    }

    fun marcarVentaSincronizada(id: String, ts: Long) {
        runBlocking { ventaDao.marcarSincronizada(id, ts) }
    }

    fun marcarVentaFallida(id: String, ts: Long) {
        runBlocking { ventaDao.marcarFallida(id, ts) }
    }

    fun marcarVentaFallidaCritica(id: String, ts: Long) {
        runBlocking { ventaDao.marcarFallidaCritica(id, ts) }
    }

    fun registrarIntentoVentaFallido(id: String, ts: Long) {
        runBlocking { ventaDao.registrarIntentoFallido(id, ts) }
    }

    fun reintentarVenta(id: String) {
        runBlocking { ventaDao.reintentarVenta(id) }
    }

    fun obtenerVentasFallidas(): List<VentaOffline> {
        return runBlocking { ventaDao.obtenerFallidas().map { it.toDomain() } }
    }

    fun contarPendientes(): Int = runBlocking { ventaDao.contarPendientes() }

    fun contarVentasFallidas(): Int = runBlocking {
        ventaDao.obtenerFallidas().size
    }

    fun limpiarVentasSincronizadas() {
        runBlocking { ventaDao.limpiarSincronizadas() }
    }

    fun obtenerVentasLocalesDesde(sucursal: String, desde: Long): List<VentaOffline> {
        return runBlocking { ventaDao.obtenerDesde(sucursal, desde).map { it.toDomain() } }
    }

    // Operaciones --------------------------------------------------------
    fun obtenerOperacionesPendientes(): List<OperacionOffline> {
        return runBlocking { operacionDao.obtenerPendientes().map { it.toDomain() } }
    }

    fun marcarOperacionSincronizada(id: String) {
        runBlocking { operacionDao.marcarSincronizada(id) }
    }

    fun registrarIntentoOperacionFallido(id: String) {
        runBlocking { operacionDao.registrarIntentoFallido(id) }
    }

    fun marcarOperacionFallida(id: String) {
        runBlocking { operacionDao.marcarFallida(id) }
    }

    fun limpiarOperacionesSincronizadas() {
        runBlocking { operacionDao.limpiarSincronizadas() }
    }

    // Turnos contingencia ------------------------------------------------
    fun guardarTurnoContingencia(turno: TurnoContingenciaLocal) {
        runBlocking { turnoDao.insert(turno.toEntity()) }
    }

    fun obtenerTurnoContingenciaAbierto(sucursal: String): TurnoContingenciaLocal? {
        return runBlocking { turnoDao.getTurnoAbierto(sucursal)?.toDomain() }
    }

    fun cerrarTurnoContingencia(id: String, estado: String, fechaCierre: Long) {
        runBlocking { turnoDao.cerrarTurno(id, estado, fechaCierre) }
    }

    fun obtenerTurnosContingenciaPendientesSync(): List<TurnoContingenciaLocal> {
        return runBlocking { turnoDao.getPendingSync().map { it.toDomain() } }
    }

    // Insumos ------------------------------------------------------------
    fun guardarInsumo(insumo: InsumoV2) {
        runBlocking { productoDao.insertInsumo(insumo.toEntity()) }
    }

    fun obtenerInsumos(): List<InsumoV2> {
        return runBlocking { productoDao.getAllInsumos().map { it.toDomain() } }
    }

    fun obtenerInsumoPorId(id: String): InsumoV2? {
        return runBlocking { productoDao.getInsumoById(id)?.toDomain() }
    }

    // Consumibles --------------------------------------------------------
    fun guardarConsumible(consumible: ConsumibleV2) {
        runBlocking { productoDao.insertConsumible(consumible.toEntity()) }
    }

    fun obtenerConsumibles(): List<ConsumibleV2> {
        return runBlocking { productoDao.getAllConsumibles().map { it.toDomain() } }
    }

    // Productos ----------------------------------------------------------
    fun guardarProducto(producto: SalesInventoryProductV2) {
        runBlocking { productoDao.insertProducto(producto.toEntity()) }
    }

    fun obtenerProductoPorId(id: String): SalesInventoryProductV2? {
        return runBlocking { productoDao.getProductoById(id)?.toDomain() }
    }

    fun obtenerTodosLosProductos(): List<SalesInventoryProductV2> {
        return runBlocking { productoDao.getAllProductos().map { it.toDomain() } }
    }

    // Recetas ------------------------------------------------------------
    fun guardarReceta(receta: RecetaV2) {
        runBlocking {
            productoDao.insertReceta(receta.toEntity())
        }
    }

    fun guardarRecetaConIngredientes(receta: RecetaV2) {
        runBlocking {
            productoDao.insertReceta(receta.toEntity())
            receta.ingredientes.forEach { ingrediente ->
                productoDao.insertIngredienteReceta(ingrediente.toEntity(receta.id))
            }
        }
    }

    fun guardarIngredienteReceta(ingrediente: IngredienteReceta, recetaId: String) {
        runBlocking { productoDao.insertIngredienteReceta(ingrediente.toEntity(recetaId)) }
    }

    fun obtenerRecetaPorId(id: String): RecetaV2? {
        return runBlocking {
            val recetaEntity = productoDao.getRecetaById(id) ?: return@runBlocking null
            val ingredientes = productoDao.getIngredientesByRecetaId(id).map { it.toDomain() }
            recetaEntity.toDomain(ingredientes)
        }
    }

    fun obtenerTodasLasRecetas(): List<RecetaV2> {
        return runBlocking {
            productoDao.getAllRecetas().map { recetaEntity ->
                val ingredientes = productoDao.getIngredientesByRecetaId(recetaEntity.id).map { it.toDomain() }
                recetaEntity.toDomain(ingredientes)
            }
        }
    }

    // Folios -------------------------------------------------------------
    fun guardarFolio(folio: FolioEntity) {
        runBlocking { folioDao.guardarFolio(folio) }
    }

    fun obtenerUltimoTicket(sucursalId: String): Long? {
        return runBlocking { folioDao.obtenerUltimoTicket(sucursalId) }
    }

    fun obtenerFolio(sucursalId: String): FolioEntity? {
        return runBlocking { folioDao.obtenerFolio(sucursalId) }
    }
}
