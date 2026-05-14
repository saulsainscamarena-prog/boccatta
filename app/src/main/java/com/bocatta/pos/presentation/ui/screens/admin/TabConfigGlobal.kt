package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bocatta.pos.presentation.ui.components.BocattaEmptyState
import com.bocatta.pos.presentation.viewmodel.ConfigGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TabConfigGlobal(
    vm: ConfigGlobalViewModel,
    allProducts: List<SalesInventoryProductV2>,
    allCategories: List<CategoriaProducto>
) {
    val grupos by vm.grupos.collectAsState()
    val feedback by vm.mensajeFeedback.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var grupoEditar by remember { mutableStateOf<GrupoConfiguracionGlobal?>(null) }
    var grupoEliminar by remember { mutableStateOf<GrupoConfiguracionGlobal?>(null) }
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo grupo") },
                text = { Text("NUEVO GRUPO", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (grupos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                BocattaEmptyState(
                    icono = Icons.Default.Dashboard,
                    titulo = "Sin grupos de configuración",
                    descripcion = "Toca el botón + para crear tu primer grupo"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(grupos, key = { it.id }) { grupo ->
                    GrupoConfigCard(
                        grupo = grupo,
                        allProducts = allProducts,
                        onToggleActivo = {
                            editandoToggleId = grupo.id
                            vm.guardar(grupo.copy(activo = !grupo.activo))
                            editandoToggleId = null
                        },
                        toggling = editandoToggleId == grupo.id,
                        onEdit = { grupoEditar = grupo },
                        onDelete = { grupoEliminar = grupo }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDialog || grupoEditar != null) {
        DialogoGrupoConfig(
            grupoInicial = grupoEditar,
            allProducts = allProducts,
            allCategories = allCategories,
            onGuardar = { grupo ->
                vm.guardar(grupo)
                showDialog = false
                grupoEditar = null
            },
            onDismiss = {
                showDialog = false
                grupoEditar = null
            }
        )
    }

    grupoEliminar?.let { grupo ->
        AlertDialog(
            onDismissRequest = { grupoEliminar = null },
            title = { Text("Eliminar grupo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de eliminar \"${grupo.title}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { vm.eliminar(grupo.id); grupoEliminar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("ELIMINAR", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { grupoEliminar = null }) { Text("CANCELAR") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun GrupoConfigCard(
    grupo: GrupoConfiguracionGlobal,
    allProducts: List<SalesInventoryProductV2>,
    onToggleActivo: () -> Unit,
    toggling: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (grupo.activo) MaterialTheme.colorScheme.surface
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(grupo.key, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        grupo.title,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (toggling) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Switch(
                            checked = grupo.activo,
                            onCheckedChange = { onToggleActivo() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        grupo.type.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        grupo.modoAsignacion.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val count = contarProductosAfectados(grupo, allProducts)
            Text(
                "$count producto${if (count != 1) "s" else ""} afectado${if (count != 1) "s" else ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

private fun contarProductosAfectados(
    grupo: GrupoConfiguracionGlobal,
    allProducts: List<SalesInventoryProductV2>
): Int {
    val excluidos = grupo.excluirProductos.toSet()
    val base = when (grupo.modoAsignacion) {
        ModoAsignacion.GENERAL -> allProducts
        ModoAsignacion.POR_CATEGORIA -> allProducts.filter { it.categoria in grupo.categorias }
        ModoAsignacion.PERSONALIZADO -> allProducts.filter { it.id in grupo.productos }
    }
    return base.count { it.id !in excluidos }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DialogoGrupoConfig(
    grupoInicial: GrupoConfiguracionGlobal?,
    allProducts: List<SalesInventoryProductV2>,
    allCategories: List<CategoriaProducto>,
    onGuardar: (GrupoConfiguracionGlobal) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf(grupoInicial?.key ?: "") }
    var title by remember { mutableStateOf(grupoInicial?.title ?: "") }
    var type by remember { mutableStateOf(grupoInicial?.type ?: ConfigFieldType.SINGLE_CHIP) }
    var modoAsignacion by remember { mutableStateOf(grupoInicial?.modoAsignacion ?: ModoAsignacion.GENERAL) }
    var categorias by remember { mutableStateOf(grupoInicial?.categorias ?: emptyList()) }
    var productos by remember { mutableStateOf(grupoInicial?.productos ?: emptyList()) }
    var excluirProductos by remember { mutableStateOf(grupoInicial?.excluirProductos ?: emptyList()) }
    var optionsText by remember { mutableStateOf(grupoInicial?.opciones?.joinToString(", ") ?: "") }
    var source by remember { mutableStateOf(grupoInicial?.source ?: "MANUAL") }
    var catalogo by remember { mutableStateOf(grupoInicial?.catalogo ?: "") }
    var required by remember { mutableStateOf(grupoInicial?.required ?: false) }
    var multiMax by remember { mutableStateOf(grupoInicial?.multiMax?.toString() ?: "") }
    var expandedType by remember { mutableStateOf(false) }
    var expandedCatalogo by remember { mutableStateOf(false) }
    var busquedaProducto by remember { mutableStateOf("") }
    var busquedaExcluir by remember { mutableStateOf("") }

    val isEditing = grupoInicial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${if (isEditing) "Editar" else "Nuevo"} grupo de configuración",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    label = { Text("Identificador (key)*") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título visible*") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = it }) {
                    OutlinedTextField(
                        value = type.name.replace("_", " "), onValueChange = {}, readOnly = true,
                        label = { Text("Tipo de campo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedType) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        ConfigFieldType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name.replace("_", " ")) },
                                onClick = { type = t; expandedType = false }
                            )
                        }
                    }
                }

                Text("Modo de asignación", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ModoAsignacion.entries.forEach { modo ->
                        FilterChip(
                            selected = modoAsignacion == modo,
                            onClick = {
                                modoAsignacion = modo
                                if (modo != ModoAsignacion.POR_CATEGORIA) categorias = emptyList()
                                if (modo != ModoAsignacion.PERSONALIZADO) productos = emptyList()
                            },
                            label = { Text(modo.name.replace("_", " "), fontSize = 11.sp) },
                            leadingIcon = {
                                if (modoAsignacion == modo) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            }
                        )
                    }
                }

                if (modoAsignacion == ModoAsignacion.POR_CATEGORIA) {
                    Text("Categorías aplicables", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    allCategories.forEach { cat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = cat.nombre in categorias,
                                onCheckedChange = { checked ->
                                    categorias = if (checked) categorias + cat.nombre else categorias - cat.nombre
                                }
                            )
                            Text(cat.nombre, fontSize = 13.sp)
                        }
                    }
                }

                if (modoAsignacion == ModoAsignacion.PERSONALIZADO) {
                    Text("Productos asignados", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = busquedaProducto, onValueChange = { busquedaProducto = it },
                        label = { Text("Buscar producto...") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    val filtered = allProducts.filter {
                        busquedaProducto.isBlank() || it.nombre.contains(busquedaProducto, ignoreCase = true)
                    }
                    filtered.take(20).forEach { prod ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = prod.id in productos,
                                onCheckedChange = { checked ->
                                    productos = if (checked) productos + prod.id else productos - prod.id
                                }
                            )
                            Text("${prod.emoji} ${prod.nombre}", fontSize = 12.sp)
                        }
                    }
                    if (filtered.size > 20) {
                        Text(
                            "... y ${filtered.size - 20} más",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Text("Fuente de opciones", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = source == "MANUAL",
                        onClick = { source = "MANUAL" },
                        label = { Text("MANUAL", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = source == "CATALOGO",
                        onClick = { source = "CATALOGO" },
                        label = { Text("CATÁLOGO", fontSize = 11.sp) }
                    )
                }

                if (source == "MANUAL") {
                    OutlinedTextField(
                        value = optionsText, onValueChange = { optionsText = it },
                        label = { Text("Opciones (separadas por coma)") },
                        placeholder = { Text("ej: Salsa Verde, Salsa Roja") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    ExposedDropdownMenuBox(expanded = expandedCatalogo, onExpandedChange = { expandedCatalogo = it }) {
                        OutlinedTextField(
                            value = catalogo, onValueChange = {}, readOnly = true,
                            label = { Text("Tipo de catálogo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCatalogo) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedCatalogo, onDismissRequest = { expandedCatalogo = false }) {
                            TipoCatalogo.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name.replace("_", " ")) },
                                    onClick = { catalogo = t.name; expandedCatalogo = false }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Requerido", modifier = Modifier.weight(1f))
                    Switch(checked = required, onCheckedChange = { required = it })
                }

                OutlinedTextField(
                    value = multiMax, onValueChange = { multiMax = it.filter { c -> c.isDigit() } },
                    label = { Text("MultiMax (máximo selecciones)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (isEditing) {
                    HorizontalDivider()
                    Text("Productos excluidos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = busquedaExcluir, onValueChange = { busquedaExcluir = it },
                        label = { Text("Buscar producto para excluir...") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    val excluirFiltered = allProducts.filter {
                        busquedaExcluir.isBlank() || it.nombre.contains(busquedaExcluir, ignoreCase = true)
                    }
                    excluirFiltered.filter { it.id !in excluirProductos }.take(10).forEach { prod ->
                        TextButton(onClick = { excluirProductos = excluirProductos + prod.id }) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${prod.emoji} ${prod.nombre}", fontSize = 12.sp)
                        }
                    }
                    excluirProductos.forEach { prodId ->
                        val prod = allProducts.find { it.id == prodId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${prod?.emoji ?: ""} ${prod?.nombre ?: prodId}",
                                modifier = Modifier.weight(1f), fontSize = 12.sp
                            )
                            IconButton(onClick = { excluirProductos = excluirProductos - prodId }) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grupo = GrupoConfiguracionGlobal(
                        id = grupoInicial?.id ?: "",
                        key = key,
                        title = title,
                        type = type,
                        modoAsignacion = modoAsignacion,
                        categorias = if (modoAsignacion == ModoAsignacion.POR_CATEGORIA) categorias else emptyList(),
                        productos = if (modoAsignacion == ModoAsignacion.PERSONALIZADO) productos else emptyList(),
                        excluirProductos = excluirProductos,
                        opciones = if (source == "MANUAL") optionsText.split(",").map { it.trim() }.filter { it.isNotBlank() } else emptyList(),
                        source = source,
                        catalogo = if (source == "CATALOGO") catalogo else null,
                        required = required,
                        multiMax = multiMax.toIntOrNull(),
                        activo = grupoInicial?.activo ?: true,
                        creadoEn = grupoInicial?.creadoEn ?: System.currentTimeMillis(),
                        actualizadoEn = System.currentTimeMillis()
                    )
                    onGuardar(grupo)
                },
                enabled = key.isNotBlank() && title.isNotBlank()
            ) { Text("GUARDAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
