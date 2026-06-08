package com.bocatta.pos.presentation.ui.screens.clientes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.presentation.viewmodel.ClienteViewModel
import com.bocatta.pos.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(vm: ClienteViewModel, onBack: () -> Unit) {
    val snackbarHost = remember { SnackbarHostState() }
    var mostrarRegistro by remember { mutableStateOf(false) }
    var clienteEditar by remember { mutableStateOf<ClienteV2?>(null) }
    var busquedaLocal by remember { mutableStateOf("") }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    if (mostrarRegistro || clienteEditar != null) {
        DialogCliente(
            clienteInicial = clienteEditar,
            onGuardar = { nombre, tel ->
                clienteEditar?.let {
                    vm.editarCliente(it.copy(nombre = nombre))
                } ?: run {
                    vm.registrarCliente(nombre, tel) {}
                }
                mostrarRegistro = false; clienteEditar = null
            },
            onCancelar = { mostrarRegistro = false; clienteEditar = null; vm.mensajeError = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GESTION DE CLIENTES", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Fidelizacion Industrial V2", color = MaterialTheme.colorScheme.onSecondary.copy(0.7f), fontSize = 10.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.onSecondary) } },
                actions = {
                    IconButton(onClick = { mostrarRegistro = true }) {
                        Icon(Icons.Default.PersonAdd, "Registrar cliente", tint = MaterialTheme.colorScheme.onSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = busquedaLocal,
                onValueChange = { busquedaLocal = it; if (it.length >= 3) vm.buscarPorTelefono(it) },
                placeholder = { Text("Buscar por nombre o telefono...") },
                leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.height(20.dp))

            val numDescuentos = vm.clientes.count { it.visitasCicloActual == 5 }
            val totalClientes = vm.clientes.size

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricLealtadCard("DESCUENTOS", numDescuentos.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricLealtadCard("EN REGISTRO", totalClientes.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }

            val clientesMostrados = if (busquedaLocal.isBlank()) vm.clientes else vm.clientes.filter {
                it.nombre.contains(busquedaLocal, ignoreCase = true) || it.telefono.contains(busquedaLocal)
            }

            if (clientesMostrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Inicia una busqueda o registra uno nuevo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(clientesMostrados, key = { it.telefono }) { cliente ->
                        ClienteIndustrialCard(
                            cliente = cliente,
                            onEditar = { clienteEditar = cliente },
                            onEliminar = { vm.eliminarCliente(cliente) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricLealtadCard(titulo: String, valor: String, color: Color, modifier: Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valor, fontWeight = FontWeight.Black, color = color, style = MaterialTheme.typography.headlineSmall)
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ClienteIndustrialCard(cliente: ClienteV2, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val esVIP = cliente.visitasCicloActual >= 5
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(if (esVIP) MaterialTheme.colorScheme.primary.copy(0.1f) else MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text(cliente.nombre.take(1).uppercase(java.util.Locale.getDefault()), color = if (esVIP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cliente.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(cliente.telefono, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(5) { i ->
                        val active = i < cliente.visitasCicloActual
                        val colorStep = if (active) {
                            if (cliente.visitasCicloActual >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.outlineVariant
                        Box(Modifier.height(4.dp).weight(1f).background(colorStep, CircleShape))
                    }
                }
                if (esVIP) Text("REGALO LISTO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Row {
                IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DialogCliente(clienteInicial: ClienteV2?, onGuardar: (String, String) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf(clienteInicial?.nombre ?: "") }
    var telefono by remember { mutableStateOf(clienteInicial?.telefono ?: "") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (clienteInicial != null) "EDITAR PERFIL" else "NUEVO CLIENTE", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), readOnly = clienteInicial != null)
            }
        },
        confirmButton = {
            Button(enabled = nombre.isNotBlank() && telefono.length >= 10, onClick = { onGuardar(nombre, telefono) }, shape = RoundedCornerShape(12.dp)) { Text("GUARDAR") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("CANCELAR") } }
    )
}
