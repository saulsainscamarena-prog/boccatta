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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.RegistroCompraV2
import com.bocatta.pos.presentation.viewmodel.ComprasViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprasScreen(
    onBack: () -> Unit,
    vm: ComprasViewModel = koinViewModel()
) {
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
                    Text("Las compras apareceran aqui", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.compras, key = { it.id }) { compra ->
                    ComprasCard(compra)
                }
            }
        }
    }
}

@Composable
private fun ComprasCard(compra: RegistroCompraV2) {
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

