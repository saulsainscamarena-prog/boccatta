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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.ConfigNegocioViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * TabConfigNegocio — antes era un composable vacío que mostraba pantalla en blanco.
 * Ahora implementado completamente usando ConfigNegocioViewModel que ya existía en Koin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabConfigNegocio() {
    val vm: ConfigNegocioViewModel = koinViewModel()

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── NEGOCIO ────────────────────────────────────────────────────
            BocattaSectionTitle("Identidad del negocio")
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = vm.nombreNegocio,
                        onValueChange = { vm.nombreNegocio = it },
                        label = { Text("Nombre del negocio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Store, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    )
                    OutlinedTextField(
                        value = vm.giroNegocio,
                        onValueChange = { vm.giroNegocio = it },
                        label = { Text("Giro / Tipo de negocio") },
                        placeholder = { Text("Ej: Crepería, Restaurante, Ferretería...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Category, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    )
                }
            }

            // ── CAJA ──────────────────────────────────────────────────────
            BocattaSectionTitle("Configuración de caja")
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = vm.toleranciaEfectivo,
                        onValueChange = { vm.toleranciaEfectivo = it },
                        label = { Text("Tolerancia efectivo ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Diferencia máxima aceptable al cuadrar efectivo") }
                    )
                    OutlinedTextField(
                        value = vm.toleranciaTarjeta,
                        onValueChange = { vm.toleranciaTarjeta = it },
                        label = { Text("Tolerancia tarjeta ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Diferencia máxima aceptable al cuadrar tarjeta") }
                    )
                    OutlinedTextField(
                        value = vm.fondoMinimo,
                        onValueChange = { vm.fondoMinimo = it },
                        label = { Text("Fondo mínimo de apertura ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            // ── MEMBRESÍA ─────────────────────────────────────────────────
            BocattaSectionTitle("Programa de membresía")
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = vm.cicloMembresia,
                        onValueChange = { vm.cicloMembresia = it },
                        label = { Text("Visitas para descuento") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Cada cuántas visitas se otorga el descuento (actual: ${FirestoreCollections.MEMBRESIA_CICLO_VISITAS})") },
                        leadingIcon = {
                            Icon(Icons.Default.Loyalty, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    )
                    OutlinedTextField(
                        value = vm.porcentajeDescuento,
                        onValueChange = { vm.porcentajeDescuento = it },
                        label = { Text("% de descuento") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Porcentaje que se aplica al alcanzar el ciclo") }
                    )
                }
            }

            // ── MÉTODOS DE PAGO ───────────────────────────────────────────
            BocattaSectionTitle("Métodos de pago habilitados")
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MetodoPagoToggle("Efectivo", vm.pagoEfectivo, Icons.Default.Payments) { vm.pagoEfectivo = it }
                    MetodoPagoToggle("Tarjeta", vm.pagoTarjeta, Icons.Default.CreditCard) { vm.pagoTarjeta = it }
                    MetodoPagoToggle("Transferencia", vm.pagoTransferencia, Icons.Default.AccountBalance) { vm.pagoTransferencia = it }
                    MetodoPagoToggle("Rappi", vm.pagoRappi, Icons.Default.DeliveryDining) { vm.pagoRappi = it }
                    MetodoPagoToggle("Uber Eats", vm.pagoUber, Icons.Default.TwoWheeler) { vm.pagoUber = it }
                    MetodoPagoToggle("DiDi Food", vm.pagoDidi, Icons.Default.DirectionsBike) { vm.pagoDidi = it }
                }
            }

            // ── BOTÓN GUARDAR ─────────────────────────────────────────────
            BocattaButton(
                texto = "Guardar configuración",
                onClick = { vm.guardarConfiguracion() },
                modifier = Modifier.fillMaxWidth(),
                cargando = vm.cargando,
                icono = Icons.Default.Save,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MetodoPagoToggle(
    nombre: String,
    habilitado: Boolean,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null,
                tint = if (habilitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.5f),
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                nombre,
                fontSize = 14.sp,
                fontWeight = if (habilitado) FontWeight.SemiBold else FontWeight.Normal,
                color = if (habilitado) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(0.5f)
            )
        }
        Switch(
            checked = habilitado,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

