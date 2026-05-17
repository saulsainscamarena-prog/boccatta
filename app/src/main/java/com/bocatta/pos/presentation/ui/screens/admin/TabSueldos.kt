package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.bocatta.pos.domain.model.ConfiguracionSalarial
import com.bocatta.pos.domain.model.FormaPago
import com.bocatta.pos.domain.model.TipoPago
import com.bocatta.pos.presentation.viewmodel.SalarioViewModel
import com.bocatta.pos.presentation.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSueldos(salarioVm: SalarioViewModel, adminVm: AdminViewModel) {
    var selectedEmpleadoId by remember { mutableStateOf("") }
    val pagos = salarioVm.pagos
    val configActual = salarioVm.configuracion

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("SUELDOS Y NÓMINA", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))

        if (adminVm.usuarios.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay empleados registrados", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(adminVm.usuarios.filter { it.uid.isNotBlank() }, key = { it.uid }) { user ->
                    val pago = pagos.find { it.empleadoId == user.uid }
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(user.nombre.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(user.rol?.name ?: "Sin rol", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                                if (pago != null) {
                                    Text("Último pago: $${"%.2f".format(pago.salarioNeto)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(if (pago.pagado) "✅ Pagado" else "⏳ Pendiente", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                } else {
                                    Text("Sin pagos registrados", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            IconButton(onClick = { salarioVm.cargarConfiguracion(user.uid); selectedEmpleadoId = user.uid }) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
    }

    if (selectedEmpleadoId.isNotBlank()) {
        val user = adminVm.usuarios.find { it.uid == selectedEmpleadoId }
        if (user != null) {
            DialogConfigSalario(
                empleadoId = selectedEmpleadoId,
                empleadoNombre = user.nombre,
                configActual = configActual,
                onSave = { salarioVm.guardarConfiguracion(it); selectedEmpleadoId = "" },
                onDismiss = { selectedEmpleadoId = "" }
            )
        }
    }
}

@Composable
private fun DialogConfigSalario(
    empleadoId: String,
    empleadoNombre: String,
    configActual: ConfiguracionSalarial?,
    onSave: (ConfiguracionSalarial) -> Unit,
    onDismiss: () -> Unit
) {
    var tipoPago by remember { mutableStateOf(configActual?.tipoPago ?: TipoPago.DIARIO) }
    var salarioBase by remember { mutableStateOf(configActual?.salarioBase?.toString() ?: "250") }
    var formaPago by remember { mutableStateOf(configActual?.formaPago ?: FormaPago.EFECTIVO) }
    var banco by remember { mutableStateOf(configActual?.banco ?: "") }
    var clabe by remember { mutableStateOf(configActual?.clabe ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(empleadoNombre.uppercase(), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tipo de pago:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TipoPago.entries.forEach { t -> FilterChip(selected = tipoPago == t, onClick = { tipoPago = t }, label = { Text(t.name.replace("_", " "), fontSize = 10.sp) }) }
                }
                OutlinedTextField(value = salarioBase, onValueChange = { salarioBase = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Salario base (\$)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                Text("Forma de pago:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FormaPago.entries.forEach { f -> FilterChip(selected = formaPago == f, onClick = { formaPago = f }, label = { Text(f.name, fontSize = 10.sp) }) }
                }
                if (formaPago == FormaPago.TRANSFERENCIA) {
                    OutlinedTextField(value = banco, onValueChange = { banco = it }, label = { Text("Banco") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = clabe, onValueChange = { clabe = it }, label = { Text("CLABE") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ConfiguracionSalarial(empleadoId = empleadoId, tipoPago = tipoPago, salarioBase = salarioBase.toDoubleOrNull() ?: 0.0, formaPago = formaPago, banco = banco, clabe = clabe)); onDismiss() },
                enabled = salarioBase.toDoubleOrNull() ?: 0.0 > 0) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}


