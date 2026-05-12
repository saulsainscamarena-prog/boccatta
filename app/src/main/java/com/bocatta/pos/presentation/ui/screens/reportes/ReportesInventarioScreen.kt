package com.bocatta.pos.presentation.ui.screens.reportes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.bocatta.pos.presentation.viewmodel.ReportesInventarioViewModel
import com.bocatta.pos.presentation.ui.theme.BocattaDanger
import com.bocatta.pos.presentation.ui.theme.BocattaPrimary
import com.bocatta.pos.presentation.ui.theme.BocattaWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesInventarioScreen(onBack: () -> Unit) {
    val vm: ReportesInventarioViewModel = koinViewModel()
    
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reportes de Inventario", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (vm.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Text("Consumo �ltimos 7 d�as", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                
                item { 
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            vm.consumoInsumos.forEach { (insumo, cantidad) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(insumo, fontSize = 14.sp)
                                    Text("${cantidad.toInt()} u.", fontWeight = FontWeight.Bold, color = BocattaPrimary)
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(8.dp)); Text("Alertas de Stock", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                
                items(vm.alertas) { alerta ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text(alerta.insumo, fontWeight = FontWeight.Bold); Text("Quedan ${alerta.diasRestantes} d�as", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Surface(color = if (alerta.esCritico) BocattaDanger else BocattaWarning, shape = MaterialTheme.shapes.small) { Text("CR�TICO", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

