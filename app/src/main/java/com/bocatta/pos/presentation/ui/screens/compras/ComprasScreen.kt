package com.bocatta.pos.presentation.ui.screens.compras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(
    onBack: () -> Unit
) {
    val vm = remember { ComprasViewModel() }
    
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historial de Compras", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (vm.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (vm.compras.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sin compras registradas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                    Text("Las compras aparecer�n aqu�", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.compras) { compra ->
                    ComprasCard(compra)
                }
            }
        }
    }
}

@Composable
private fun ComprasCard(compra: CompraRegistro) {
    val fechaFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-MX")).format(Date(compra.fecha))
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(compra.insumoId, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("$${String.format("%.2f", compra.precioTotal)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Cantidad", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${compra.cantidadComprada}", fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Precio Unit.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%.2f", compra.precioUnitarioCompra)}", fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Fecha", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fechaFmt, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }
            if (compra.proveedorId.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Proveedor: ${compra.proveedorId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class CompraRegistro(
    val id: String = "",
    val insumoId: String = "",
    val cantidadComprada: Double = 0.0,
    val precioUnitarioCompra: Double = 0.0,
    val precioTotal: Double = 0.0,
    val proveedorId: String = "",
    val fecha: Long = 0L,
    val sucursalRecibe: String = ""
)

class ComprasViewModel : ViewModel() {
    private val db = FirebaseFirestoreProvider.db
    var compras = mutableStateListOf<CompraRegistro>()
    var cargando: Boolean by mutableStateOf(true)
    
    init { cargarHistorial() }
    
    private fun cargarHistorial() {
        viewModelScope.launch {
            try {
                cargando = true
                val snap = db.collection(FirestoreCollections.COMPRAS)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .limit(50).get().await()
                compras.clear()
                snap.documents.forEach { doc ->
                    compras.add(CompraRegistro(
                        id = doc.id,
                        insumoId = doc.getString("insumoId") ?: "",
                        cantidadComprada = doc.getDouble("cantidadComprada") ?: 0.0,
                        precioUnitarioCompra = doc.getDouble("precioUnitarioCompra") ?: 0.0,
                        precioTotal = doc.getDouble("precioTotal") ?: 0.0,
                        proveedorId = doc.getString("proveedorId") ?: "",
                        fecha = doc.getLong("fecha") ?: 0L,
                        sucursalRecibe = doc.getString("sucursalRecibe") ?: ""
                    ))
                }
            } catch (e: Exception) { /* handle error */ }
            finally { cargando = false }
        }
    }
}



