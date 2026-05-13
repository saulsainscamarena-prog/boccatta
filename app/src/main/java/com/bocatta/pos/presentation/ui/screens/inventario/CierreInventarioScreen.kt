package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.domain.model.ItemConteo
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreInventarioScreen(
    vm: InventoryViewModel,
    session: SessionViewModel,
    onBack: () -> Unit
) {
    val db = FirebaseFirestoreProvider.db
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    val itemsConteo = remember { mutableStateListOf<ItemConteo>() }
    var cargando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }

    val insumosFiltros = listOf(
        "masa_crepa" to "Masa Crepa (unidades)",
        "charola" to "Charolas (piezas)",
        "servilletas" to "Servilletas (paq/pzs)"
    )

    LaunchedEffect(Unit) {
        cargando = true
        try {
            val sucursalId = session.sucursalActual.lowercase()
            itemsConteo.clear()
            insumosFiltros.forEach { (id, nombre) ->
                val stockRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$id").get().await()
                val stock = stockRef.getDouble("cantidadEnBase") ?: stockRef.getDouble("cantidadDisponible") ?: 0.0
                val docMaestro = db.collection(FirestoreCollections.INSUMOS).document(id).get().await()
                val costoU = docMaestro.getDouble("costoUnitarioBase") ?: 0.0
                itemsConteo.add(ItemConteo(id = id, nombre = nombre, stockSistema = stock, costoUnitario = costoU))
            }
        } catch (e: Exception) {
            snackbarHost.showSnackbar("Error: ${e.message}")
        }
        cargando = false
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("CIERRE DE INVENTARIO", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("RECUENTO FÍSICO FINAL · ${session.sucursalActual.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Surface(color = Color.White.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.padding(10.dp)) 
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFF0F111A).copy(0.95f),
                border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                cargando = true
                                try {
                                    val batch = db.batch()
                                    val sucursalId = session.sucursalActual.lowercase()
                                    itemsConteo.forEach { item ->
                                        val fisico = item.conteoFisico.toDoubleOrNull() ?: return@forEach
                                        val dif = item.stockSistema - fisico
                                        batch.set(
                                            db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_${item.id}"),
                                            mapOf("cantidadEnBase" to fisico, "ultimaActualizacion" to System.currentTimeMillis()),
                                            com.google.firebase.firestore.SetOptions.merge()
                                        )
                                        if (dif > 0.01) {
                                            batch.set(db.collection(FirestoreCollections.MERMA_LOGS).document(), mapOf(
                                                "insumoId" to item.id, "sucursal" to session.sucursalActual, "nombre" to item.nombre,
                                                "cantidad" to dif, "costoEstimado" to (dif * item.costoUnitario),
                                                "motivo" to "Cierre Industrial V2", "fecha" to System.currentTimeMillis(), "usuarioId" to session.uid
                                            ))
                                        }
                                    }
                                    batch.commit().await()
                                    guardado = true
                                    snackbarHost.showSnackbar("Cierre industrializado con éxito")
                                } catch (e: Exception) { snackbarHost.showSnackbar("Error: ${e.message}") }
                                cargando = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp).shadow(16.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !cargando && !guardado && itemsConteo.any { it.conteoFisico.isNotBlank() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.White, disabledContainerColor = Color.White.copy(0.1f)),
                    ) {
                        if (cargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else {
                            Icon(if (guardado) Icons.Default.CheckCircle else Icons.Default.Inventory2, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (guardado) "CIERRE REGISTRADO" else "CONSOLIDAR CIERRE INDUSTRIAL", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))))) {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
            ) {
                item {
                    Text("Recuento Físico Final", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                items(itemsConteo) { item ->
                    val fisico = item.conteoFisico.toDoubleOrNull()
                    val dif = if (fisico != null) item.stockSistema - fisico else null
                    val statusColor = when {
                        dif == null -> Color.White.copy(0.1f)
                        dif > 0.01 -> MaterialTheme.colorScheme.tertiary
                        dif < -0.01 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    Surface(
                        shape = RoundedCornerShape(28.dp), 
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        border = BorderStroke(1.dp, statusColor.copy(0.4f))
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(item.nombre.uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp, letterSpacing = 1.sp)
                                    Text("STOCK EN SISTEMA: ${item.stockSistema}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
                                }
                                
                                if (dif != null && Math.abs(dif) > 0.01) {
                                    StatusBadgePremium(
                                        text = if(dif > 0) "-${"%.1f".format(dif)} MERMA" else "+${"%.1f".format(dif * -1)} SOBRANTE",
                                        color = statusColor
                                    )
                                }
                            }
                            
                            OutlinedTextField(
                                value = item.conteoFisico,
                                onValueChange = { nuevo ->
                                    val idx = itemsConteo.indexOf(item)
                                    if (idx >= 0) itemsConteo[idx] = item.copy(conteoFisico = nuevo)
                                },
                                label = { Text("CANTIDAD REAL DETERMINADA", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = statusColor,
                                    unfocusedBorderColor = Color.White.copy(0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = statusColor,
                                    unfocusedLabelColor = Color.White.copy(0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
