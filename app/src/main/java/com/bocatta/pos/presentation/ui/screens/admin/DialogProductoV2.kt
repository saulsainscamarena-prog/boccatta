package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.ui.components.BuscadorSelector
import com.bocatta.pos.presentation.ui.components.ItemSeleccionable
import com.bocatta.pos.presentation.ui.components.SeccionConfiguracion
import com.bocatta.pos.presentation.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DialogProducto(
    productoInicial: SalesInventoryProductV2?,
    vm: AdminViewModel,
    onGuardar: (SalesInventoryProductV2, RecetaV2) -> Unit,
    onCancelar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var paso by remember { mutableIntStateOf(0) }

    // ─── Paso 1: Info básica ───
    var nombre by remember { mutableStateOf(productoInicial?.nombre ?: "") }
    var emoji by remember { mutableStateOf(productoInicial?.emoji ?: "🍩") }
    var categoria by remember { mutableStateOf(productoInicial?.categoria ?: "") }
    var expandCat by remember { mutableStateOf(false) }
    var nuevaCat by remember { mutableStateOf("") }
    var subcategoria by remember { mutableStateOf(productoInicial?.subcategoria ?: "") }
    var tipoProducto by remember { mutableStateOf(productoInicial?.tipoProducto ?: "PREPARADO") }
    var esCombo by remember { mutableStateOf(productoInicial?.esCombo ?: false) }
    var comboMode by remember { mutableStateOf(productoInicial?.comboMode ?: "COMBO_ONLY") }
    var productosCombo by remember { mutableStateOf(productoInicial?.productosCombo ?: emptyList<String>()) }

    // ─── Paso 2: Precios + Config ───
    val preciosMap = remember { mutableStateMapOf<String, String>().apply {
        if (productoInicial != null) productoInicial.precioVenta.forEach { (k, v) -> put(k, v.toString()) }
        else { put("atlixco", "0.0"); put("metepec", "0.0") }
    }}
    var configSchema by remember { mutableStateOf(productoInicial?.configSchema ?: emptyList()) }
    var editandoKey = remember { mutableStateOf("") }
    var editandoTitle = remember { mutableStateOf("") }
    var editandoType = remember { mutableStateOf("SINGLE_CHIP") }
    var editandoOptions = remember { mutableStateOf("") }
    var editandoPremium = remember { mutableStateMapOf<String, Double>() }
    val editandoDescuentos = remember { mutableStateMapOf<String, DescuentoOpcion>() }
    var showGroupEditor by remember { mutableStateOf(false) }
    var consumibles by remember { mutableStateOf(productoInicial?.consumiblesAsociados ?: emptyList()) }

    // ─── Paso 3: Inventario ───
    val ingredientes = remember { mutableStateListOf<IngredienteReceta>() }
    var requiereReceta by remember { mutableStateOf(false) }
    var rendimientoTanda by remember { mutableStateOf(productoInicial?.rendimientoTanda?.toString() ?: "1") }
    var unidadCompra by remember { mutableStateOf(productoInicial?.unidadCompra ?: "") }
    var pesoPorcion by remember { mutableStateOf(productoInicial?.pesoPorcion?.toString() ?: "") }
    var sePorciona by remember { mutableStateOf(productoInicial?.pesoPorcion != null) }
    var porPeso by remember { mutableStateOf(productoInicial?.porPeso ?: false) }

    val productosDisponibles = vm.productos.filter { it.id != productoInicial?.id }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Column {
                Text("${if (productoInicial != null) "Editar" else "Nuevo"} producto — paso ${paso + 1}/4", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { (paso + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (paso) {
                    0 -> {
                        SeccionConfiguracion(titulo = "INFORMACIÓN BÁSICA", ayuda = "Nombre visible en el POS. La categoría agrupa productos similares y aparece como pestaña en ventas.") {
                            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            ExposedDropdownMenuBox(expanded = expandCat, onExpandedChange = { expandCat = it }) {
                                OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría*") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandCat) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                ExposedDropdownMenu(expanded = expandCat, onDismissRequest = { expandCat = false }) {
                                    vm.categorias.forEach { cat -> DropdownMenuItem(text = { Text(cat.nombre) }, onClick = { categoria = cat.nombre; expandCat = false }) }
                                    HorizontalDivider()
                                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(value = nuevaCat, onValueChange = { nuevaCat = it }, label = { Text("Nueva") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                                        IconButton(onClick = { if (nuevaCat.isNotBlank()) { vm.agregarCategoria(nuevaCat); nuevaCat = "" } }) { Icon(Icons.Default.Add, "Agregar") }
                                    }
                                }
                            }
                            OutlinedTextField(value = subcategoria, onValueChange = { subcategoria = it }, label = { Text("Subcategoría (opcional)") }, placeholder = { Text("Ej: REFRESCOS, CREPAS DULCES") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        }
                        SeccionConfiguracion(titulo = "TIPO DE PRODUCTO", ayuda = "🔨 Preparado = tiene receta y se produce. 📦 Comprado = se compra y revende. 🧩 Servicio = no descuenta inventario.") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("PREPARADO" to "🔨", "COMPRADO" to "📦", "SERVICIO" to "🧩").forEach { (t, e) ->
                                    FilterChip(selected = tipoProducto == t, onClick = { tipoProducto = t }, label = { Text("$e $t", fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                        SeccionConfiguracion(titulo = "COMBO", ayuda = "Si activas, el producto se vende como paquete. Puedes elegir si los productos también se venden por separado.") {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("Es combo", modifier = Modifier.weight(1f)); Switch(checked = esCombo, onCheckedChange = { esCombo = it }) }
                            if (esCombo) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("COMBO_ONLY" to "Solo combo", "COMBO_AND_INDIVIDUAL" to "Combo + individual").forEach { (m, l) ->
                                        FilterChip(selected = comboMode == m, onClick = { comboMode = m }, label = { Text(l, fontSize = 11.sp) })
                                    }
                                }
                                productosCombo.forEach { pid ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(productosDisponibles.find { it.id == pid }?.nombre ?: pid, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { productosCombo = productosCombo - pid }) { Icon(Icons.Default.Close, "Cerrar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                                    }
                                }
                                var expandProd by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expandProd, onExpandedChange = { expandProd = it }) {
                                    OutlinedTextField(value = "", onValueChange = {}, readOnly = true, placeholder = { Text("Agregar producto...") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandProd) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                    ExposedDropdownMenu(expanded = expandProd, onDismissRequest = { expandProd = false }) {
                                        productosDisponibles.filter { it.id !in productosCombo }.forEach { prod ->
                                            DropdownMenuItem(text = { Text(prod.nombre) }, onClick = { productosCombo = productosCombo + prod.id; expandProd = false })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        SeccionConfiguracion(titulo = "PRECIOS POR SUCURSAL", ayuda = "Define el precio de venta para cada sucursal.") {
                            preciosMap.keys.toList().forEach { suc ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(suc.replaceFirstChar { it.uppercase() }, modifier = Modifier.width(70.dp))
                                    OutlinedTextField(value = preciosMap[suc] ?: "0", onValueChange = { preciosMap[suc] = it }, label = { Text("Precio \$") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                                }
                            }
                            val tienePrecioCero = preciosMap.values.any { it.toDoubleOrNull() == null || it.toDoubleOrNull() == 0.0 }
                            if (tienePrecioCero) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Advertencia: Al menos una sucursal tiene precio de $0.00 o inválido.",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                        SeccionConfiguracion(titulo = "OPCIONES PARA EL CLIENTE (ConfigSchema)", ayuda = "Opciones que el cliente elige al comprar (salsas, toppings, extras). NO son productos ni ingredientes.", ejemplos = "SALSA: [Verde, Roja], TOPPINGS: [Queso, Cebolla]", contraejemplos = "NO: precios, recetas, consumibles") {
                            configSchema.forEachIndexed { idx, g ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(g.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { configSchema = configSchema.toMutableList().also { it.removeAt(idx) } }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                        }
                                        Text("Tipo: ${labelConfigTypeProducto(g.type)} | Opciones: ${g.options.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                            Button(onClick = { showGroupEditor = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Text("+ Agregar opción") }
                            if (showGroupEditor) {
                                AlertDialog(onDismissRequest = { showGroupEditor = false }, title = { Text("Configurar opción") }, text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = editandoKey.value, onValueChange = { editandoKey.value = it }, label = { Text("Identificador (key)*") }, placeholder = { Text("salsa, toppings, extras") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                                        OutlinedTextField(value = editandoTitle.value, onValueChange = { editandoTitle.value = it }, label = { Text("Título visible*") }, placeholder = { Text("SALSA, EXTRAS") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                                        Text("Tipo:", style = MaterialTheme.typography.labelSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            ConfigFieldType.entries.forEach { t ->
                                                FilterChip(
                                                    selected = editandoType.value == t.name,
                                                    onClick = { editandoType.value = t.name },
                                                    label = { Text(labelConfigTypeProducto(t), fontSize = 10.sp) },
                                                    modifier = Modifier.height(36.dp)
                                                )
                                            }
                                        }
                                        Text("OPCIONES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text("Escribe cada opción y marca si tiene costo extra (Premium):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                        val optsList = editandoOptions.value.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        optsList.forEach { opt ->
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(opt, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                                    var isPrem by remember(opt) { mutableStateOf(editandoPremium.containsKey(opt)) }
                                                    Switch(checked = isPrem, onCheckedChange = { v ->
                                                        isPrem = v
                                                        if (v) { if (!editandoPremium.containsKey(opt)) editandoPremium[opt] = 10.0 }
                                                        else editandoPremium.remove(opt)
                                                    }, modifier = Modifier.height(28.dp))
                                                    if (isPrem) {
                                                        OutlinedTextField(
                                                            value = if (editandoPremium.containsKey(opt)) "%.0f".format(editandoPremium[opt]) else "10",
                                                            onValueChange = { editandoPremium[opt] = it.toDoubleOrNull() ?: 10.0 },
                                                            modifier = Modifier.width(60.dp).height(40.dp),
                                                            label = { Text("\$", fontSize = 9.sp) },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            singleLine = true,
                                                            textStyle = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                    var hasInsumo by remember(opt) { mutableStateOf(editandoDescuentos.containsKey(opt)) }
                                                    IconButton(onClick = {
                                                        if (hasInsumo) {
                                                            editandoDescuentos.remove(opt)
                                                            hasInsumo = false
                                                        } else {
                                                            editandoDescuentos[opt] = DescuentoOpcion("", 10.0, "g")
                                                            hasInsumo = true
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Inventory,
                                                            contentDescription = "Mapear Inventario",
                                                            tint = if (hasInsumo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                var hasInsumoState by remember(opt) { mutableStateOf(editandoDescuentos.containsKey(opt)) }
                                                LaunchedEffect(editandoDescuentos.containsKey(opt)) {
                                                    hasInsumoState = editandoDescuentos.containsKey(opt)
                                                }
                                                if (hasInsumoState) {
                                                    val currentDesc = editandoDescuentos[opt] ?: DescuentoOpcion("", 10.0, "g")
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        var expandInsumo by remember { mutableStateOf(false) }
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            val selectedInsumo = vm.insumosMaestros.find { it.id == currentDesc.insumoId }
                                                            OutlinedButton(
                                                                onClick = { expandInsumo = true },
                                                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                                            ) {
                                                                Text(selectedInsumo?.nombre ?: "Vincular Insumo...", fontSize = 11.sp)
                                                            }
                                                            DropdownMenu(
                                                                expanded = expandInsumo,
                                                                onDismissRequest = { expandInsumo = false }
                                                            ) {
                                                                vm.insumosMaestros.forEach { ins ->
                                                                    DropdownMenuItem(
                                                                        text = { Text(ins.nombre, fontSize = 12.sp) },
                                                                        onClick = {
                                                                            editandoDescuentos[opt] = currentDesc.copy(insumoId = ins.id, unidad = ins.unidadBase)
                                                                            expandInsumo = false
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        OutlinedTextField(
                                                            value = if (currentDesc.cantidad == 0.0) "" else currentDesc.cantidad.toString(),
                                                            onValueChange = {
                                                                val v = it.toDoubleOrNull() ?: 0.0
                                                                editandoDescuentos[opt] = currentDesc.copy(cantidad = v)
                                                            },
                                                            label = { Text("Cant", fontSize = 9.sp) },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier.width(65.dp).height(45.dp),
                                                            textStyle = MaterialTheme.typography.labelSmall,
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        OutlinedTextField(
                                                            value = currentDesc.unidad,
                                                            onValueChange = {
                                                                editandoDescuentos[opt] = currentDesc.copy(unidad = it)
                                                            },
                                                            label = { Text("Unid", fontSize = 9.sp) },
                                                            modifier = Modifier.width(55.dp).height(45.dp),
                                                            textStyle = MaterialTheme.typography.labelSmall,
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        OutlinedTextField(value = editandoOptions.value, onValueChange = { editandoOptions.value = it; editandoPremium.clear(); editandoDescuentos.clear() }, label = { Text("Opciones (separadas por coma)") }, placeholder = { Text("Oreja, Bombón, Nuez, Fresa") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                                        Text("💡 Las opciones marcadas como Premium generan un cargo extra al cliente.", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }, confirmButton = {
                                    Button(onClick = {
                                        val opts = editandoOptions.value.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        configSchema = configSchema + ConfigOptionGroup(
                                            key = editandoKey.value, title = editandoTitle.value,
                                            type = try { ConfigFieldType.valueOf(editandoType.value) } catch (_: Exception) { ConfigFieldType.SINGLE_CHIP },
                                            options = opts,
                                            preciosExtra = editandoPremium.toMap(),
                                            descuentosInsumo = editandoDescuentos.toMap()
                                        )
                                        showGroupEditor = false; editandoKey.value = ""; editandoTitle.value = ""; editandoOptions.value = ""; editandoPremium.clear(); editandoDescuentos.clear()
                                    }, enabled = editandoKey.value.isNotBlank() && editandoTitle.value.isNotBlank()) { Text("Agregar") }
                                }, dismissButton = { TextButton(onClick = { showGroupEditor = false; editandoKey.value = ""; editandoTitle.value = ""; editandoOptions.value = ""; editandoPremium.clear(); editandoDescuentos.clear() }) { Text("Cancelar") } }, shape = RoundedCornerShape(20.dp))
                            }
                        }
                        HorizontalDivider()
                        SeccionConfiguracion(titulo = "CONSUMIBLES", ayuda = "Se descuentan del inventario al vender. NO son ingredientes ni opciones del cliente.", ejemplos = "Charola, tenedor, servilleta, vaso, domo, cuchara", contraejemplos = "NO: salsas, queso, toppings, masa") {
                            val insumosSeleccionados = vm.insumosMaestros.filter { ins -> consumibles.any { it.consumibleId == ins.id } }
                            BuscadorSelector(
                                label = "consumibles", items = vm.insumosMaestros,
                                selectedItems = insumosSeleccionados.map { ItemSeleccionable(id = it.id, nombre = it.nombre, data = it, cantidad = consumibles.find { c -> c.consumibleId == it.id }?.cantidad ?: 1.0, unidad = consumibles.find { c -> c.consumibleId == it.id }?.unidad ?: "pz") },
                                filterPredicate = { ins, q -> ins.nombre.lowercase().contains(q.lowercase()) },
                                itemLabel = { it.nombre },
                                onAddItem = { insumo -> if (consumibles.none { it.consumibleId == insumo.id }) consumibles = consumibles + ConsumibleRequerido(consumibleId = insumo.id, cantidad = 1.0, unidad = "pz") },
                                onRemoveItem = { insumo -> consumibles = consumibles.filter { it.consumibleId != insumo.id } }
                            )
                        }
                    }
                    2 -> {
                        SeccionConfiguracion(titulo = "INVENTARIO", ayuda = when (tipoProducto) {
                            "PREPARADO" -> "🔨 Tiene receta: los ingredientes se descuentan al vender según el rendimiento por tanda."
                            "COMPRADO" -> "📦 Se compra y revende. Define la unidad de compra y si se porciona."
                            else -> "🧩 No descuenta inventario."
                        }) {
                            when (tipoProducto) {
                                "PREPARADO" -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Descuenta inventario", modifier = Modifier.weight(1f)); Switch(checked = requiereReceta, onCheckedChange = { requiereReceta = it }) }
                                    OutlinedTextField(value = rendimientoTanda, onValueChange = { rendimientoTanda = it.filter { c -> c.isDigit() } }, label = { Text("Rendimiento por tanda") }, placeholder = { Text("Ej: 30") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                                    if (requiereReceta) {
                                        ingredientes.forEachIndexed { i, ing ->
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(vm.insumosMaestros.find { it.id == ing.insumoId }?.nombre ?: ing.insumoId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                                OutlinedTextField(value = ing.cantidad.toString(), onValueChange = { v -> ingredientes[i] = ing.copy(cantidad = v.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.width(60.dp), shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                                                IconButton(onClick = { ingredientes.removeAt(i) }) { Icon(Icons.Default.Close, "Cerrar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                            }
                                        }
                                        val insumosIngredientes = vm.insumosMaestros.filter { ins -> ingredientes.any { it.insumoId == ins.id } }
                                    BuscadorSelector(
                                        label = "ingredientes", items = vm.insumosMaestros,
                                        selectedItems = insumosIngredientes.map { ItemSeleccionable(id = it.id, nombre = it.nombre, data = it, cantidad = ingredientes.find { ing -> ing.insumoId == it.id }?.cantidad ?: 0.0, unidad = it.unidadBase) },
                                        filterPredicate = { ins, q -> ins.nombre.lowercase().contains(q.lowercase()) },
                                        itemLabel = { it.nombre },
                                        onAddItem = { ins -> if (ingredientes.none { it.insumoId == ins.id }) ingredientes.add(IngredienteReceta(insumoId = ins.id, nombreInsumo = ins.nombre, unidad = ins.unidadBase)) },
                                        onRemoveItem = { ins -> ingredientes.removeAll { it.insumoId == ins.id } }
                                        )
                                    }
                                }
                                "COMPRADO" -> {
                                    OutlinedTextField(value = unidadCompra, onValueChange = { unidadCompra = it }, label = { Text("Unidad de compra") }, placeholder = { Text("kg, pza, caja, bolsa") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text("¿Se porciona?", modifier = Modifier.weight(1f)); Switch(checked = sePorciona, onCheckedChange = { sePorciona = it }) }
                                    if (sePorciona) {
                                        OutlinedTextField(value = pesoPorcion, onValueChange = { pesoPorcion = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Peso/tamaño por porción") }, placeholder = { Text("Ej: 250 (g)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("¿Vender por peso?")
                                            Text("El precio se interpreta como \$/kg. El operador ingresa gramos al vender.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Switch(checked = porPeso, onCheckedChange = { porPeso = it })
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("RESUMEN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("$emoji $nombre", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("${categoria.uppercase()} ${if (subcategoria.isNotBlank()) "> ${subcategoria.uppercase()}" else ""} · $tipoProducto", color = MaterialTheme.colorScheme.outline)
                                HorizontalDivider()
                                preciosMap.forEach { (suc, v) -> Text("${suc.replaceFirstChar { it.uppercase() }}: \$${v}", fontSize = 14.sp) }
                                if (esCombo) Text("Combo ($comboMode) · ${productosCombo.size} productos", color = MaterialTheme.colorScheme.primary)
                                if (configSchema.isNotEmpty()) Text("⚙️ ${configSchema.size} grupo(s) de configuración", fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                                if (consumibles.isNotEmpty()) Text("📦 ${consumibles.size} consumible(s)", fontSize = 13.sp)
                                if (tipoProducto == "PREPARADO" && requiereReceta) Text("🥘 ${ingredientes.size} ingredientes · ${"$rendimientoTanda"} p/tanda", fontSize = 13.sp)
                                if (tipoProducto == "COMPRADO") Text("📦 $unidadCompra${if (sePorciona) " → ${pesoPorcion}g/porción" else ""}${if (porPeso) " · ⚖️ venta por peso" else ""}", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (paso) {
                3 -> Button(
                    onClick = {
                        val id = productoInicial?.id ?: vm.generarNuevoProductoId()
                        val prod = SalesInventoryProductV2(
                            id = id, nombre = nombre, emoji = emoji, categoria = categoria, subcategoria = subcategoria,
                            tipoProducto = tipoProducto,
                            precioVenta = preciosMap.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
                            esCombo = esCombo, comboMode = comboMode, productosCombo = if (esCombo) productosCombo else emptyList(),
                            configSchema = configSchema, consumiblesAsociados = consumibles,
                            rendimientoTanda = rendimientoTanda.toIntOrNull() ?: 1,
                            unidadCompra = unidadCompra,
                            pesoPorcion = if (sePorciona) pesoPorcion.toDoubleOrNull() else null,
                            porPeso = porPeso
                        )
                        val receta = if (requiereReceta && ingredientes.isNotEmpty())
                            RecetaV2(id = "receta_$id", nombre = "$nombre Receta", productoId = id, ingredientes = ingredientes)
                        else RecetaV2()
                        onGuardar(prod, receta)
                    },
                    enabled = nombre.length >= 2 && categoria.isNotBlank()
                ) { Text("GUARDAR PRODUCTO", fontWeight = FontWeight.Black) }
                else -> Button(onClick = { paso++ }, enabled = nombre.length >= 2 && categoria.isNotBlank()) { Text("Siguiente →") }
            }
        },
        dismissButton = {
            when (paso) {
                0 -> TextButton(onClick = onCancelar) { Text("Cancelar") }
                else -> TextButton(onClick = { paso-- }) { Text("← Anterior") }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

private fun labelConfigTypeProducto(type: ConfigFieldType): String = when (type) {
    ConfigFieldType.SINGLE_CHIP -> "Una opcion"
    ConfigFieldType.MULTI_CHIP -> "Varias opciones"
    ConfigFieldType.MULTI_CHECKBOX -> "Casillas"
    ConfigFieldType.TEXT -> "Texto libre"
}
