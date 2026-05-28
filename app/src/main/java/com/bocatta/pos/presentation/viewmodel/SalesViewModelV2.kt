package com.bocatta.pos.presentation.viewmodel



import android.app.Application

import androidx.compose.runtime.*

import androidx.lifecycle.viewModelScope

import com.bocatta.pos.domain.model.*

import com.bocatta.pos.core.constants.FirestoreCollections

import com.bocatta.pos.di.SalesDependencies

import com.bocatta.pos.network.NetworkStateProvider

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.Job

import kotlinx.coroutines.launch

import kotlinx.coroutines.tasks.await

import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import java.math.BigDecimal
import java.util.Locale

import com.bocatta.pos.domain.repository.IInventoryRepository

import com.bocatta.pos.domain.repository.IProductRepository

import com.bocatta.pos.domain.repository.SalesRepository

import com.bocatta.pos.data.sync.OfflineManager

import com.bocatta.pos.data.sync.SyncScheduler

import com.bocatta.pos.data.repository.InventoryDeductions

import com.bocatta.pos.data.repository.OperationalCatalogSyncRepository

import com.bocatta.pos.data.repository.PromocionesRepository

import com.bocatta.pos.data.local.OfflineDatabase

import timber.log.Timber

import com.google.firebase.firestore.ListenerRegistration

import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider

import com.bocatta.pos.domain.engine.PricingEngine

import com.bocatta.pos.domain.usecase.SalesFlowUseCase

import com.bocatta.pos.domain.util.CarritoCalculator

import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase

import com.bocatta.pos.domain.usecase.PromocionesEngine

import com.bocatta.pos.core.constants.SucursalConfig



class SalesViewModelV2(

   application: Application,

   deps: SalesDependencies,

   private val networkStateProvider: NetworkStateProvider,

   private val catalogoUseCase: com.bocatta.pos.domain.usecase.CatalogoOperativoUseCase,

   private val authManager: com.bocatta.pos.domain.usecase.AuthorizationManager,

   private val catalogSyncRepository: OperationalCatalogSyncRepository

) : BaseAndroidViewModel(application) {

   private val db = FirebaseFirestoreProvider.db

   private val productRepo: IProductRepository = deps.productRepo

   private val inventoryRepo: IInventoryRepository = deps.inventoryRepo

   private val salesFlowUseCase: SalesFlowUseCase = deps.salesFlowUseCase

   private val repository: SalesRepository = deps.repository

   private val generarTicketWhatsAppUseCase: GenerarTicketWhatsAppUseCase = deps.generarTicketWhatsAppUseCase

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

   var menuUltimaCarga by mutableStateOf(0L)

      private set

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

    private var _pagoMixtoActivo by mutableStateOf(false)

    var pagoMixtoActivo: Boolean

       get() = _pagoMixtoActivo

       set(value) { _pagoMixtoActivo = value }

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



    private var _descuentoManual by mutableStateOf(0.0)

    val descuentoManual: Double get() = _descuentoManual



    fun aplicarDescuentoManual(porcentaje: Int) {

       val base = (totalCarrito.toDouble() - _descuentoLealtad - descuentoPromociones).coerceAtLeast(0.0)

       _descuentoManual = base * porcentaje / 100.0

    }



    fun limpiarDescuentoManual() {

       _descuentoManual = 0.0

    }



    // Undo stack

    private val _undoStack = mutableListOf<List<ItemCarritoV2>>()

    val hayUndo: Boolean get() = _undoStack.isNotEmpty()



    fun guardarEstadoParaUndo() {

       _undoStack.add(_carrito.toList())

       if (_undoStack.size > 20) _undoStack.removeFirst() // limite 20

    }



    fun undoLastAction() {

       if (_undoStack.isEmpty()) return

       _carrito.clear()

       _carrito.addAll(_undoStack.removeLast())

    }



    private var menuListener: ListenerRegistration? = null

   private var stockListener: ListenerRegistration? = null



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

       menuListener?.remove()

       menuCargando = true

       menuError = null

       menuListener = db.collection(FirestoreCollections.PRODUCTOS).addSnapshotListener { snap, error ->

          if (error != null) {

             menuCargando = false

             menuError = "No se pudo cargar el menu. Revisa conexion o vuelve a intentar."

             return@addSnapshotListener

          }

          if (snap != null) {

             _productos.clear()

             snap.documents.forEach { doc ->

                doc.toObject(SalesInventoryProductV2::class.java)?.let { prod ->

                   val schema = (doc.get("configSchema") as? List<*>)

                      ?.mapNotNull { rawGroup ->

                         (rawGroup as? Map<*, *>)?.entries

                            ?.mapNotNull { (key, value) -> (key as? String)?.let { it to value } }

                            ?.toMap()

                      }

                      ?.map { mapToConfigGroup(it) } ?: emptyList()

                   _productos.add(prod.copy(configSchema = schema, id = doc.id))

                }

             }

             menuCargando = false

             menuError = null

             menuUltimaCarga = System.currentTimeMillis()

             catalogoProcesado = catalogoUseCase.clasificarYOrdenar(
                menuOriginal = _productos.toList(),
                ventasHistorial = ventasHistorial,
                sucursal = sucursalActual
             )
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

       val type = try { ConfigFieldType.valueOf(typeStr) } catch (_: Exception) { ConfigFieldType.SINGLE_CHIP }

       return ConfigOptionGroup(

          key = map["key"] as? String ?: "",

          title = map["title"] as? String ?: "",

          type = type,

          options = (map["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),

          required = map["required"] as? Boolean ?: false,

          multiMax = (map["multiMax"] as? Number)?.toInt(),

          defaultValue = map["defaultValue"] as? String

       )

    }



   private fun escucharStock() {

      stockListener?.remove()

      stockListener = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)

         .whereEqualTo("sucursal", sucursalActual)

         .addSnapshotListener { snap, _ ->

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

      catalogSyncJob?.cancel()

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

            mensajeError = "Este cliente ya esta registrado."

            return@launch

         }

         val docRef = db.collection(FirestoreCollections.CLIENTES).document()

         val nuevo = ClienteV2(idDocumento = docRef.id, nombre = nombre, telefono = telefono)

         docRef.set(nuevo).await()

         _clienteSeleccionado = nuevo

         _descuentoLealtad = 0.0

      }

   }



     private fun crearItemCarrito(

        producto: SalesInventoryProductV2,

        sucursal: String,

        base: String? = null,

        aderezos: List<String> = emptyList(),

        toppings: List<String> = emptyList(),

        esSeparado: Boolean = false,

        componentes: List<ItemCarritoV2> = emptyList(),

        cantidadGramos: Double? = null

      ): ItemCarritoV2 {

         val nota = buildString {

            if (cantidadGramos != null && cantidadGramos > 0.0) {

                append("${cantidadGramos.toInt()}g de ${producto.nombre}")

            } else {

                base?.let { append("Base: $it. ") }

                if (aderezos.isNotEmpty()) append("Aderezos: ${aderezos.joinToString(", ")}. ")

                if (toppings.isNotEmpty()) append("Extras: ${toppings.joinToString(", ")}")

                if (esSeparado) append(" (Separadas)")

            }

        }



        val precioBase = producto.precioVenta[sucursal.lowercase(Locale.ROOT)] ?: 0.0

        val esCrepaOCombo = producto.categoria.uppercase(Locale.ROOT).contains("CREPA") ||

                            producto.categoria.equals("Combos", true)



        val precioCalculado = if (esCrepaOCombo) {

           InventoryDeductions.calcularPrecioCrepa(precioBase, base ?: "", toppings, producto.costoToppingExtra)

        } else {

           val premiumToppings = listOf("oreo", "nuez", "bombon")

           val tienePremium = toppings.any { t -> premiumToppings.any { p -> t.lowercase(Locale.ROOT).contains(p) } }

           val totalIngredientesNormales = toppings.count { t ->

              !premiumToppings.any { p -> t.lowercase(Locale.ROOT).contains(p) }

           }

           val extraToppings = if (tienePremium || totalIngredientesNormales >= 2) 10.0 else 0.0

           precioBase + extraToppings

        }



        return ItemCarritoV2(

           cartId = "cart_${System.currentTimeMillis()}_${_carrito.size}_${producto.id}",

           producto = producto,

           precioFinal = BigDecimal.valueOf(precioCalculado),

           nota = nota,

           nombre = producto.nombre,

           base = base ?: "",

           aderezos = aderezos,

           toppings = toppings,

           esSeparado = esSeparado,

           componentesCombo = componentes,

           cantidadGramos = cantidadGramos

        )

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

         guardarEstadoParaUndo()

         val item = crearItemCarrito(producto, sucursal, base, aderezos, toppings, esSeparado, componentes, cantidadGramos)

         _carrito.add(item)

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

        val index = _carrito.indexOf(itemOriginal)

        if (index == -1) return

        guardarEstadoParaUndo()

        _carrito[index] = crearItemCarrito(producto, sucursal, base, aderezos, toppings, esSeparado, componentes, cantidadGramos)

     }



     fun agregarAlCarritoConConfig(

        producto: SalesInventoryProductV2,

        sucursal: String,

        config: ConfigResult

     ) {

         guardarEstadoParaUndo()

         val base = config["base"]?.firstOrNull()?.removeSuffix(" (Premium)")

         val aderezos = config["aderezos"] ?: emptyList()

         val toppings = (config["toppings"] ?: emptyList()).map { it.removeSuffix(" (Premium)").removeSuffix(" (Premium)") }

         val extras = config.entries

            .filter { it.key !in setOf("base", "aderezos", "toppings") }

            .flatMap { (key, values) -> values.map { "$key: $it" } }



         val nota = buildString {

            base?.let { append("Base: $it. ") }

            if (aderezos.isNotEmpty()) append("Aderezos: ${aderezos.joinToString(", ")}. ")

            if (toppings.isNotEmpty()) append("Extras: ${toppings.joinToString(", ")}")

            if (extras.isNotEmpty()) append(" [${extras.joinToString("; ")}]")

         }



         val precioBase = producto.precioVenta[sucursal.lowercase(Locale.ROOT)] ?: 0.0

         val preciosExtra = producto.configSchema.flatMap { it.preciosExtra.entries }.associate { it.key to it.value }

         val precioCalculado = PricingEngine.calcularPrecioProducto(precioBase, producto.categoria, config, preciosExtra)



        val item = ItemCarritoV2(

           cartId = "cart_${System.currentTimeMillis()}_${_carrito.size}_${producto.id}",

           producto = producto,

           precioFinal = BigDecimal.valueOf(precioCalculado),

           nota = nota,

           nombre = producto.nombre,

           base = base ?: "",

           aderezos = aderezos,

           toppings = toppings,

            esSeparado = false,

            componentesCombo = emptyList()

        )

        _carrito.add(item)

      }



    fun modificarCantidad(item: ItemCarritoV2, delta: Int) {

       val index = _carrito.indexOf(item)

       if (index != -1) {

          val nuevaCant = item.cantidad + delta

          if (nuevaCant >= 1) _carrito[index] = item.copy(cantidad = nuevaCant)

       }

    }



    fun eliminarDelCarrito(item: ItemCarritoV2, motivo: String, usuarioNombre: String, sucursal: String) {

       guardarEstadoParaUndo()

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

                "sucursal" to sucursal.lowercase(Locale.ROOT),

                "estado" to "pendiente_revision",

                "fecha" to System.currentTimeMillis()

             )

             db.collection(FirestoreCollections.CANCELACIONES).document(logId).set(log).await()

             Timber.tag("CART").i("Cancelacion guardada en Firestore: $logId")

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

             Timber.tag("CART").w("Error guardando cancelacion en Firestore, se guardo offline")

          }

       }

    }



    suspend fun validarStockCarrito(sucursal: String): Pair<Boolean, String> {

       val sucursalId = sucursal.lowercase(Locale.ROOT)

       val offlineDb = OfflineDatabase.getInstance(getApplication())

       val consolidado = mutableMapOf<String, Double>()



       for (item in _carrito) {

          val recetaId = item.producto.recetaId

          val receta = if (!recetaId.isNullOrEmpty()) {

             withContext(Dispatchers.IO) {

                offlineDb.obtenerRecetaPorId(recetaId)

             }

          } else {

             null

          }



          val deds = InventoryDeductions.calcularParaItem(item, receta?.ingredientes ?: emptyList())

          if (deds.isNotEmpty()) {

             deds.forEach { (insumoId, cantidad) ->

                consolidado[insumoId] = (consolidado[insumoId] ?: 0.0) + cantidad

             }

             if (item.producto.id.contains("crepa", ignoreCase = true) ||

                  item.producto.categoria.uppercase(Locale.ROOT).contains("COMBO")) {

                if (!consolidado.containsKey("masa_crepa")) {

                   consolidado["masa_crepa"] = (consolidado["masa_crepa"] ?: 0.0) + item.cantidad.toDouble()

                }

             }

          } else {

             val insumoId = if (item.producto.id.contains("crepa", ignoreCase = true) ||

                                 item.producto.categoria.uppercase(Locale.ROOT).contains("COMBO")) {

                "masa_crepa"

             } else {

                item.producto.id

             }

             consolidado[insumoId] = (consolidado[insumoId] ?: 0.0) + item.cantidad.toDouble()

          }

       }



       for ((insumoId, cantidadRequerida) in consolidado) {

          val suficiente = withContext(Dispatchers.IO) {

             inventoryRepo.hasSufficientStock(

                branchId = sucursalId,

                productId = insumoId,

                requiredQty = cantidadRequerida,

                unit = "pza"

             )

          }



          if (!suficiente) {

             val nombreInsumo = if (insumoId == "masa_crepa") {

                "MASA DE CREPA"

             } else {

                withContext(Dispatchers.IO) {

                   offlineDb.obtenerInsumos().find { it.id == insumoId }?.nombre

                       ?: offlineDb.obtenerProductoPorId(insumoId)?.nombre

                       ?: insumoId

                }

             }



             val msg = if (insumoId == "masa_crepa") {

                "Sin stock de MASA DE CREPA en $sucursalId. Registra una tanda en Inventario > Produccion."

             } else {

                "Stock insuficiente de $nombreInsumo en $sucursalId. Requerido: $cantidadRequerida."

             }

             Timber.tag("INVENTORY").w(msg)

             return Pair(false, msg)

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

        _activeHeldOrderId = null

        _mesaIdSeleccionada = null

        _modalidadOrden = ModalidadOrden.LOCAL

        splitActivo = false

        _splitPartes.clear()

    }



    fun finalizarVenta(sucursal: String, usuarioNombre: String) {

        if (cargando || _carrito.isEmpty()) return

        if (_esConsumoEmpleado && _rolUsuario == Rol.VENDEDOR) {

            mensajeError = "Solo administradores pueden registrar consumos de cortesia"

            return

        }



        cargando = true

        _uiState.update { it.copy(isLoading = true, error = null, showSuccess = false) }



        viewModelScope.launch {

            val (stockValido, errorMsg) = validarStockCarrito(sucursal)

            if (!stockValido) {

                mensajeError = errorMsg

                _uiState.update { it.copy(isLoading = false, error = errorMsg) }

                cargando = false

                return@launch

            }



            val totalVenta = calcularTotalVenta()

            val currentCarrito = _carrito.toList()

            val clienteSnapshot = _clienteSeleccionado

            val descuentoLealtadSnapshot = _descuentoLealtad

            val descuentoPromocionesSnapshot = descuentoPromociones

            val descuentoManualSnapshot = _descuentoManual

            val esConsumoEmpleadoSnapshot = _esConsumoEmpleado

            val metodoPago = if (esConsumoEmpleadoSnapshot) {

                "Cortesia"

            } else if (splitActivo) {

                "Dividido: [" + _splitPartes.joinToString(", ") { "${it.metodoPago.valor} $${"%.2f".format(it.monto)}" } + "]"

            } else {

                _metodoPagoSeleccionado.valor

            }



            try {

                mensajeError = null

                val online = withContext(Dispatchers.IO) {

                    OfflineManager.isNetworkAvailable(getApplication())

                }

                isOnline = online

                if (!online) {

                    val resultado = withContext(Dispatchers.IO) {

                        procesarVentaOffline(

                            currentCarrito = currentCarrito,

                            sucursal = sucursal,

                            usuarioNombre = usuarioNombre,

                            totalVenta = totalVenta,

                            metodoPago = metodoPago,

                            descuentoLealtad = descuentoLealtadSnapshot,

                            clienteSeleccionado = clienteSnapshot,

                            esConsumoEmpleado = esConsumoEmpleadoSnapshot

                        )

                    }

                    aplicarResultadoVenta(resultado, sucursal, currentCarrito, totalVenta, metodoPago)

                    mensajeFeedback = "Venta guardada localmente (modo offline)"

                    _uiState.update { it.copy(showSuccess = true) }

                    return@launch

                }



                // 2. Ejecutar Venta en la Nube

                val resultado = withContext(Dispatchers.IO) {

                    repository.finalizarVentaConInventario(

                        carrito = currentCarrito,

                        sucursal = sucursal,

                        usuarioNombre = usuarioNombre,

                        clienteSeleccionado = clienteSnapshot,

                        descuentoLealtad = descuentoLealtadSnapshot,

                        metodoPagoSeleccionado = metodoPago,

                        esConsumoEmpleado = esConsumoEmpleadoSnapshot,

                        descuentoPromociones = descuentoPromocionesSnapshot,

                        descuentoManual = descuentoManualSnapshot,

                        splitPartes = if (splitActivo) _splitPartes.toList() else emptyList()

                    )

                }

                aplicarResultadoVenta(resultado, sucursal, currentCarrito, totalVenta, metodoPago)

                mensajeFeedback = "Venta finalizada: ticket #${resultado.codigoTicket}"

                _uiState.update { it.copy(showSuccess = true) }



            } catch (e: Exception) {

                mensajeError = mensajeOperativo(e)

                _uiState.update { it.copy(error = mensajeError) }

                Timber.tag("SALE").e(e, "Error en flujo de venta")



                // Fallback offline si es error de conexion/servidor (no de stock)

                if (e !is IllegalStateException && isOnline) {

                    try {

                        val resultado = withContext(Dispatchers.IO) {

                            procesarVentaOffline(

                                currentCarrito = currentCarrito,

                                sucursal = sucursal,

                                usuarioNombre = usuarioNombre,

                                totalVenta = totalVenta,

                                metodoPago = metodoPago,

                                descuentoLealtad = descuentoLealtadSnapshot,

                                clienteSeleccionado = clienteSnapshot,

                                esConsumoEmpleado = esConsumoEmpleadoSnapshot

                            )

                        }

                        aplicarResultadoVenta(resultado, sucursal, currentCarrito, totalVenta, metodoPago)

                        mensajeFeedback = "Error de red. Venta guardada localmente."

                        mensajeError = null

                        _uiState.update { it.copy(error = null, showSuccess = true) }

                    } catch (fallbackEx: Exception) {

                        mensajeError = "Fallo total: ${fallbackEx.message}"

                        _uiState.update { it.copy(error = mensajeError) }

                    }

                }

            } finally {

                cargando = false

                _uiState.update { it.copy(isLoading = false) }

            }

        }

    }



    private fun calcularTotalVenta(): Double {

        return CarritoCalculator.calcularTotalVenta(

            subtotal = CarritoCalculator.calcularSubtotal(_carrito),

            descuentoLealtad = _descuentoLealtad,

            descuentoPromociones = descuentoPromociones,

            descuentoManual = _descuentoManual

        )

    }



    private fun mensajeOperativo(error: Exception): String {

        val raw = error.message.orEmpty()

        return when {

            raw.contains("masa_crepa", ignoreCase = true) ->

                "No hay crepas asignadas para esta sucursal. Ve a apertura/asignacion o registra produccion."

            raw.contains("bodega central", ignoreCase = true) ->

                "Falta stock en bodega central. Revisa compras, produccion o asignacion antes de cobrar."

            raw.contains("sucursal", ignoreCase = true) && raw.contains("Stock insuficiente", ignoreCase = true) ->

                "Falta stock asignado para esta sucursal. Revisa la asignacion del turno."

            raw.contains("Contador", ignoreCase = true) ->

                "El folio de tickets se preparo automaticamente. Intenta cobrar de nuevo."

            raw.isBlank() -> "No se pudo completar la venta. Revisa stock y conexion."

            else -> raw

        }

    }



    private fun procesarVentaOffline(

        currentCarrito: List<ItemCarritoV2>,

        sucursal: String,

        usuarioNombre: String,

        totalVenta: Double,

        metodoPago: String,

        descuentoLealtad: Double,

        clienteSeleccionado: ClienteV2?,

        esConsumoEmpleado: Boolean

    ): com.bocatta.pos.domain.repository.ResultadoVenta {

        return OfflineManager.guardarVentaOffline(

            context = getApplication(),

            carrito = currentCarrito,

            sucursal = sucursal,

            usuarioNombre = usuarioNombre,

            total = totalVenta,

            descuentoLealtad = descuentoLealtad,

            clienteSeleccionado = clienteSeleccionado,

            metodoPago = metodoPago,

            esConsumoEmpleado = esConsumoEmpleado

        )

    }



    private fun aplicarResultadoVenta(

        resultado: com.bocatta.pos.domain.repository.ResultadoVenta,

        sucursal: String,

        currentCarrito: List<ItemCarritoV2>,

        totalVenta: Double,

        metodoPago: String

    ) {

        ultimoTicketAsignado = resultado.numeroTicket

        ultimoCodigoTicket = resultado.codigoTicket

        ultimoTicketTexto = generarTicketWhatsAppUseCase(

            sucursal = sucursal,

            items = currentCarrito,

            codigoTicket = resultado.codigoTicket,

            total = totalVenta,

            descuentoLealtad = _descuentoLealtad,

            descuentoPromociones = descuentoPromociones,

            metodoPago = metodoPago

        )

        Timber.tag("SALE").i("Venta completada, ticket #${resultado.codigoTicket}")

        mostrarConfirmacionVenta = true

        _lastCompletedHeldOrderId = _activeHeldOrderId

        limpiarEstadoPostVenta()

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

        guardarEstadoParaUndo()

        _carrito.clear()

        _clienteSeleccionado = null

        _descuentoManual = 0.0

        _metodoPagoSeleccionado = MetodoPago.EFECTIVO

        _esConsumoEmpleado = false

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

        val index = _carrito.indexOfFirst { it.cartId == cartId }

        if (index != -1) {

            val item = _carrito[index]

            _carrito[index] = item.copy(paraLlevar = !item.paraLlevar)

        }

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

        _descuentoManual = 0.0

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

}
