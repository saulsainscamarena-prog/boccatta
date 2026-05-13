package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("LOGÍSTICA GLOBAL", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("CONTROL DE STOCK Y TRANSFERENCIAS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A))))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
            ) {
                item { 
                    Text("INVENTARIO CENTRALIZADO - SELECCIONA PARA TRANSFERIR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f), letterSpacing = 1.sp) 
                }
                items(vm.insumosGlobal) { insumo ->
                    val stock = vm.stockGlobal[insumo] ?: 0.0
                    Surface(
                        onClick = { selectedInsumo = insumo; showDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Row(
                            Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(insumo.uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp, letterSpacing = 1.sp)
                            StatusBadgePremium(
                                text = "${stock.toInt()} UNIDADES",
                                color = if (stock > 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
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


