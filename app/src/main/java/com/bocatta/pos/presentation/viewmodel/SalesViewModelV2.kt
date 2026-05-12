package com.bocatta.pos.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.di.SalesDependencies
import com.bocatta.pos.network.NetworkStateProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.repository.SalesRepository
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.data.sync.SyncScheduler
import com.bocatta.pos.core.TicketUtils
import com.bocatta.pos.data.repository.InventoryDeductions
import com.bocatta.pos.data.repository.PromocionesRepository
import timber.log.Timber
import com.google.firebase.firestore.ListenerRegistration
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.domain.usecase.SalesFlowUseCase
import com.bocatta.pos.domain.usecase.SaleItemInput
import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase
import com.bocatta.pos.domain.usecase.PromocionesEngine
import com.bocatta.pos.core.constants.SucursalConfig

class SalesViewModelV2(
   application: Application,
   deps: SalesDependencies,
   private val networkStateProvider: NetworkStateProvider
) : BaseAndroidViewModel(application) {
   private val db = FirebaseFirestoreProvider.db
   private val productRepo: IProductRepository = deps.productRepo
   private val inventoryRepo: IInventoryRepository = deps.inventoryRepo
   private val salesFlowUseCase: SalesFlowUseCase = deps.salesFlowUseCase
   private val repository: SalesRepository = deps.repository
   private val generarTicketWhatsAppUseCase: GenerarTicketWhatsAppUseCase = deps.generarTicketWhatsAppUseCase
   private val promocionesEngine: PromocionesEngine = deps.promocionesEngine
   private val promocionesRepository: PromocionesRepository = deps.promocionesRepository

   var isOnline by mutableStateOf(true)
      private set

   private var _productos = mutableStateListOf<SalesInventoryProductV2>()
   val productos: List<SalesInventoryProductV2> get() = _productos
   private val _carrito = mutableStateListOf<ItemCarritoV2>()
   val carrito: List<ItemCarritoV2> get() = _carrito
   private var _clienteSeleccionado by mutableStateOf<ClienteV2?>(null)
   val clienteSeleccionado: ClienteV2? get() = _clienteSeleccionado
   private var _descuentoLealtad by mutableStateOf(0.0)
   val descuentoLealtad: Double get() = _descuentoLealtad
   var ultimoTicketAsignado by mutableStateOf(0L)
      private set
   var ultimoCodigoTicket by mutableStateOf("")
      private set
   var ultimoTicketTexto by mutableStateOf<String?>(null)
      private set
   private var _mensajeFeedback by mutableStateOf<String?>(null)
   var mensajeFeedback: String?
      get() = _mensajeFeedback
      set(value) { _mensajeFeedback = value }
   private var _metodoPagoSeleccionado by mutableStateOf(MetodoPago.EFECTIVO)
   var metodoPagoSeleccionado: MetodoPago
      get() = _metodoPagoSeleccionado
      set(value) { _metodoPagoSeleccionado = value }
   private var _esConsumoEmpleado by mutableStateOf(false)
   var esConsumoEmpleado: Boolean
      get() = _esConsumoEmpleado
      set(value) { _esConsumoEmpleado = value }
   private var _rolUsuario by mutableStateOf(Rol.VENDEDOR)
   var rolUsuario: Rol
      get() = _rolUsuario
      set(value) { _rolUsuario = value }
   private val _alertasStock = mutableStateMapOf<String, Double>()
   val alertasStock: Map<String, Double> get() = _alertasStock
   private var _stockInsuficiente by mutableStateOf(false)
   val stockInsuficiente: Boolean get() = _stockInsuficiente

   private var _mostrarConfirmacionVenta by mutableStateOf(false)
   var mostrarConfirmacionVenta: Boolean
      get() = _mostrarConfirmacionVenta
      set(value) { _mostrarConfirmacionVenta = value }
   private val _clientesSugeridos = mutableStateListOf<ClienteV2>()
   val clientesSugeridos: List<ClienteV2> get() = _clientesSugeridos

   private val _promocionesActivas = mutableStateListOf<PromocionUniversal>()
   val promocionesActivas: List<PromocionUniversal> get() = _promocionesActivas

   val descuentoPromociones by derivedStateOf {
      promocionesEngine.calcular(_carrito, _promocionesActivas)
   }

   private var menuListener: ListenerRegistration? = null
   private var stockListener: ListenerRegistration? = null

   val totalCarrito by derivedStateOf {
      _carrito.fold(BigDecimal.ZERO) { acc, item ->
         acc.add(item.precioFinal.multiply(BigDecimal(item.cantidad)))
      }
   }

   fun configurar(sucursal: String, usuarioNombre: String, rol: Rol) {
      _rolUsuario = rol
      escucharMenu()
      escucharStock()
      obtenerPromociones()
   }

   private fun obtenerPromociones() {
      viewModelScope.launch {
         val promos = promocionesRepository.getPromocionesActivas()
         _promocionesActivas.clear()
         _promocionesActivas.addAll(promos)
      }
   }

   private fun escucharMenu() {
      menuListener?.remove()
      menuListener = db.collection(FirestoreCollections.PRODUCTOS).addSnapshotListener { snap, _ ->
         if (snap != null) {
            _productos.clear()
            snap.documents.forEach { doc ->
               doc.toObject(SalesInventoryProductV2::class.java)?.let { _productos.add(it.copy(id = doc.id)) }
            }
         }
      }
   }

   private fun escucharStock() {
      stockListener?.remove()
      stockListener = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).addSnapshotListener { snap, _ ->
         if (snap != null) {
            _alertasStock.clear()
            snap.documents.forEach { doc ->
               val cant = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
               val id = doc.getString("insumoId") ?: SucursalConfig.extraerInsumoIdDeDocId(doc.id)
               _alertasStock[id] = cant
            }
         }
      }
   }

   override fun onCleared() {
      super.onCleared()
      menuListener?.remove()
      stockListener?.remove()
   }

   fun buscarCliente(query: String) {
      viewModelScope.launch(safeHandler) {
         val snap = db.collection(FirestoreCollections.CLIENTES)
            .whereGreaterThanOrEqualTo("nombre", query)
            .whereLessThanOrEqualTo("nombre", query + "\uf8ff")
            .limit(5).get().await()
         _clientesSugeridos.clear()
         snap.documents.forEach { doc -> doc.toObject(ClienteV2::class.java)?.let { _clientesSugeridos.add(it.copy(idDocumento = doc.id)) } }
      }
   }

   fun seleccionarCliente(cliente: ClienteV2) {
      _clienteSeleccionado = cliente
      _descuentoLealtad = if (cliente.visitasCicloActual == FirestoreCollections.MEMBRESIA_CICLO_VISITAS) {
         cliente.comprasCicloActual.average()
      } else 0.0
   }

   fun eliminarCliente() {
      _clienteSeleccionado = null
      _descuentoLealtad = 0.0
   }

   fun registrarClienteNuevo(nombre: String, telefono: String) {
      viewModelScope.launch(safeHandler) {
         val snap = db.collection(FirestoreCollections.CLIENTES).whereEqualTo("telefono", telefono).get().await()
         if (!snap.isEmpty) { 
            mensajeError = "Este cliente ya está registrado."
            return@launch 
         }
         val docRef = db.collection(FirestoreCollections.CLIENTES).document()
         val nuevo = ClienteV2(idDocumento = docRef.id, nombre = nombre, telefono = telefono)
         docRef.set(nuevo).await()
         _clienteSeleccionado = nuevo
         _descuentoLealtad = 0.0
      }
   }

   fun agregarAlCarrito(
      producto: SalesInventoryProductV2, 
      sucursal: String, 
      base: String? = null, 
      aderezo: String? = null, 
      toppings: List<String> = emptyList(), 
      esSeparado: Boolean = false
   ) {
      val nota = buildString {
         base?.let { append("Base: $it. ") }
         aderezo?.let { append("Aderezo: $it. ") }
         if (toppings.isNotEmpty()) append("Extras: ${toppings.joinToString(", ")}")
         if (esSeparado) append(" (Separadas)")
      }

      val precioBase = producto.precioVenta[sucursal.lowercase()] ?: 0.0
      val esCrepaOCombo = producto.categoria.uppercase().contains("CREPA") || 
                          producto.categoria.equals("Combos", true)

      val precioCalculado = if (esCrepaOCombo) {
         InventoryDeductions.calcularPrecioCrepa(precioBase, base ?: "", toppings, producto.costoToppingExtra)
      } else {
         val premiumToppings = listOf("oreo", "nuez", "bombon")
         val tienePremium = toppings.any { t -> premiumToppings.any { p -> t.lowercase().contains(p) } }
         val totalIngredientesNormales = toppings.count { t ->
            !premiumToppings.any { p -> t.lowercase().contains(p) }
         }
         val extraToppings = if (tienePremium || totalIngredientesNormales >= 2) 10.0 else 0.0
         precioBase + extraToppings
      }

      val item = ItemCarritoV2(
         producto = producto,
         precioFinal = BigDecimal.valueOf(precioCalculado),
         nota = nota,
         nombre = producto.nombre,
         base = base ?: "",
         aderezo = aderezo ?: "",
         toppings = toppings,
         esSeparado = esSeparado,
      )
      _carrito.add(item)
      mensajeFeedback = "${producto.nombre} añadido"
      Timber.tag("CART").i("Producto ${producto.nombre} añadido al carrito con $precioCalculado MXN")
   }

   fun modificarCantidad(item: ItemCarritoV2, delta: Int) {
      val index = _carrito.indexOf(item)
      if (index != -1) {
         val nuevaCant = item.cantidad + delta
         if (nuevaCant >= 1) _carrito[index] = item.copy(cantidad = nuevaCant)
      }
   }

   fun eliminarDelCarrito(item: ItemCarritoV2, motivo: String, usuarioNombre: String, sucursal: String) {
      _carrito.remove(item)
      viewModelScope.launch {
         try {
            val logId = db.collection(FirestoreCollections.CANCELACIONES).document().id
            val log = mapOf(
               "id" to logId, 
               "productoNombre" to item.nombre, 
               "precio" to item.precioFinal.toDouble(),
               "cantidad" to item.cantidad, 
               "motivo" to motivo, 
               "usuario" to usuarioNombre,
               "sucursal" to sucursal.lowercase(), 
               "fecha" to System.currentTimeMillis()
            )
            db.collection(FirestoreCollections.CANCELACIONES).document(logId).set(log).await()
            Timber.tag("CART").i("Cancelación guardada en Firestore: $logId")
         } catch (e: Exception) {
            val dataJson = "{\"productoNombre\":\"${item.nombre}\",\"precio\":${item.precioFinal.toDouble()},\"cantidad\":${item.cantidad}}"
            OfflineManager.guardarOperacionOffline(
               context = getApplication(),
               tipo = "cancelacion",
               ventaId = null,
               motivo = motivo,
               usuarioId = usuarioNombre,
               sucursal = sucursal,
               dataJson = dataJson,
               requiereAprobacion = true
            )
            Timber.tag("CART").w("Error guardando cancelación en Firestore, se guardó offline")
         }
      }
   }

   suspend fun validarStockCarrito(): Pair<Boolean, String> {
      for (item in _carrito) {
         val suficiente = inventoryRepo.hasSufficientStock(
            branchId = item.producto.id,
            productId = item.producto.id,
            requiredQty = item.cantidad.toDouble(),
            unit = "pza"
         )
         if (!suficiente) {
            Timber.tag("INVENTORY").w("Stock insuficiente para ${item.nombre}")
            return Pair(false, "Stock insuficiente para: ${item.nombre}")
         }
      }
      return Pair(true, "")
   }

   private fun limpiarEstadoPostVenta() {
      _carrito.clear()
      _clienteSeleccionado = null
      _descuentoLealtad = 0.0
      _esConsumoEmpleado = false
      _metodoPagoSeleccionado = MetodoPago.EFECTIVO
   }

   fun finalizarVenta(sucursal: String, usuarioNombre: String) {
      if (cargando || _carrito.isEmpty()) return
      if (_esConsumoEmpleado && _rolUsuario == Rol.VENDEDOR) {
         mensajeError = "Solo administradores pueden registrar consumos de cortesía"
         return
      }

      viewModelScope.launch {
         try {
            cargando = true
            isOnline = OfflineManager.isNetworkAvailable(getApplication())

            val stockOk = validarStockCarrito()
            if (!stockOk.first) {
               mensajeError = stockOk.second
               cargando = false
               return@launch
            }

            val totalVenta = calcularTotalVenta()
            val currentCarrito = _carrito.toList()
            val metodoPago = if (_esConsumoEmpleado) "Cortesía" else _metodoPagoSeleccionado.valor

            if (!isOnline) {
               procesarVentaOfflineConResultado(sucursal, usuarioNombre)
               return@launch
            }

            val saleItems = currentCarrito.map { item ->
               SaleItemInput(
                  productId = item.producto.id,
                  name = item.nombre,
                  quantity = item.cantidad.toDouble(),
                  unit = "pza",
                  unitPrice = item.precioFinal.toDouble(),
                  taxRate = 0.0,
                  category = item.producto.categoria,
                  recipe = null,
                  modifiers = emptyList()
               )
            }

            val result = salesFlowUseCase.processSale(
               branchId = sucursal.lowercase(),
               userId = usuarioNombre,
               items = saleItems,
               discounts = emptyList(),
               giro = "FOOD"
            )

            if (result.success) {
               val ticketNum = OfflineManager.obtenerUltimoTicketLocal(getApplication(), sucursal.lowercase()) + 1
               val codigoTicket = TicketUtils.generarCodigoTicket(sucursal.lowercase(), ticketNum)
               OfflineManager.guardarUltimoTicketLocal(getApplication(), sucursal.lowercase(), ticketNum)
               
               val resultado = com.bocatta.pos.domain.repository.ResultadoVenta(
                  numeroTicket = ticketNum,
                  codigoTicket = codigoTicket
               )
               aplicarResultadoVenta(resultado, sucursal, currentCarrito, totalVenta)
            } else {
               mensajeError = result.error ?: "Error al procesar venta"
            }
         } catch (e: Exception) {
            mensajeError = "Error en venta: ${e.message}. Guardando offline..."
            Timber.tag("SALE").e(e, "Error venta, guardando offline")
            try {
               procesarVentaOfflineConResultado(sucursal, usuarioNombre)
            } catch (fallbackEx: Exception) {
               mensajeError = "Error crítico al guardar offline: ${fallbackEx.message}"
               Timber.tag("SALE").e(fallbackEx, "Fallo total del fallback offline")
            }
         } finally {
            cargando = false
         }
      }
   }

   private suspend fun procesarVentaOfflineConResultado(sucursal: String, usuarioNombre: String) {
      val totalVenta = calcularTotalVenta()
      val currentCarrito = _carrito.toList()
      val metodoPago = if (_esConsumoEmpleado) "Cortesía" else _metodoPagoSeleccionado.valor
      val resultado = procesarVentaOffline(currentCarrito, sucursal, usuarioNombre, totalVenta, metodoPago)
      aplicarResultadoVenta(resultado, sucursal, currentCarrito, totalVenta)
   }

   private fun calcularTotalVenta(): Double {
      val total = totalCarrito
         .subtract(BigDecimal.valueOf(_descuentoLealtad))
         .subtract(BigDecimal.valueOf(descuentoPromociones))
         .toDouble()
      return if (total < 0) 0.0 else total
   }

   private fun procesarVentaOffline(
      currentCarrito: List<ItemCarritoV2>,
      sucursal: String,
      usuarioNombre: String,
      totalVenta: Double,
      metodoPago: String
   ): com.bocatta.pos.domain.repository.ResultadoVenta {
      val sucursalId = sucursal.lowercase()
      val ticketNum = OfflineManager.obtenerUltimoTicketLocal(getApplication(), sucursalId) + 1
      val codigoTicket = TicketUtils.generarCodigoTicket(sucursalId, ticketNum)
      OfflineManager.guardarUltimoTicketLocal(getApplication(), sucursalId, ticketNum)
      return OfflineManager.guardarVentaOffline(
         context = getApplication(),
         carrito = currentCarrito,
         sucursal = sucursal,
         usuarioNombre = usuarioNombre,
         total = totalVenta,
         descuentoLealtad = _descuentoLealtad,
         clienteSeleccionado = _clienteSeleccionado,
         metodoPago = metodoPago,
         esConsumoEmpleado = _esConsumoEmpleado,
         ticketNumber = ticketNum,
         codigoTicket = codigoTicket
      )
   }

   private fun aplicarResultadoVenta(
      resultado: com.bocatta.pos.domain.repository.ResultadoVenta,
      sucursal: String,
      currentCarrito: List<ItemCarritoV2>,
      totalVenta: Double
   ) {
      ultimoTicketAsignado = resultado.numeroTicket
      ultimoCodigoTicket = resultado.codigoTicket
      ultimoTicketTexto = generarTicketWhatsAppUseCase(
         sucursal, currentCarrito, resultado.codigoTicket,
         totalVenta, _descuentoLealtad, descuentoPromociones, _metodoPagoSeleccionado
      )
      Timber.tag("SALE").i("Venta completada, ticket #${resultado.codigoTicket}")
      mostrarConfirmacionVenta = true
      limpiarEstadoPostVenta()
   }

   fun registrarRetiroAlimento(sucursal: String, usuarioNombre: String, monto: Double) {
      if (monto > 150.0) { 
         mensajeError = "Límite excedido: Máximo $150"
         return 
      }
      viewModelScope.launch {
         try {
            val exito = repository.registrarGastoValidado(
               monto = monto,
               motivo = "Retiro Alimento Empleado: $usuarioNombre",
               sucursal = sucursal,
               usuarioId = usuarioNombre
            )
            if (exito) {
               mensajeFeedback = "Retiro de $${monto} registrado"
            } else {
               mensajeError = "Error al registrar retiro"
            }
         } catch (e: Exception) {
            mensajeError = "Error al registrar retiro: ${e.message}"
         }
      }
   }

   fun limpiarError() { mensajeError = null }
}
