package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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
            // Intentar cargar con un l�mite de tiempo l�gico (simulado con await)
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

            val sucursalId = session.sucursalActual.lowercase()
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
            snackbarHost.showSnackbar("Usando configuraci�n de respaldo...")
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
            CenterAlignedTopAppBar(
                title = { Text("CARGA DE INVENTARIO", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                color = BocattaSurfaceDark.copy(0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)),
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
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !cargando && !guardado && contados == total && total > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = BocattaNeonCyan),
                        elevation = ButtonDefaults.buttonElevation(8.dp)
                    ) {
                        Icon(if (guardado) Icons.Default.CheckCircle else Icons.Default.Inventory, null, tint = BocattaBgDark)
                        Spacer(Modifier.width(12.dp))
                        Text(if (guardado) "APERTURA REGISTRADA" else "CONFIRMAR ($contados/$total)", fontWeight = FontWeight.ExtraBold, color = BocattaBgDark)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BocattaBgDark, BocattaSurfaceDark)))) {
            if (cargando && itemsConteo.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BocattaNeonCyan)
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
                            Math.abs(diferencia) < 0.01 -> BocattaNeonGreen
                            diferencia < 0 -> BocattaNeonMagenta
                            else -> BocattaNeonCyan
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.nombre, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    Text("Sistema: ${item.stockSistema}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
                                }

                                OutlinedTextField(
                                    value = item.conteoFisico,
                                    onValueChange = { nuevo ->
                                        val idx = itemsConteo.indexOf(item)
                                        if (idx >= 0) itemsConteo[idx] = item.copy(conteoFisico = nuevo)
                                    },
                                    label = { Text("Conteo F\u00EDsico") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = statusColor,
                                        unfocusedBorderColor = Color.White.copy(0.1f),
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
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



