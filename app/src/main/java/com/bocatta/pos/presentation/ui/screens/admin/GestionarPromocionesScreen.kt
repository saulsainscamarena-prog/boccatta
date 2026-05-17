package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.AlcancePromo
import com.bocatta.pos.domain.model.PromocionUniversal
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.TipoDescuentoPromo
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionarPromocionesScreen(
    usuarioActual: Usuario,
    onBack: () -> Unit
) {
    // PROTECCIÓN DE ROL EXTREMA: Bloqueamos la UI desde la raíz.
    if (usuarioActual.rol == Rol.VENDEDOR) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Warning, contentDescription = "Bloqueado", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Text("ACCESO DENEGADO", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.error)
                Text("Solo Administradores y Dueños pueden gestionar promociones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Volver")
                }
            }
        }
        return
    }

    // VARIABLES DE ESTADO (Para maquetar la UI antes de conectar el backend)
    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    
    // Lista Dummy temporal para poder ver la UI
    val promocionesActivas = remember { mutableStateListOf<PromocionUniversal>() }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Promociones", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogoNueva = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, "Nueva") },
                text = { Text("NUEVA PROMO", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (promocionesActivas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CardGiftcard, "Tarjeta", Modifier.size(80.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No hay promociones activas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Toca el botón + para crear tu primera regla de descuento", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(promocionesActivas) { promo ->
                    PromocionCardUI(promocion = promo)
                }
                item { Spacer(Modifier.height(80.dp)) } // Padding para el FAB
            }
        }
    }

    // Modal para crear nueva promo
    if (mostrarDialogoNueva) {
        DialogoCrearPromocion(
            onDismiss = { mostrarDialogoNueva = false },
            onSave = { nuevaPromo -> 
                promocionesActivas.add(nuevaPromo)
                mostrarDialogoNueva = false 
            }
        )
    }
}

@Composable
fun PromocionCardUI(promocion: PromocionUniversal) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(promocion.nombre, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Switch(checked = promocion.activa, onCheckedChange = { /* TODO: Toggle Status en backend */ })
            }
            if (promocion.descripcion.isNotBlank()) {
                Text(promocion.descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            
            Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("DESCUENTO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val descStr = when(promocion.tipoDescuento) {
                        TipoDescuentoPromo.PORCENTAJE -> "${promocion.valorDescuento.toInt()}% OFF"
                        TipoDescuentoPromo.MONTO_FIJO_TICKET -> "-$${promocion.valorDescuento}"
                        TipoDescuentoPromo.MONTO_FIJO_ITEM -> "-$${promocion.valorDescuento} c/u"
                        TipoDescuentoPromo.PRECIO_FIJO_ITEM -> "A $${promocion.valorDescuento}"
                    }
                    Text(descStr, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ALCANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val alcanceStr = when(promocion.alcance) {
                        AlcancePromo.TICKET_COMPLETO -> "Todo el Ticket"
                        AlcancePromo.CATEGORIAS_ESPECIFICAS -> "Categorías"
                        AlcancePromo.PRODUCTOS_ESPECIFICOS -> "Productos Específicos"
                    }
                    Text(alcanceStr, fontWeight = FontWeight.Bold)
                }
            }
            
            if (promocion.etiquetas.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    promocion.etiquetas.forEach { tag ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                            Text(tag.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoCrearPromocion(onDismiss: () -> Unit, onSave: (PromocionUniversal) -> Unit) {
    // Variables para el formulario MVP
    var nombre by remember { mutableStateOf("") }
    var tipoDescuento by remember { mutableStateOf(TipoDescuentoPromo.PORCENTAJE) }
    var valorDescuento by remember { mutableStateOf("") }
    
    // BottomSheet o Dialog completo (usaremos un Dialog a pantalla completa para que quepan las reglas)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Promoción", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre de la Promo (Ej: Anti-Merma)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                
                Text("Tipo de Descuento", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipoDescuento == TipoDescuentoPromo.PORCENTAJE,
                        onClick = { tipoDescuento = TipoDescuentoPromo.PORCENTAJE },
                        label = { Text("%") }
                    )
                    FilterChip(
                        selected = tipoDescuento == TipoDescuentoPromo.MONTO_FIJO_TICKET,
                        onClick = { tipoDescuento = TipoDescuentoPromo.MONTO_FIJO_TICKET },
                        label = { Text("-$ Ticket") }
                    )
                }

                OutlinedTextField(
                    value = valorDescuento, onValueChange = { valorDescuento = it },
                    label = { Text(if(tipoDescuento == TipoDescuentoPromo.PORCENTAJE) "Porcentaje (Ej: 20)" else "Monto (Ej: 50.0)") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nueva = PromocionUniversal(
                        id = java.util.UUID.randomUUID().toString(),
                        nombre = nombre,
                        tipoDescuento = tipoDescuento,
                        valorDescuento = valorDescuento.toDoubleOrNull() ?: 0.0,
                        activa = true,
                        etiquetas = listOf("nueva")
                    )
                    onSave(nueva)
                },
                enabled = nombre.isNotBlank() && valorDescuento.isNotBlank()
            ) { Text("GUARDAR PROMO") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}


