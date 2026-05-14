package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ComboProducto
import com.bocatta.pos.domain.model.ItemCombo
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.TipoCombo
import com.bocatta.pos.presentation.ui.components.BocattaEmptyState
import com.bocatta.pos.presentation.viewmodel.ComboViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabCombos(
    vm: ComboViewModel,
    allProducts: List<SalesInventoryProductV2>,
    onBack: () -> Unit
) {
    val combos by vm.combos.collectAsState()
    val feedback by vm.mensajeFeedback.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var comboEditar by remember { mutableStateOf<ComboProducto?>(null) }
    var comboEliminar by remember { mutableStateOf<ComboProducto?>(null) }
    var editandoToggleId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.cargar() }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHost.showSnackbar(it)
            vm.limpiarFeedback()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Combos", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo combo") },
                text = { Text("NUEVO COMBO", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (combos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                BocattaEmptyState(
                    icono = Icons.Default.Dashboard,
                    titulo = "Sin combos registrados",
                    descripcion = "Toca el botón + para crear tu primer combo"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(combos, key = { it.id }) { combo ->
                    ComboCard(
                        combo = combo,
                        allProducts = allProducts,
                        onToggleActivo = {
                            editandoToggleId = combo.id
                            vm.guardar(combo.copy(activo = !combo.activo))
                            editandoToggleId = null
                        },
                        toggling = editandoToggleId == combo.id,
                        onEdit = { comboEditar = combo },
                        onDelete = { comboEliminar = combo }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog || comboEditar != null) {
        DialogoCombo(
            comboInicial = comboEditar,
            allProducts = allProducts,
            onGuardar = { combo ->
                vm.guardar(combo)
                showDialog = false
                comboEditar = null
            },
            onDismiss = {
                showDialog = false
                comboEditar = null
            }
        )
    }

    comboEliminar?.let { combo ->
        AlertDialog(
            onDismissRequest = { comboEliminar = null },
            title = { Text("Eliminar combo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de eliminar \"${combo.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { vm.eliminar(combo.id); comboEliminar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("ELIMINAR", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { comboEliminar = null }) { Text("CANCELAR") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ComboCard(
    combo: ComboProducto,
    allProducts: List<SalesInventoryProductV2>,
    onToggleActivo: () -> Unit,
    toggling: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (combo.activo) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(combo.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(combo.nombre, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(
                            combo.tipo.name,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (toggling) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Switch(
                            checked = combo.activo,
                            onCheckedChange = { onToggleActivo() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val preciosStr = combo.precioVenta.entries.joinToString(" · ") { (suc, p) ->
                "${suc.replaceFirstChar { it.uppercase() }}: $${"%.0f".format(p)}"
            }
            if (preciosStr.isNotBlank()) {
                Text(preciosStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(6.dp))

            val productosNombres = combo.productos.mapNotNull { item ->
                allProducts.find { it.id == item.productoId }?.let { prod ->
                    "${prod.emoji} ${item.cantidad}x ${prod.nombre}"
                }
            }
            if (productosNombres.isNotEmpty()) {
                Text(
                    "Incluye: ${productosNombres.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoCombo(
    comboInicial: ComboProducto?,
    allProducts: List<SalesInventoryProductV2>,
    onGuardar: (ComboProducto) -> Unit,
    onDismiss: () -> Unit
) {
    var paso by remember { mutableIntStateOf(0) }
    var nombre by remember { mutableStateOf(comboInicial?.nombre ?: "") }
    var emoji by remember { mutableStateOf(comboInicial?.emoji ?: "🎁") }
    var categoria by remember { mutableStateOf(comboInicial?.categoria ?: "") }
    var tipo by remember { mutableStateOf(comboInicial?.tipo ?: TipoCombo.FIJO) }
    val preciosMap = remember { mutableStateMapOf<String, String>().apply {
        if (comboInicial != null) {
            comboInicial.precioVenta.forEach { (k, v) -> put(k, v.toString()) }
        } else {
            put("atlixco", "0.0"); put("metepec", "0.0")
        }
    }}
    val itemsCombo = remember { mutableStateListOf<ItemCombo>().apply {
        if (comboInicial != null) addAll(comboInicial.productos)
    } }
    var busquedaProducto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "${if (comboInicial != null) "Editar" else "Nuevo"} combo — paso ${paso + 1}/4",
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { (paso + 1) / 4f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (paso) {
                    0 -> {
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre del combo*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { emoji = it },
                            label = { Text("Emoji") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = categoria,
                            onValueChange = { categoria = it },
                            label = { Text("Categoría") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    1 -> {
                        Text("Precios por sucursal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        preciosMap.keys.toList().forEach { suc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    suc.replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.width(80.dp)
                                )
                                OutlinedTextField(
                                    value = preciosMap[suc] ?: "0",
                                    onValueChange = { preciosMap[suc] = it },
                                    label = { Text("Precio \$") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                    2 -> {
                        Text("Productos incluidos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = busquedaProducto,
                            onValueChange = { busquedaProducto = it },
                            label = { Text("Buscar producto...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true
                        )
                        val filtered = allProducts.filter {
                            busquedaProducto.isBlank() ||
                            it.nombre.contains(busquedaProducto, ignoreCase = true) ||
                            it.categoria.contains(busquedaProducto, ignoreCase = true)
                        }
                        filtered.forEach { prod ->
                            val existing = itemsCombo.find { it.productoId == prod.id }
                            val isSelected = existing != null
                            var cantidad by remember(prod.id) {
                                mutableStateOf(existing?.cantidad?.toString() ?: "1")
                            }
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                itemsCombo.add(
                                                    ItemCombo(
                                                        productoId = prod.id,
                                                        cantidad = cantidad.toIntOrNull() ?: 1,
                                                        fijo = true
                                                    )
                                                )
                                            } else {
                                                itemsCombo.removeAll { it.productoId == prod.id }
                                            }
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text("${prod.emoji} ${prod.nombre}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(prod.categoria, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        OutlinedTextField(
                                            value = cantidad,
                                            onValueChange = { v ->
                                                cantidad = v
                                                val idx = itemsCombo.indexOfFirst { it.productoId == prod.id }
                                                if (idx >= 0) {
                                                    itemsCombo[idx] = itemsCombo[idx].copy(
                                                        cantidad = v.toIntOrNull() ?: 1
                                                    )
                                                }
                                            },
                                            modifier = Modifier.width(60.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            label = { Text("Cant", fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("Tipo de combo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tipo == TipoCombo.FIJO,
                                onClick = { tipo = TipoCombo.FIJO },
                                label = { Text("FIJO") },
                                leadingIcon = {
                                    if (tipo == TipoCombo.FIJO) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                }
                            )
                            FilterChip(
                                selected = tipo == TipoCombo.CONFIGURABLE,
                                onClick = { tipo = TipoCombo.CONFIGURABLE },
                                label = { Text("CONFIGURABLE") },
                                leadingIcon = {
                                    if (tipo == TipoCombo.CONFIGURABLE) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Resumen del combo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("Productos: ${itemsCombo.size}", fontSize = 12.sp)
                                val precioStr = preciosMap.entries.joinToString(", ") { (s, p) ->
                                    "$s: \$${p.toDoubleOrNull()?.let { "%.2f".format(it) } ?: "0.00"}"
                                }
                                Text("Precios: $precioStr", fontSize = 12.sp)
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
                        val id = comboInicial?.id ?: ""
                        val combo = ComboProducto(
                            id = id,
                            nombre = nombre,
                            emoji = emoji,
                            categoria = categoria,
                            precioVenta = preciosMap.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
                            tipo = tipo,
                            productos = itemsCombo.toList(),
                            activo = comboInicial?.activo ?: true
                        )
                        onGuardar(combo)
                    },
                    enabled = nombre.isNotBlank()
                ) { Text("GUARDAR COMBO", fontWeight = FontWeight.Bold) }
                else -> Button(
                    onClick = { paso++ },
                    enabled = nombre.isNotBlank()
                ) { Text("SIGUIENTE →") }
            }
        },
        dismissButton = {
            when (paso) {
                0 -> TextButton(onClick = onDismiss) { Text("CANCELAR") }
                else -> TextButton(onClick = { paso-- }) { Text("← ANTERIOR") }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
