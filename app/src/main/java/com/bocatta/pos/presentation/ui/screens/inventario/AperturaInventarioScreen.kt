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
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturaInventarioScreen(
    vm: InventoryViewModel,
    session: SessionViewModel,
    onBack: () -> Unit
) {
    val db = FirebaseFirestoreProvider.db
    val snackbarHost = remember { SnackbarHostState() }
    val itemsConteo = remember { mutableStateListOf<ItemConteo>() }
    var cargando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }

    val insumosFallback = listOf(
        "masa_crepa" to "Masa Crepa (unidades)",
        "charola" to "Charolas (piezas)",
        "servilletas" to "Servilletas (paq/pzs)"
    )

    LaunchedEffect(Unit) {
        cargando = true
        try {
            // Intentar cargar con un limite de tiempo logico (simulado con await)
            val configSnap = db.collection(FirestoreCollections.CONFIGURACION).document("apertura_insumos")
                .collection("items").orderBy("orden").get().await()

            val finalConfig = if (!configSnap.isEmpty) {
                configSnap.documents.map { d ->
                    val id     = d.getString("id") ?: d.id
                    val nombre = d.getString("nombre") ?: ""
                    val unidad = d.getString("unidad") ?: ""
                    id to "$nombre ($unidad)"
                }
            } else {
                insumosFallback
            }

            val sucursalId = session.sucursalActual.lowercase(java.util.Locale.getDefault())
            itemsConteo.clear()
            finalConfig.forEach { (id, nombre) ->
                try {
                    val stockRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId}_$id").get().await()
                    val stock = stockRef.getDouble("cantidadEnBase") ?: stockRef.getDouble("cantidadDisponible") ?: 0.0
                    itemsConteo.add(ItemConteo(id = id, nombre = nombre, stockSistema = stock))
                } catch (e: Exception) {
                    itemsConteo.add(ItemConteo(id = id, nombre = nombre, stockSistema = 0.0))
                }
            }
        } catch (e: Exception) {
            snackbarHost.showSnackbar("Usando configuracion de respaldo...")
            itemsConteo.clear()
            insumosFallback.forEach { (id, nombre) ->
                itemsConteo.add(ItemConteo(id = id, nombre = nombre, stockSistema = 0.0))
            }
        } finally {
            cargando = false
        }
    }

    val contados = itemsConteo.count { it.conteoFisico.isNotBlank() }
    val total = itemsConteo.size

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("CARGA DE INVENTARIO", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("AUDITORIA DE STOCK INICIAL - ${session.sucursalActual.uppercase(java.util.Locale.getDefault())}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Surface(color = Color.White.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.padding(10.dp)) 
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = {
                            cargando = true
                            val conteos = itemsConteo.associate { it.id to (it.conteoFisico.toDoubleOrNull() ?: it.stockSistema) }
                            vm.registrarAperturaDiaria(conteos, session.uid) { exito ->
                                cargando = false
                                if (exito) guardado = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp).shadow(16.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !cargando && !guardado && contados == total && total > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background, disabledContainerColor = Color.White.copy(0.1f)),
                    ) {
                        Icon(if (guardado) Icons.Default.CheckCircle else Icons.Default.Inventory, "Inventario")
                        Spacer(Modifier.width(12.dp))
                        Text(if (guardado) "CARGA FINALIZADA" else "CONFIRMAR CARGA ($contados/$total)", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainer)))) {
            if (cargando && itemsConteo.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
                ) {
                    item {
                        Text(
                            "Valida el stock f\u00EDsico inicial. El sistema compara contra el cierre de ayer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.5f)
                        )
                    }

                    items(itemsConteo, key = { it.id }) { item ->
                        val fisico = item.conteoFisico.toDoubleOrNull()
                        val diferencia = if (fisico != null) fisico - item.stockSistema else null
                        val statusColor = when {
                            diferencia == null -> Color.White.copy(0.1f)
                            Math.abs(diferencia) < 0.01 -> MaterialTheme.colorScheme.tertiary
                            diferencia < 0 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(28.dp), // Meridian Spec: 28dp
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, statusColor.copy(0.4f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(item.nombre.uppercase(java.util.Locale.getDefault()), fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp, letterSpacing = 1.sp)
                                        Text("STOCK EN SISTEMA: ${item.stockSistema}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
                                    }
                                    
                                    if (diferencia != null && Math.abs(diferencia) > 0.01) {
                                        StatusBadgePremium(
                                            text = if(diferencia > 0) "+${"%.1f".format(diferencia)}" else "${"%.1f".format(diferencia)}",
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
                                    label = { Text("CONTEO FISICO ACTUAL", fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
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
}






