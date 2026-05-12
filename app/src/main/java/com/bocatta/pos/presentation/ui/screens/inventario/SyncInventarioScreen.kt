package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.bocatta.pos.presentation.ui.theme.BocattaSuccess
import com.bocatta.pos.presentation.ui.theme.BocattaWarning
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncInventarioScreen(onBack: () -> Unit) {
    val vm = remember { SyncInventarioViewModel() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedInsumo by remember { mutableStateOf<String?>(null) }
    var selectedSucursal by remember { mutableStateOf<String?>(null) }
    var cantidad by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(showSnackbar) { showSnackbar?.let { snackbar.showSnackbar(it); showSnackbar = null } }

    if (showDialog && selectedInsumo != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Sync a Sucursal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Insumo: $selectedInsumo")
                    Text("Stock Global: ${vm.stockGlobal[selectedInsumo] ?: 0.0}")
                    
                    OutlinedTextField(
                        value = selectedSucursal ?: "",
                        onValueChange = { selectedSucursal = it },
                        label = { Text("Sucursal (ej: Atlixco)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Cantidad a transferir") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val cantValida = cantidad.toDoubleOrNull() ?: 0.0
                Button(
                    enabled = cantValida > 0,
                    onClick = {
                        val insumo = selectedInsumo ?: return@Button
                        vm.syncToBranch(insumo, selectedSucursal ?: "", cantValida)
                        showDialog = false
                        cantidad = ""
                        selectedSucursal = null
                        showSnackbar = "Transferencia completada"
                    }
                ) {
                    Text("Sincronizar")
                }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sync Inventario Global", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Volver"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Stock Global - Toca para transferir", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(vm.insumosGlobal) { insumo ->
                val stock = vm.stockGlobal[insumo] ?: 0.0
                ElevatedCard(
                    onClick = { selectedInsumo = insumo; showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(insumo, fontWeight = FontWeight.Medium)
                        Text(
                            "${stock.toInt()} u.",
                            fontWeight = FontWeight.Bold,
                            color = if (stock > 10) BocattaSuccess else BocattaWarning
                        )
                    }
                }
            }
        }
    }
}

class SyncInventarioViewModel : ViewModel() {
    private val db = FirebaseFirestoreProvider.db
    var insumosGlobal = mutableStateListOf<String>()
    var sucursales = mutableStateListOf<String>()
    var stockGlobal = mutableStateMapOf<String, Double>()
    var cargando by mutableStateOf(true)

    init { cargarGlobal() }

    private fun cargarGlobal() {
        viewModelScope.launch {
            try {
                cargando = true
                val snap = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).get().await()
                val branchSnap = db.collection(FirestoreCollections.SUCURSALES).get().await()
                
                insumosGlobal.clear()
                stockGlobal.clear()
                sucursales.clear()
                
                snap.documents.forEach { doc ->
                    insumosGlobal.add(doc.id)
                    stockGlobal[doc.id] = doc.getDouble("cantidadEnBase") ?: doc.getDouble("cantidadDisponible") ?: 0.0
                }
                
                branchSnap.documents.forEach { doc ->
                    sucursales.add(doc.getString("nombre") ?: doc.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncInvScreen", "Error cargando inventario global", e)
            }
            finally { cargando = false }
        }
    }

    fun syncToBranch(insumoId: String, sucursalId: String, cantidad: Double) {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val branchRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL).document("${sucursalId.lowercase()}_$insumoId")
                batch.set(branchRef, mapOf("cantidadEnBase" to FieldValue.increment(cantidad)), com.google.firebase.firestore.SetOptions.merge())
                
                // Tambi�n descontar de global
                val globalRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId)
                batch.set(globalRef, mapOf("cantidadEnBase" to FieldValue.increment(-cantidad)), com.google.firebase.firestore.SetOptions.merge())
                
                batch.commit().await()
            } catch (e: Exception) {
                android.util.Log.e("SyncInvScreen", "Error sincronizando a sucursal", e)
            }
        }
    }
}


