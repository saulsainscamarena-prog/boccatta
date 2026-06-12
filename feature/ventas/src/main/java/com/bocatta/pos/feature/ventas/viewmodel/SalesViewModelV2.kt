package com.bocatta.pos.feature.ventas.viewmodel



import android.app.Application

import androidx.compose.runtime.*
import com.bocatta.pos.core.ui.viewmodel.BaseAndroidViewModel
import androidx.lifecycle.viewModelScope
import timber.log.Timber


import com.bocatta.pos.domain.model.*

import com.bocatta.pos.core.constants.FirestoreCollections

import com.bocatta.pos.feature.ventas.di.SalesDependencies

import com.bocatta.pos.network.NetworkStateProvider

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.Job

import kotlinx.coroutines.launch

import kotlinx.coroutines.tasks.await

import kotlinx.coroutines.withContext

import com.bocatta.pos.domain.MembresiaManager
import com.bocatta.pos.domain.MembresiaDiscountCalculator
import com.bocatta.pos.data.repository.MembresiaRepository
import com.bocatta.pos.domain.usecase.CheckoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.repository.SalesRepository
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.data.repository.InventoryDeductions
import com.bocatta.pos.data.repository.OperationalCatalogSyncRepository
import com.bocatta.pos.data.repository.PromocionesRepository
import com.bocatta.pos.data.local.OfflineDatabase
import com.google.firebase.firestore.ListenerRegistration
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.domain.engine.PricingEngine
import com.bocatta.pos.domain.usecase.SalesFlowUseCase
import com.bocatta.pos.feature.ventas.usecase.CartManager
import com.bocatta.pos.domain.util.CarritoCalculator

import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase

import com.bocatta.pos.domain.usecase.PromocionesEngine

import com.bocatta.pos.core.constants.SucursalConfig

import org.json.JSONObject



class SalesViewModelV2(

   application: Application,

   deps: SalesDependencies,

   private val networkStateProvider: NetworkStateProvider,

   private val catalogoUseCase: com.bocatta.pos.domain.usecase.CatalogoOperativoUseCase,

   private val authManager: com.bocatta.pos.domain.usecase.AuthorizationManager,

   private val catalogSyncRepository: OperationalCatalogSyncRepository,

   private val cartManager: CartManager,

   private val checkoutUseCase: CheckoutUseCase,

   private val customerRepository: com.bocatta.pos.data.repository.CustomerRepository,
   private val tenantManager: com.bocatta.pos.domain.usecase.TenantSessionManager
) : BaseAndroidViewModel(application) {

   val businessFeatures = com.bocatta.pos.domain.engine.BusinessLogicProvider.getFeaturesForType(tenantManager.getBusinessType())

   private val db = FirebaseFirestoreProvider.db

   private val productRepo: IProductRepository = deps.productRepo

   private val inventoryRepo: IInventoryRepository = deps.inventoryRepo

   private val salesFlowUseCase: SalesFlowUseCase = deps.salesFlowUseCase

   private val repository: SalesRepository = deps.repository

   private val generarTicketWhatsAppUseCase: GenerarTicketWhatsAppUseCase = deps.generarTicketWhatsAppUseCase

   private val registrarMermaProductoUseCase = deps.registrarMermaProductoUseCase

   private val promocionesEngine: PromocionesEngine = deps.promocionesEngine

   private val promocionesRepository: PromocionesRepository = deps.promocionesRepository



    private val _uiState = MutableStateFlow(SalesUiState())

    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()



    var catalogoProcesado by mutableStateOf<com.bocatta.pos.domain.usecase.CatalogoProcesado?>(null)

       private set



    private var ventasHistorial = emptyMap<String, Int>()

    private var catalogSyncJob: Job? = null



    private var _activeHeldOrderId by mutableStateOf<String?>(null)

    val activeHeldOrderId: String? get() = _activeHeldOrderId

    private var _lastCompletedHeldOrderId by mutableStateOf<String?>(null)

    val lastCompletedHeldOrderId: String? get() = _lastCompletedHeldOrderId



    private var menuJob: Job? = null

    private var stockJob: Job? = null



    private var _modalidadOrden by mutableStateOf(ModalidadOrden.LOCAL)

    val modalidadOrden: ModalidadOrden get() = _modalidadOrden



    private var _mesaIdSeleccionada by mutableStateOf<String?>(null)

    val mesaIdSeleccionada: String? get() = _mesaIdSeleccionada



    var isOnline by mutableStateOf(true)

       private set



   private var _productos = mutableStateListOf<SalesInventoryProductV2>()

   val productos: List<SalesInventoryProductV2> get() = _productos

   var menuCargando by mutableStateOf(false)

      private set

   var menuError by mutableStateOf<String?>(null)

      private set

   var menuUltimaCarga by mutableLongStateOf(0L)

      private set

    private val _carrito get() = cartManager.carrito

    val carrito: List<ItemCarritoV2> get() = _carrito

   private val cartIdSequence = AtomicLong(System.currentTimeMillis())

   private var _clienteSeleccionado by mutableStateOf<ClienteV2?>(null)

   val clienteSeleccionado: ClienteV2? get() = _clienteSeleccionado

   private var _descuentoLealtad by mutableDoubleStateOf(0.0)

   val descuentoLealtad: Double get() = _descuentoLealtad

   var ultimoTicketAsignado by mutableLongStateOf(0L)

      private set

   var ultimoCodigoTicket by mutableStateOf("")

      private set

   var ultimoTicketTexto by mutableStateOf<String?>(null)

      private set

    private var _mensajeFeedback by mutableStateOf<String?>(null)

    var mensajeFeedback: String?

       get() = _mensajeFeedback

       set(value) { _mensajeFeedback = value }

    // Loyalty message showing eligibility or progress
    private var _lealtadMensaje by mutableStateOf<String?>(null)
    var lealtadMensaje: String?
        get() = _lealtadMensaje
        private set(value) { _lealtadMensaje = value }

    private var _metodoPagoSeleccionado by mutableStateOf(MetodoPago.EFECTIVO)

    var metodoPagoSeleccionado: MetodoPago

       get() = _metodoPagoSeleccionado

       set(value) { _metodoPagoSeleccionado = value }

    private var _pagoMixtoActivo by mutableStateOf(false)

    var pagoMixtoActivo: Boolean

       get() = _pagoMixtoActivo

       set(value) {
          _pagoMixtoActivo = value
          if (!value) _montosMixtos.clear()
       }

    private val _montosMixtos = mutableStateMapOf<MetodoPago, Double>()

    val montosMixtos: Map<MetodoPago, Double> get() = _montosMixtos



    var splitActivo by mutableStateOf(false)

    private val _splitPartes = mutableStateListOf<com.bocatta.pos.domain.model.SplitParte>()

    val splitPartes: List<com.bocatta.pos.domain.model.SplitParte> get() = _splitPartes



    fun activarSplit(total: Double, personas: Int) {

        splitActivo = true

        _splitPartes.clear()

        _splitPartes.addAll(com.bocatta.pos.domain.model.calcularSplit(total, personas, _metodoPagoSeleccionado))

    }



    fun desactivarSplit() {

        splitActivo = false

        _splitPartes.clear()

    }



    fun actualizarSplitPartes(nuevasPartes: List<com.bocatta.pos.domain.model.SplitParte>) {

        _splitPartes.clear()

        _splitPartes.addAll(nuevasPartes)

    }



    fun toggleMetodoMixto(metodo: MetodoPago, monto: Double) {

        if (_montosMixtos.containsKey(metodo)) _montosMixtos.remove(metodo)

        else _montosMixtos[metodo] = monto

    }



    fun actualizarMontoMixto(metodo: MetodoPago, monto: Double) {

        _montosMixtos[metodo] = monto

    }

    private fun resetearEstadoPago() {
        _metodoPagoSeleccionado = MetodoPago.EFECTIVO
        _pagoMixtoActivo = false
        _montosMixtos.clear()
        splitActivo = false
        _splitPartes.clear()
        _esConsumoEmpleado = false
    }

    private fun generarCartId(productoId: String): String {
       val safeProductId = productoId.ifBlank { "item" }
          .replace(Regex("[^A-Za-z0-9_-]"), "_")
       return "cart_${cartIdSequence.incrementAndGet()}_$safeProductId"
    }



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



    private var _descuentoManualPorcentaje by mutableIntStateOf(0)

    val descuentoManual by derivedStateOf {
       val base = (totalCarrito.toDouble() - _descuentoLealtad - descuentoPromociones).coerceAtLeast(0.0)
       base * _descuentoManualPorcentaje / 100.0
    }



    fun aplicarDescuentoManual(porcentaje: Int) {

       _descuentoManualPorcentaje = porcentaje.coerceIn(0, 100)

    }



    fun limpiarDescuentoManual() {

       _descuentoManualPorcentaje = 0

    }



    val hayUndo: Boolean get() = cartManager.hayUndo



    fun guardarEstadoParaUndo() {
       cartManager.guardarEstadoParaUndo()

    }



     fun undoLastAction() {
        cartManager.undoLastAction()
      }



    val totalCarrito by derivedStateOf {

        CarritoCalculator.calcularSubtotal(_carrito)

    }



    private var sucursalActual: String = ""



    fun configurar(sucursal: String, usuarioNombre: String, rol: Rol) {

       _rolUsuario = rol

       sucursalActual = sucursal.lowercase(Locale.ROOT)

       escucharMenu()

       escucharStock()

       obtenerPromociones()

    }



    fun refrescarMenu() {

       escucharMenu()

       escucharStock()

       mensajeFeedback = "Actualizando menu y stock..."

    }



   private fun obtenerPromociones() {

      viewModelScope.launch {

         val promos = promocionesRepository.getPromocionesActivas()

         _promocionesActivas.clear()

         _promocionesActivas.addAll(promos)

      }

   }



    private fun escucharMenu() {
       menuJob?.cancel()
       menuCargando = true
       menuError = null
       menuJob = viewModelScope.launch {
          productRepo.getSalesProducts().collect { productsList ->
             _productos.clear()
             _productos.addAll(productsList)
             menuCargando = false
             menuError = null
             menuUltimaCarga = System.currentTimeMillis()
             
             val result = withContext(Dispatchers.Default) {
                 catalogoUseCase.clasificarYOrdenar(
                     menuOriginal = _productos.toList(),
                     ventasHistorial = ventasHistorial,
                     sucursal = sucursalActual
                 )
             }
             catalogoProcesado = result
             sincronizarCatalogoOperativoLocal(_productos.toList())
          }
       }
    }

    private fun sincronizarCatalogoOperativoLocal(productos: List<SalesInventoryProductV2>) {
       catalogSyncJob?.cancel()
       catalogSyncJob = viewModelScope.launch(Dispatchers.IO) {
          catalogSyncRepository.sincronizarCatalogoSucursal(sucursalActual, productos)
       }
    }



    private fun mapToConfigGroup(map: Map<String, Any?>): ConfigOptionGroup {
       val typeStr = (map["type"] as? String) ?: "SINGLE_CHIP"
       val type = try { ConfigFieldType.valueOf(typeStr) } catch (e: Exception) { Timber.e(e, "Invalid ConfigFieldType: %s", typeStr); ConfigFieldType.SINGLE_CHIP }

       val preciosExtraRaw = map["preciosExtra"] as? Map<*, *>
       val preciosExtra = preciosExtraRaw?.entries?.mapNotNull { (k, v) ->
          val key = k as? String
          val value = (v as? Number)?.toDouble()
          if (key != null && value != null) key to value else null
       }?.toMap() ?: emptyMap()

       val descuentosInsumoRaw = map["descuentosInsumo"] as? Map<*, *>
       val descuentosInsumo = descuentosInsumoRaw?.entries?.mapNotNull { (k, v) ->
          val key = k as? String
          val valMap = v as? Map<*, *>
          if (key != null && valMap != null) {
             val insumoId = valMap["insumoId"] as? String ?: ""
             val cantidad = (valMap["cantidad"] as? Number)?.toDouble() ?: 0.0
             val unidad = valMap["unidad"] as? String ?: "g"
             key to DescuentoOpcion(insumoId, cantidad, unidad)
          } else null
       }?.toMap() ?: emptyMap()

       return ConfigOptionGroup(
          key = map["key"] as? String ?: "",
          title = map["title"] as? String ?: "",
          type = type,
          options = (map["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
          required = map["required"] as? Boolean ?: false,
          multiMax = (map["multiMax"] as? Number)?.toInt(),
          defaultValue = map["defaultValue"] as? String,
          preciosExtra = preciosExtra,
          descuentosInsumo = descuentosInsumo
       )
    }



   private fun escucharStock() {
      stockJob?.cancel()
      stockJob = viewModelScope.launch {
         inventoryRepo.getStockAlertsFlow(sucursalActual).collect { alerts ->
            _alertasStock.clear()
            _alertasStock.putAll(alerts)
         }
      }
   }



   override fun onCleared() {

      super.onCleared()

      catalogSyncJob?.cancel()

      menuJob?.cancel()

      stockJob?.cancel()

   }



   fun buscarCliente(query: String) {

      viewModelScope.launch(safeHandler) {

         val results = customerRepository.buscarCliente(query)

         _clientesSugeridos.clear()

         _clientesSugeridos.addAll(results)

      }

   }



    fun seleccionarCliente(cliente: ClienteV2) {
        _clienteSeleccionado = cliente
        // Check loyalty eligibility
        val estado = MembresiaManager.verificarEstadoMembresia(cliente.visitasCicloActual)
        if (estado.esElegiblePremio) {
            val promedio = MembresiaDiscountCalculator.calcularPromedio(cliente.comprasCicloActual)
            val descuento = MembresiaDiscountCalculator.calcularPorcentajeDescuento(promedio)
            _descuentoLealtad = descuento
            lealtadMensaje = "┬íBeneficio de lealtad aplicado!"
        } else {
            _descuentoLealtad = 0.0
            lealtadMensaje = "${estado.nivel} ┬À ${estado.visitasRestantesParaPremio} visitas para beneficio"
        }
    }



   fun eliminarCliente() {

      _clienteSeleccionado = null

      _descuentoLealtad = 0.0

   }



   fun registrarClienteNuevo(nombre: String, telefono: String) {

      viewModelScope.launch(safeHandler) {

         val result = customerRepository.registrarClienteNuevo(nombre, telefono)

         result.onSuccess { nuevo ->

            _clienteSeleccionado = nuevo

            _descuentoLealtad = 0.0

         }.onFailure {

            mensajeError = it.message ?: "Error al registrar cliente."

         }

      }

   }



      fun agregarAlCarrito(
         producto: SalesInventoryProductV2,
         sucursal: String,
         base: String? = null,
         aderezos: List<String> = emptyList(),
         toppings: List<String> = emptyList(),
         esSeparado: Boolean = false,
         componentes: List<ItemCarritoV2> = emptyList(),
         cantidadGramos: Double? = null
      ) {
         cartManager.agregarAlCarrito(producto, sucursal, base, aderezos, toppings, esSeparado, componentes, cantidadGramos)
      }

      fun reemplazarItemCarrito(
         itemOriginal: ItemCarritoV2,
         producto: SalesInventoryProductV2,
         sucursal: String,
         base: String? = null,
         aderezos: List<String> = emptyList(),
         toppings: List<String> = emptyList(),
         esSeparado: Boolean = false,
         componentes: List<ItemCarritoV2> = emptyList(),
         cantidadGramos: Double? = null
      ) {
         cartManager.reemplazarItemCarrito(itemOriginal, producto, sucursal, base, aderezos, toppings, esSeparado, componentes, cantidadGramos)
      }

      fun agregarAlCarritoConConfig(
         producto: SalesInventoryProductV2,
         sucursal: String,
         config: ConfigResult
      ) {
         cartManager.agregarAlCarritoConConfig(producto, sucursal, config)
      }

      fun reemplazarItemCarritoConConfig(
         itemOriginal: ItemCarritoV2,
         producto: SalesInventoryProductV2,
         sucursal: String,
         config: ConfigResult
      ) {
         cartManager.reemplazarItemCarritoConConfig(itemOriginal, producto, sucursal, config)
      }

      fun modificarCantidad(item: ItemCarritoV2, delta: Int) {
         cartManager.modificarCantidad(item, delta)
      }



    fun eliminarDelCarrito(item: ItemCarritoV2, motivo: String, usuarioNombre: String, sucursal: String) {

       cartManager.eliminarDelCarrito(item)

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

                "sucursal" to sucursal.lowercase(Locale.ROOT),

                "estado" to "pendiente_revision",

                "fecha" to System.currentTimeMillis()

             )

             db.collection(FirestoreCollections.CANCELACIONES).document(logId).set(log).await()

             Timber.tag("CART").i("Cancelacion guardada en Firestore: $logId")

          } catch (e: Exception) {

              val dataJson = JSONObject()
                 .put("productoNombre", item.nombre)
                 .put("precio", item.precioFinal.toDouble())
                 .put("cantidad", item.cantidad)
                 .toString()

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

             Timber.tag("CART").w("Error guardando cancelacion en Firestore, se guardo offline")

          }

       }

    }



    private fun limpiarEstadoPostVenta() {

        cartManager.limpiarCarrito(guardarUndo = false)

        _clienteSeleccionado = null

        _descuentoLealtad = 0.0

        resetearEstadoPago()

        _activeHeldOrderId = null

        _mesaIdSeleccionada = null

        _modalidadOrden = ModalidadOrden.LOCAL

    }



    fun finalizarVenta(sucursal: String, usuarioNombre: String, propina: Double = 0.0, notaOrden: String = "") {
        if (cargando || _carrito.isEmpty()) {
            Timber.tag("SALE_FLOW").w("finish_ignored cargando=$cargando carrito=${_carrito.size}")
            Timber.tag("sale_finish_ignored").d("cargando=$cargando carrito=${_carrito.size}")
            return
        }

        if (_esConsumoEmpleado && _rolUsuario == Rol.VENDEDOR) {
            mensajeError = "Solo administradores pueden registrar consumos de cortesia"
            Timber.tag("sale_finish_blocked").d("employee_consumption_role")
            return
        }

        cargando = true
        _uiState.update { it.copy(isLoading = true, error = null, showSuccess = false) }
        Timber.tag("SALE_FLOW").i("finish_start carrito=${_carrito.size} sucursal=$sucursal metodo=${_metodoPagoSeleccionado.valor}")
        Timber.tag("sale_finish_start").d("items=${_carrito.size} sucursal=$sucursal")

        viewModelScope.launch {
            try {
                val res = checkoutUseCase.finalizarVenta(
                    carrito = _carrito.toList(),
                    sucursal = sucursal,
                    usuarioNombre = usuarioNombre,
                    clienteSeleccionado = _clienteSeleccionado,
                    descuentoLealtad = _descuentoLealtad,
                    descuentoPromociones = descuentoPromociones,
                    descuentoManual = descuentoManual,
                    propina = propina,
                    notaOrden = notaOrden,
                    esConsumoEmpleado = _esConsumoEmpleado,
                    splitActivo = splitActivo,
                    splitPartes = if (splitActivo) _splitPartes.toList() else emptyList(),
                    metodoPagoSeleccionado = _metodoPagoSeleccionado.valor,
                    forcedVentaId = db.collection(FirestoreCollections.VENTAS).document().id
                )

                isOnline = res.online
                if (res.success) {
                    ultimoTicketAsignado = res.numeroTicket ?: 0L
                    ultimoCodigoTicket = res.codigoTicket ?: ""
                    ultimoTicketTexto = res.ticketText ?: ""
                    mostrarConfirmacionVenta = true
                    _lastCompletedHeldOrderId = _activeHeldOrderId
                    mensajeFeedback = if (res.online) {
                        "Venta finalizada: ticket #${res.codigoTicket}"
                    } else {
                        "Venta guardada localmente (modo offline)"
                    }
                    _uiState.update { it.copy(showSuccess = true, isLoading = false) }
                    Timber.tag("sale_finish_success").d("ticket=${res.codigoTicket}")

                    // Post-sale loyalty visit increment
                    viewModelScope.launch(Dispatchers.IO) {
                        val cliente = _clienteSeleccionado
                        if (cliente != null) {
                            val premioAplicado = _descuentoLealtad > 0
                            val subtotal = _carrito.sumOf { item -> item.precioFinal.toDouble() * item.cantidad }
                            val totalSinPropina = (subtotal - _descuentoLealtad - descuentoPromociones - descuentoManual).coerceAtLeast(0.0)
                            val totalVenta = if (_esConsumoEmpleado) 0.0 else totalSinPropina + propina.coerceAtLeast(0.0)
                            MembresiaRepository().incrementarVisita(
                                clienteId = cliente.idDocumento,
                                montoCompra = totalVenta,
                                premioAplicado = premioAplicado
                            )
                            if (premioAplicado) {
                                _descuentoLealtad = 0.0
                                lealtadMensaje = null
                            }
                        }
                    }
                } else {
                    mensajeError = res.error
                    _uiState.update { it.copy(error = res.error, isLoading = false) }
                    Timber.tag("sale_finish_failed").d(res.error.orEmpty().take(80))
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Error desconocido"
                mensajeError = errorMsg
                _uiState.update { it.copy(error = errorMsg, isLoading = false) }
                Timber.tag("sale_finish_exception").d(errorMsg.take(80))
            } finally {
                cargando = false
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }



    fun registrarRetiroAlimento(sucursal: String, usuarioNombre: String, monto: Double) {

        if (monto > 150.0) {

            mensajeError = "Limite excedido: Maximo $150"

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



    fun limpiarCarrito() {

        cartManager.limpiarCarrito()

        _clienteSeleccionado = null

        resetearEstadoPago()

        _activeHeldOrderId = null

        _mesaIdSeleccionada = null

        _modalidadOrden = ModalidadOrden.LOCAL

        _uiState.update { SalesUiState() }

    }



    fun cambiarModalidadOrden(modalidad: ModalidadOrden) {

        _modalidadOrden = modalidad

    }



    fun seleccionarMesa(mesaId: String?) {

        _mesaIdSeleccionada = mesaId

    }



    fun toggleParaLlevarItem(cartId: String) {
        cartManager.toggleParaLlevarItem(cartId)
    }



    fun cargarOrdenEnCarrito(

        items: List<ItemCarritoV2>,

        cliente: ClienteV2?,

        modalidad: ModalidadOrden,

        idMesa: String?,

        heldOrderId: String? = null

    ) {

        _carrito.clear()

        _carrito.addAll(items)

        _clienteSeleccionado = cliente

        if (cliente != null) {

            _descuentoLealtad = if (cliente.visitasCicloActual == FirestoreCollections.MEMBRESIA_CICLO_VISITAS) {

                cliente.comprasCicloActual.average()

            } else 0.0

        } else {

            _descuentoLealtad = 0.0

        }

        limpiarDescuentoManual()

        _modalidadOrden = modalidad

        _mesaIdSeleccionada = idMesa

        _activeHeldOrderId = heldOrderId

        _uiState.update { SalesUiState() }

    }



    fun limpiarError() { mensajeError = null }



    fun onErrorShown() {

        _uiState.update { it.copy(error = null) }

    }



    fun onSuccessShown() {

        _lastCompletedHeldOrderId = null

        _uiState.update { it.copy(showSuccess = false) }

    }

    fun registrarMermaProducto(
        producto: SalesInventoryProductV2,
        cantidad: Int,
        motivo: String,
        sucursal: String,
        usuarioNombre: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = registrarMermaProductoUseCase(
                producto = producto,
                cantidad = cantidad,
                motivo = motivo,
                sucursal = sucursal,
                usuarioNombre = usuarioNombre
            )
            result
                .onSuccess {
                    mensajeFeedback = "Merma de ${producto.nombre} registrada con exito"
                    onResult(true)
                }
                .onFailure { error ->
                    mensajeError = "Error al registrar merma: ${error.message}"
                    onResult(false)
                }
        }
    }

}
