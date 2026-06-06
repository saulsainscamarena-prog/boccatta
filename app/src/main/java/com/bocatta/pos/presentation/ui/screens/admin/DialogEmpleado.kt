package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.presentation.viewmodel.SalarioViewModel
import kotlinx.coroutines.tasks.await
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEmpleado(
    empleado: EmpleadoV2,
    onDismiss: () -> Unit,
    onSave: (EmpleadoV2, PermisoEmpleado, String?) -> Unit
) {
    val db = FirebaseFirestoreProvider.db
    val salarioVm: SalarioViewModel = koinViewModel()

    var nombre by remember { mutableStateOf(empleado.nombre) }
    val rolesValidos = listOf("VENDEDOR", "ADMIN", "DUEÑO")
    var rolSeleccionado by remember { mutableStateOf(empleado.rol) }
    var sucursalAsignada by remember { mutableStateOf(empleado.sucursalAsignada) }
    var authUid by remember { mutableStateOf(empleado.authUid) }

    var permisos by remember { mutableStateOf(PermisoEmpleado()) }
    var puesto by remember { mutableStateOf("Mesero") }

    // Sub-dialog states (Progressive Disclosure)
    var showPinDialog by remember { mutableStateOf(false) }
    var showSalarioDialog by remember { mutableStateOf(false) }
    var showPermisosDialog by remember { mutableStateOf(false) }

    // PIN state
    var pinNuevo by remember { mutableStateOf<String?>(null) }

    // Salario state
    var salarioBase by remember { mutableStateOf(0.0) }
    var tipoPago by remember { mutableStateOf(TipoPago.DIARIO) }
    var formaPago by remember { mutableStateOf(FormaPago.EFECTIVO) }
    var banco by remember { mutableStateOf("") }
    var clabe by remember { mutableStateOf("") }
    var cuenta by remember { mutableStateOf("") }
    var deduccionIsr by remember { mutableStateOf(0.0) }
    var deduccionImss by remember { mutableStateOf(0.0) }
    var deduccionPrestamo by remember { mutableStateOf(0.0) }

    // Cargar permisos y sueldos al iniciar
    LaunchedEffect(empleado.id) {
        if (empleado.id.isNotBlank()) {
            try {
                // Cargar permisos del mismo documento del empleado
                val doc = db.collection(FirestoreCollections.EMPLEADOS).document(empleado.id).get().await()
                if (doc.exists()) {
                    doc.toObject(PermisoEmpleado::class.java)?.let {
                        permisos = it
                        puesto = it.puesto
                    }
                }
                // Cargar salario
                salarioVm.cargarConfiguracion(empleado.id)
            } catch (e: Exception) {
                Timber.tag("EMPLOYEE_DIALOG").e(e, "Error al cargar configuración")
            }
        }
    }

    LaunchedEffect(salarioVm.configuracion) {
        salarioVm.configuracion?.let {
            salarioBase = it.salarioBase
            tipoPago = it.tipoPago
            formaPago = it.formaPago
            banco = it.banco
            clabe = it.clabe
            cuenta = it.cuenta
            deduccionIsr = it.deduccionIsr
            deduccionImss = it.deduccionImss
            deduccionPrestamo = it.deduccionPrestamo
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (empleado.id.isBlank()) "REGISTRAR EMPLEADO" else "EDITAR FICHA DE EMPLEADO", fontWeight = FontWeight.Black, fontSize = 20.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nombre completo
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Sucursal
                OutlinedTextField(
                    value = sucursalAsignada,
                    onValueChange = { sucursalAsignada = it },
                    label = { Text("Sucursal Asignada") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Auth UID
                OutlinedTextField(
                    value = authUid,
                    onValueChange = { authUid = it },
                    label = { Text("ID de Usuario Firebase (AuthUid)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Puesto administrativo
                OutlinedTextField(
                    value = puesto,
                    onValueChange = { puesto = it },
                    label = { Text("Puesto descriptivo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Selección de Rol
                Column {
                    Text("Rol Administrativo:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        rolesValidos.forEach { rol ->
                            FilterChip(
                                selected = rolSeleccionado == rol,
                                onClick = { rolSeleccionado = rol },
                                label = { Text(rol, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.08f))

                // Configuración de Módulos (Progressive Disclosure Buttons)
                Text("Configuraciones avanzadas:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                // Botón PIN
                OutlinedButton(
                    onClick = { showPinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Default.Pin, null)
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("Configurar PIN de Acceso", fontWeight = FontWeight.Bold)
                        Text(
                            if (pinNuevo != null) "PIN nuevo ingresado" else "Haz clic para cambiar el PIN",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }

                // Botón Salario y Nómina
                OutlinedButton(
                    onClick = { showSalarioDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Default.Payments, null)
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("Configuración Salarial", fontWeight = FontWeight.Bold)
                        Text(
                            "Salario: $${"%.2f".format(salarioBase)} (${tipoPago.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }

                // Botón Permisos Operativos
                OutlinedButton(
                    onClick = { showPermisosDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Default.Security, null)
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("Permisos Operativos", fontWeight = FontWeight.Bold)
                        Text(
                            "Editar accesos táctiles del mostrador",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEmp = empleado.copy(
                        nombre = nombre.trim(),
                        rol = rolSeleccionado,
                        sucursalAsignada = sucursalAsignada.trim(),
                        authUid = authUid.trim()
                    )
                    val finalPerms = permisos.copy(
                        puesto = puesto.trim()
                    )

                    // Guardar salario en background si se modificó
                    salarioVm.guardarConfiguracion(
                        ConfiguracionSalarial(
                            empleadoId = empleado.id.ifBlank { "NUEVO" },
                            tipoPago = tipoPago,
                            salarioBase = salarioBase,
                            formaPago = formaPago,
                            banco = banco.trim(),
                            clabe = clabe.trim(),
                            cuenta = cuenta.trim(),
                            deduccionIsr = deduccionIsr,
                            deduccionImss = deduccionImss,
                            deduccionPrestamo = deduccionPrestamo,
                            activo = true
                        )
                    )

                    onSave(finalEmp, finalPerms, pinNuevo)
                },
                enabled = nombre.isNotBlank()
            ) { Text("GUARDAR FICHA", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )

    // 1. DIÁLOGO PIN
    if (showPinDialog) {
        var pinTemp by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configurar PIN de Acceso", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa un nuevo PIN numérico de 4 dígitos:")
                    OutlinedTextField(
                        value = pinTemp,
                        onValueChange = { if (it.length <= 4) pinTemp = it.filter { c -> c.isDigit() } },
                        label = { Text("Nuevo PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pinNuevo = pinTemp
                        showPinDialog = false
                    },
                    enabled = pinTemp.length == 4
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancelar") } }
        )
    }

    // 2. DIÁLOGO SALARIO
    if (showSalarioDialog) {
        var localSalario by remember { mutableStateOf(salarioBase.toString()) }
        var localBanco by remember { mutableStateOf(banco) }
        var localClabe by remember { mutableStateOf(clabe) }
        var localCuenta by remember { mutableStateOf(cuenta) }
        var localIsr by remember { mutableStateOf(deduccionIsr.toString()) }
        var localImss by remember { mutableStateOf(deduccionImss.toString()) }
        var localPrestamo by remember { mutableStateOf(deduccionPrestamo.toString()) }
        var localTipoPago by remember { mutableStateOf(tipoPago) }
        var localFormaPago by remember { mutableStateOf(formaPago) }

        AlertDialog(
            onDismissRequest = { showSalarioDialog = false },
            title = { Text("Configuración de Salario", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Detalles Salariales Administrativos:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tipo de Pago", style = MaterialTheme.typography.labelSmall)
                            TipoPago.entries.forEach { t ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = localTipoPago == t, onClick = { localTipoPago = t })
                                    Text(t.name, fontSize = 11.sp)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Forma de Pago", style = MaterialTheme.typography.labelSmall)
                            FormaPago.entries.forEach { f ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = localFormaPago == f, onClick = { localFormaPago = f })
                                    Text(f.name, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = localSalario,
                        onValueChange = { localSalario = it },
                        label = { Text("Salario Base ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider(color = Color.White.copy(0.08f))
                    Text("Información Bancaria:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = localBanco,
                        onValueChange = { localBanco = it },
                        label = { Text("Banco") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = localClabe,
                        onValueChange = { localClabe = it },
                        label = { Text("CLABE (18 dígitos)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = localCuenta,
                        onValueChange = { localCuenta = it },
                        label = { Text("Número de Cuenta") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider(color = Color.White.copy(0.08f))
                    Text("Deducciones Fijas:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = localIsr,
                        onValueChange = { localIsr = it },
                        label = { Text("Deducción ISR ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = localImss,
                        onValueChange = { localImss = it },
                        label = { Text("Deducción IMSS ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = localPrestamo,
                        onValueChange = { localPrestamo = it },
                        label = { Text("Deducción Préstamo ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        salarioBase = localSalario.toDoubleOrNull() ?: 0.0
                        banco = localBanco
                        clabe = localClabe
                        cuenta = localCuenta
                        deduccionIsr = localIsr.toDoubleOrNull() ?: 0.0
                        deduccionImss = localImss.toDoubleOrNull() ?: 0.0
                        deduccionPrestamo = localPrestamo.toDoubleOrNull() ?: 0.0
                        tipoPago = localTipoPago
                        formaPago = localFormaPago
                        showSalarioDialog = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showSalarioDialog = false }) { Text("Cancelar") } }
        )
    }

    // 3. DIÁLOGO PERMISOS
    if (showPermisosDialog) {
        var localTomar by remember { mutableStateOf(permisos.puedeTomarOrden) }
        var localEfectivo by remember { mutableStateOf(permisos.puedeCobrarEfectivo) }
        var localTarjeta by remember { mutableStateOf(permisos.puedeCobrarTarjeta) }
        var localPreparar by remember { mutableStateOf(permisos.puedePreparar) }
        var localMesas by remember { mutableStateOf(permisos.puedeGestionarMesas) }
        var localTransferir by remember { mutableStateOf(permisos.puedeTransferirMesas) }
        var localZonas by remember { mutableStateOf(permisos.puedeAprobarCambiosZona) }
        var localTurnos by remember { mutableStateOf(permisos.puedeCerrarTurnoAjeno) }
        var localVerSueldos by remember { mutableStateOf(permisos.puedeVerSueldos) }
        var localGestionarEmp by remember { mutableStateOf(permisos.puedeGestionarEmpleados) }

        AlertDialog(
            onDismissRequest = { showPermisosDialog = false },
            title = { Text("Permisos de Mostrador", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PermisoSwitch("Tomar Órdenes", localTomar) { localTomar = it }
                    PermisoSwitch("Cobrar Efectivo", localEfectivo) { localEfectivo = it }
                    PermisoSwitch("Cobrar Tarjeta", localTarjeta) { localTarjeta = it }
                    PermisoSwitch("Preparar en Cocina", localPreparar) { localPreparar = it }
                    PermisoSwitch("Gestionar Mesas/Cuentas", localMesas) { localMesas = it }
                    PermisoSwitch("Transferir Cuentas", localTransferir) { localTransferir = it }
                    PermisoSwitch("Aprobar Cambios de Zona", localZonas) { localZonas = it }
                    PermisoSwitch("Cerrar Turnos Ajenos", localTurnos) { localTurnos = it }
                    PermisoSwitch("Ver Nómina y Sueldos", localVerSueldos) { localVerSueldos = it }
                    PermisoSwitch("Administrar Empleados", localGestionarEmp) { localGestionarEmp = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        permisos = permisos.copy(
                            puedeTomarOrden = localTomar,
                            puedeCobrarEfectivo = localEfectivo,
                            puedeCobrarTarjeta = localTarjeta,
                            puedePreparar = localPreparar,
                            puedeGestionarMesas = localMesas,
                            puedeTransferirMesas = localTransferir,
                            puedeAprobarCambiosZona = localZonas,
                            puedeCerrarTurnoAjeno = localTurnos,
                            puedeVerSueldos = localVerSueldos,
                            puedeGestionarEmpleados = localGestionarEmp
                        )
                        showPermisosDialog = false
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showPermisosDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PermisoSwitch(titulo: String, valor: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(titulo, fontSize = 14.sp)
        Switch(checked = valor, onCheckedChange = onToggle)
    }
}
