package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.viewmodel.GestionSucursalesViewModel
import com.bocatta.pos.presentation.viewmodel.SucursalInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionSucursalesScreen(
    vm: GestionSucursalesViewModel,
    onBack: () -> Unit
) {
    val snackbarHost = remember { SnackbarHostState() }
    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var sucursalAccion by remember { mutableStateOf<SucursalInfo?>(null) }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    // ── DIÁLOGO NUEVA SUCURSAL ─────────────────────────────────────────────
    if (mostrarDialogoNueva) {
        DialogNuevaSucursal(
            onConfirmar = { nombre, ciudad ->
                vm.crearSucursal(nombre, ciudad)
                mostrarDialogoNueva = false
            },
            onCancelar = { mostrarDialogoNueva = false }
        )
    }

    // ── DIÁLOGO DE ACCIONES ────────────────────────────────────────────────
    sucursalAccion?.let { suc ->
        AlertDialog(
            onDismissRequest = { sucursalAccion = null },
            title = { Text(suc.nombre, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ciudad: ${suc.ciudad}", style = MaterialTheme.typography.bodySmall)
                    Text("Estado: ${if (suc.activa) "✅ Activa" else "⛔ Inactiva"}", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Acciones disponibles:", style = MaterialTheme.typography.labelMedium)
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            vm.toggleActivarSucursal(suc.id, !suc.activa)
                            sucursalAccion = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (suc.activa) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (suc.activa) "Desactivar sucursal" else "Activar sucursal")
                    }
                    OutlinedButton(
                        onClick = {
                            vm.resetearInventarioSucursal(suc.id)
                            sucursalAccion = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Resetear inventario", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Resetear inventario a cero")
                    }
                    TextButton(
                        onClick = { sucursalAccion = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancelar") }
                }
            },
            dismissButton = {}
        )
    }

    // ── OVERLAY DE PROGRESO ────────────────────────────────────────────────
    if (vm.iniciandoSucursal) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("🏪 Abriendo sucursal...", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        vm.progresoMensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Esto tomará solo unos segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestión de Sucursales", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        Text("${vm.sucursales.size} sucursal(es) registradas", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoNueva = true }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Nueva sucursal", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { padding ->
        if (vm.sucursales.isEmpty() && !vm.cargando) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.StoreMallDirectory,
                        contentDescription = "Sin sucursales",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(0.3f)
                    )
                    Text("No hay sucursales registradas", color = MaterialTheme.colorScheme.outline)
                    Button(
                        onClick = { mostrarDialogoNueva = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Crear sucursal")
                        Spacer(Modifier.width(8.dp))
                        Text("Crear primera sucursal")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Apertura rápida", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Apertura en segundos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Al crear una sucursal, el sistema inicializa automáticamente el inventario, los contadores de tickets y la configuración de caja.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                            )
                        }
                    }
                }
            }

            items(vm.sucursales, key = { it.id }) { sucursal ->
                SucursalCard(
                    sucursal = sucursal,
                    onClick = { sucursalAccion = sucursal }
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { mostrarDialogoNueva = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = "Abrir sucursal")
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir nueva sucursal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SucursalCard(sucursal: SucursalInfo, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de estado
            Surface(
                color = if (sucursal.activa) MaterialTheme.colorScheme.primary.copy(0.15f) else MaterialTheme.colorScheme.error.copy(0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (sucursal.activa) Icons.Default.Store else Icons.Default.StoreMallDirectory,
                        contentDescription = if (sucursal.activa) "Sucursal activa" else "Sucursal inactiva",
                        tint = if (sucursal.activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sucursal.nombre, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (sucursal.activa) MaterialTheme.colorScheme.primary.copy(0.12f) else MaterialTheme.colorScheme.error.copy(0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            if (sucursal.activa) "ACTIVA" else "INACTIVA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sucursal.activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (sucursal.ciudad.isNotBlank()) {
                    Text(sucursal.ciudad, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                if (sucursal.creadaEn > 0L) {
                    Text(
                        "Creada: ${sdf.format(Date(sucursal.creadaEn))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = "Ver detalle", tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun DialogNuevaSucursal(
    onConfirmar: (nombre: String, ciudad: String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Nueva sucursal", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Nueva sucursal", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "El sistema inicializará automáticamente el inventario, la configuración de caja y los contadores de tickets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la sucursal*") },
                    placeholder = { Text("Ej: Atlixco, Metepec, Centro...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    supportingText = {
                        val id = remember(nombre) { nombre.lowercase(java.util.Locale.getDefault()).trim().replace(" ", "_").replace(Regex("[^a-z0-9_]") , "") }
                        if (nombre.isNotBlank()) Text("ID: $id", style = MaterialTheme.typography.labelSmall)
                    }
                )

                OutlinedTextField(
                    value = ciudad,
                    onValueChange = { ciudad = it },
                    label = { Text("Ciudad (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )

                // Resumen de lo que se va a crear
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Se inicializará:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("✅ Inventario local (todos los insumos en cero)", style = MaterialTheme.typography.bodySmall)
                        Text("✅ Contador de tickets independiente", style = MaterialTheme.typography.bodySmall)
                        Text("✅ Configuración de caja", style = MaterialTheme.typography.bodySmall)
                        Text("✅ Catálogo de productos vinculado", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(nombre.trim(), ciudad.trim()) },
                enabled = nombre.length >= 2,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Crear sucursal", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Crear y abrir sucursal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

