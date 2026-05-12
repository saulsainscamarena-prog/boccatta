package com.bocatta.pos.presentation.ui.screens.inventario

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import com.bocatta.pos.presentation.ui.theme.*
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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CIERRE DE INVENTARIO", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp)
                        Text(session.sucursalActual.uppercase(), color = BocattaNeonMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                color = BocattaSurfaceDark.copy(0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)),
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
                                    snackbarHost.showSnackbar("Cierre completado con �xito")
                                } catch (e: Exception) { snackbarHost.showSnackbar("Error: ${e.message}") }
                                cargando = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !cargando && !guardado && itemsConteo.any { it.conteoFisico.isNotBlank() },
                        colors = ButtonDefaults.buttonColors(containerColor = BocattaNeonMagenta),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        if (cargando) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        else {
                            Icon(if (guardado) Icons.Default.CheckCircle else Icons.Default.Save, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (guardado) "CIERRE REGISTRADO" else "REGISTRAR CIERRE FINAL", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BocattaBgDark, BocattaSurfaceDark)))) {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
            ) {
                item {
                    Text("Conteo F�sico Final", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                }
                items(itemsConteo) { item ->
                    val fisico = item.conteoFisico.toDoubleOrNull()
                    val dif = if (fisico != null) item.stockSistema - fisico else null
                    val statusColor = when {
                        dif == null -> Color.White.copy(0.1f)
                        dif > 0.01 -> BocattaNeonMagenta
                        dif < -0.01 -> BocattaNeonCyan
                        else -> BocattaNeonGreen
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(24.dp), 
                        color = Color.White.copy(0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(0.3f))
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.nombre, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.weight(1f))
                                Text("Sistema: ${item.stockSistema}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
                            }
                            OutlinedTextField(
                                value = item.conteoFisico,
                                onValueChange = { nuevo ->
                                    val idx = itemsConteo.indexOf(item)
                                    if (idx >= 0) itemsConteo[idx] = item.copy(conteoFisico = nuevo)
                                },
                                label = { Text("Cantidad Real") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = statusColor,
                                    unfocusedBorderColor = Color.White.copy(0.1f),
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            if (dif != null) {
                                Text(
                                    text = if (dif > 0.01) "?� Merma detectada: $dif" else if (dif < -0.01) "?? Sobrante: ${dif * -1}" else "? Cuadra perfectamente",
                                    color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


