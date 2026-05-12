package com.bocatta.pos.presentation.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.MetodoPago
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2

@Composable
fun ConfirmacionVentaDialog(
    vmV2: SalesViewModelV2,
    sucursalActual: String,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        icon = { Icon(Icons.Default.CheckCircle, null, tint = BocattaSuccess, modifier = Modifier.size(48.dp)) },
        title = { Text("¡VENTA EXITOSA!", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Código de ticket:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("🎫 ${vmV2.ultimoCodigoTicket}", fontWeight = FontWeight.Black, fontSize = 28.sp, color = BocattaPrimary)
                Spacer(Modifier.height(8.dp))
                Text("en ${sucursalActual.uppercase()}", textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ticket = vmV2.ultimoTicketTexto ?: ""
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, ticket)
                        type = "text/plain"
                        setPackage("com.whatsapp")
                    }
                    try { context.startActivity(intent) } catch (e: Exception) {
                        context.startActivity(Intent.createChooser(intent, "Enviar ticket"))
                    }
                    onDismiss()
                }, 
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BocattaSuccess)
            ) { 
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("WhatsApp") 
            }
        },
        dismissButton = { 
            TextButton(onClick = { onDismiss() }) { 
                Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            } 
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun BuscarClienteDialog(
    vmV2: SalesViewModelV2,
    onDismiss: () -> Unit,
    onNavigateToRegistrar: () -> Unit
) {
    var filtroInput by remember { mutableStateOf("") }
    LaunchedEffect(filtroInput) { if (filtroInput.length >= 3) vmV2.buscarCliente(filtroInput) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = "Cliente frecuente", tint = BocattaPrimary)
                Spacer(Modifier.width(12.dp))
                Text("CLIENTE FRECUENTE", fontWeight = FontWeight.Black) 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = filtroInput, 
                    onValueChange = { filtroInput = it }, 
                    label = { Text("Nombre o Teléfono") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    leadingIcon = { Icon(Icons.Default.Search, null) }, 
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vmV2.clientesSugeridos) { c ->
                        ElevatedCard(
                            onClick = { vmV2.seleccionarCliente(c); onDismiss() }, 
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = BocattaPrimary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(c.nombre.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Black, color = BocattaPrimary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column { 
                                    Text(c.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(c.telefono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                                }
                                Spacer(Modifier.height(12.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(onClick = { onDismiss(); onNavigateToRegistrar() }, shape = RoundedCornerShape(12.dp)) { 
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Registro") 
            } 
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RegistrarClienteDialog(
    vmV2: SalesViewModelV2,
    onDismiss: () -> Unit
) {
    var n by remember { mutableStateOf("") }
    var t by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NUEVO CLIENTE", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("WhatsApp (10 dígitos)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            }
        },
        confirmButton = { 
            Button(onClick = { vmV2.registrarClienteNuevo(n, t); onDismiss() }, enabled = n.length > 3 && t.length == 10, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { 
                Text("Registrar Cliente") 
            } 
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RetiroAlimentoDialog(
    vmV2: SalesViewModelV2,
    sucursalActual: String,
    nombreUsuario: String,
    onDismiss: () -> Unit
) {
    var montoRetiro by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RETIRO ALIMENTO ($)", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Máximo permitido: $150 pesos por turno.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = montoRetiro, 
                    onValueChange = { montoRetiro = it }, 
                    label = { Text("Monto a retirar") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$ ") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    vmV2.registrarRetiroAlimento(sucursalActual, nombreUsuario, montoRetiro.toDoubleOrNull() ?: 0.0)
                    onDismiss() 
                },
                enabled = (montoRetiro.toDoubleOrNull() ?: 0.0) in 1.0..150.0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Registrar Retiro") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PagoDialog(
    vmV2: SalesViewModelV2,
    sucursalActual: String,
    nombreUsuario: String,
    onDismiss: () -> Unit
) {
    var pagaCon by remember { mutableStateOf("") }
    val total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones).coerceAtLeast(0.0)
    val cambio = (pagaCon.toDoubleOrNull() ?: 0.0) - total
    val esAdmin = vmV2.rolUsuario == com.bocatta.pos.domain.model.Rol.ADMIN
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = "Pago", tint = BocattaPrimary)
                Spacer(Modifier.width(12.dp))
                Text("PAGO FINAL", fontWeight = FontWeight.Black) 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), 
                    shape = RoundedCornerShape(16.dp), 
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL A PAGAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${"%.2f".format(total)}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = BocattaPrimary)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vmV2.metodoPagoSeleccionado = MetodoPago.EFECTIVO }, 
                        modifier = Modifier.weight(1f).height(48.dp), 
                        shape = RoundedCornerShape(14.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (vmV2.metodoPagoSeleccionado == MetodoPago.EFECTIVO) BocattaPrimary else MaterialTheme.colorScheme.surfaceVariant, 
                            contentColor = if (vmV2.metodoPagoSeleccionado == MetodoPago.EFECTIVO) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) { 
                        Text("Efectivo", fontSize = 12.sp) 
                    }
                    Button(
                        onClick = { vmV2.metodoPagoSeleccionado = MetodoPago.TARJETA }, 
                        modifier = Modifier.weight(1f).height(48.dp), 
                        shape = RoundedCornerShape(14.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (vmV2.metodoPagoSeleccionado == MetodoPago.TARJETA) BocattaPrimary else MaterialTheme.colorScheme.surfaceVariant, 
                            contentColor = if (vmV2.metodoPagoSeleccionado == MetodoPago.TARJETA) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) { 
                        Text("Tarjeta", fontSize = 12.sp) 
                    }
                    Button(
                        onClick = { vmV2.metodoPagoSeleccionado = MetodoPago.TRANSFERENCIA }, 
                        modifier = Modifier.weight(1f).height(48.dp), 
                        shape = RoundedCornerShape(14.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (vmV2.metodoPagoSeleccionado == MetodoPago.TRANSFERENCIA) BocattaPrimary else MaterialTheme.colorScheme.surfaceVariant, 
                            contentColor = if (vmV2.metodoPagoSeleccionado == MetodoPago.TRANSFERENCIA) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) { 
                        Text("Transf.", fontSize = 12.sp) 
                    }
                }

                if (esAdmin) {
                    Surface(
                        onClick = { vmV2.esConsumoEmpleado = !vmV2.esConsumoEmpleado },
                        color = if (vmV2.esConsumoEmpleado) BocattaNeonGreen.copy(0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, if (vmV2.esConsumoEmpleado) BocattaNeonGreen else Color.Transparent)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = vmV2.esConsumoEmpleado, onCheckedChange = { vmV2.esConsumoEmpleado = it })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Consumo Empleado (Cortesía)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Aplica para 1 pieza de menú.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                if (vmV2.metodoPagoSeleccionado == MetodoPago.EFECTIVO) {
                    OutlinedTextField(
                        value = pagaCon, 
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) pagaCon = it }, 
                        label = { Text("Recibido con:") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$ ") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (pagaCon.isNotEmpty() && cambio >= 0) {
                        Surface(
                            color = BocattaPrimary.copy(0.1f), 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("CAMBIO:", fontWeight = FontWeight.Black, color = BocattaPrimary)
                                Text("$${"%.2f".format(cambio)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BocattaPrimary)
                            }
                        }
                    }
                }

                val itemsSinStock = vmV2.carrito.filter { item -> (vmV2.alertasStock[item.producto.id] ?: 99.0) <= 0 }
                if (itemsSinStock.isNotEmpty()) {
                    Surface(
                        color = BocattaDanger.copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BocattaDanger)
                    ) {
                        Text(
                            "STOCK INSUFICIENTE: ${itemsSinStock.joinToString { it.nombre }}",
                            modifier = Modifier.padding(12.dp),
                            color = BocattaDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            val sinStock = vmV2.carrito.any { item -> (vmV2.alertasStock[item.producto.id] ?: 99.0) <= 0 }
            Button(
                enabled = (vmV2.metodoPagoSeleccionado != MetodoPago.EFECTIVO || cambio >= 0) && !vmV2.cargando && total > 0 && !sinStock,
                onClick = { vmV2.finalizarVenta(sucursalActual, nombreUsuario) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BocattaPrimary)
            ) { 
                if (vmV2.cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirmar Pago", fontWeight = FontWeight.Black) 
                }
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { 
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant) 
            } 
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EliminarItemDialog(
    item: ItemCarritoV2,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ELIMINAR PRODUCTO", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("¿Estás seguro de eliminar ${item.nombre}?")
                OutlinedTextField(
                    value = motivo, 
                    onValueChange = { motivo = it }, 
                    label = { Text("Motivo de cancelación") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(motivo) }, enabled = motivo.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = BocattaDanger)) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
