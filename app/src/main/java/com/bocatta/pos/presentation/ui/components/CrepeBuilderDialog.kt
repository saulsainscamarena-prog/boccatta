package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.data.repository.InventoryDeductions
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2
import com.bocatta.pos.presentation.ui.theme.*
import java.math.BigDecimal

@Composable
fun SectionTitle(title: String, color: Color) {
    Column {
        Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = color, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(0.2f)))
        Spacer(Modifier.height(12.dp))
    }
}

data class ConfigCrepa(
    val base: String? = null,
    val aderezos: List<String> = emptyList(),
    val toppings: List<String> = emptyList(),
    val esSalada: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrepeBuilderDialog(
    producto: SalesInventoryProductV2,
    vmV2: SalesViewModelV2,
    sucursal: String,
    onDismiss: () -> Unit,
    onAddToCart: (SalesInventoryProductV2, String?, List<String>, List<String>, Boolean, List<ItemCarritoV2>) -> Unit
) {
    // Si es combo, determinamos cuántas configuraciones necesitamos
    // Por ahora asumimos que los productos en combo que terminan en 'CREPA' o 'CREPAS' son los que se configuran
    val numConfiguraciones = if (producto.esCombo) {
        // En Bocatta un combo suele tener 1 o 2 crepas configurables
        if (producto.nombre.uppercase().contains("2 CREPAS")) 2 else 1
    } else 1

    var currentConfigIndex by remember { mutableIntStateOf(0) }
    val configs = remember { 
        mutableStateListOf<ConfigCrepa>().apply {
            repeat(numConfiguraciones) { 
                add(ConfigCrepa(esSalada = producto.categoria.uppercase().contains("SALADA"))) 
            }
        }
    }
    
    var esSeparado by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(scrollState)
            ) {
                // Header con Selector de Configuración si es Combo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val configActual = configs[currentConfigIndex]
                    Surface(
                        color = if(configActual.esSalada) MaterialTheme.colorScheme.primary.copy(0.1f) else MaterialTheme.colorScheme.tertiary.copy(0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if(configActual.esSalada) Icons.Default.LunchDining else Icons.Default.Icecream, 
                                null, 
                                tint = if(configActual.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (numConfiguraciones > 1) "CONFIGURANDO ITEM ${currentConfigIndex + 1} DE $numConfiguraciones" 
                            else "ORDEN PERSONALIZADA", 
                            fontWeight = FontWeight.Black, 
                            fontSize = 11.sp, 
                            color = if(configActual.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, 
                            letterSpacing = 1.sp
                        )
                        Text(producto.nombre.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (numConfiguraciones > 1) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(numConfiguraciones) { i ->
                            val active = currentConfigIndex == i
                            Surface(
                                onClick = { currentConfigIndex = i },
                                color = if (active) (if(configs[i].esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary) else MaterialTheme.colorScheme.onSurface.copy(0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(4.dp)
                            ) {}
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                val currentConfig = configs[currentConfigIndex]

                // SECCIÓN: TIPO (Dulce/Salada) - Solo si el producto lo permite o es combo dinámico
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TIPO DE CREPA:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = !currentConfig.esSalada,
                        onClick = { configs[currentConfigIndex] = currentConfig.copy(esSalada = false) },
                        label = { Text("DULCE") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.tertiary, selectedLabelColor = MaterialTheme.colorScheme.onTertiary)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = currentConfig.esSalada,
                        onClick = { configs[currentConfigIndex] = currentConfig.copy(esSalada = true) },
                        label = { Text("SALADA") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // SECCIÓN: BASE
                SectionTitle("BASE / UNTABLE", if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                val bases = if(currentConfig.esSalada) listOf("Sin Base", "Salsa Tomate", "Queso Crema") else listOf("Sin Base", "Nutella", "Philadelphia", "Lechera", "Mermelada Fresa")
                
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bases.forEach { b ->
                        val isSelected = (currentConfig.base == b) || (currentConfig.base == null && b == "Sin Base")
                        FilterChip(
                            selected = isSelected,
                            onClick = { configs[currentConfigIndex] = currentConfig.copy(base = if(b == "Sin Base") null else b) },
                            label = { Text(b) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // SECCIÓN: ADEREZOS (MULTI-SELECCIÓN)
                SectionTitle("ADEREZOS (VARIOS)", if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                val aderezos = if(currentConfig.esSalada) 
                    listOf("Mayonesa", "Catsup", "Mostaza", "Chipotle", "Ranch", "Valentina")
                else 
                    listOf("Cajeta", "Hershey's", "Canela", "Azúcar Glass")

                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    aderezos.forEach { a ->
                        val isSelected = currentConfig.aderezos.contains(a)
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                val newList = if (isSelected) currentConfig.aderezos - a else currentConfig.aderezos + a
                                configs[currentConfigIndex] = currentConfig.copy(aderezos = newList)
                            },
                            label = { Text(a) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // SECCIÓN: TOPPINGS
                SectionTitle("TOPPINGS", if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
    // Hardcoded placeholder toppings list
    val toppingsOpts = listOf("Fresa", "Nutella", "Oreo", "Choco Chips")
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (topping in toppingsOpts) {
            val isSelected = currentConfig.toppings.contains(topping)
            Surface(
                onClick = {
                    val newList = if (isSelected) currentConfig.toppings - topping else currentConfig.toppings + topping
                    configs[currentConfigIndex] = currentConfig.copy(toppings = newList)
                },
                modifier = Modifier.fillMaxWidth(),
                color = if (isSelected) (if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary).copy(0.1f) else MaterialTheme.colorScheme.onSurface.copy(0.03f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isSelected) (if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary) else MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                    )
                    Spacer(Modifier.width(8.dp))
Text(topping, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                }
            }
        }


                Spacer(Modifier.height(32.dp))

                // Footer Actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (numConfiguraciones > 1 && currentConfigIndex < numConfiguraciones - 1) {
                        Button(
                            onClick = { currentConfigIndex++ },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("SIGUIENTE ITEM", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.surface)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente", tint = MaterialTheme.colorScheme.surface)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("CANCELAR", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Button(
                            onClick = { 
                                if (numConfiguraciones == 1) {
                                    val conf = configs[0]
                                    onAddToCart(producto, conf.base, conf.aderezos, conf.toppings, esSeparado, emptyList())
                                } else {
                                    // Crear sub-items para el combo
                                    val subItems = configs.map { conf ->
                                        ItemCarritoV2(
                                            producto = SalesInventoryProductV2(nombre = "CREPA ${if(conf.esSalada) "SALADA" else "DULCE"}"),
                                            nombre = "Crepa ${if(conf.esSalada) "Salada" else "Dulce"}",
                                            precioFinal = BigDecimal.ZERO,
                                            base = conf.base ?: "",
                                            aderezos = conf.aderezos,
                                            toppings = conf.toppings
                                        )
                                    }
                                    onAddToCart(producto, null, emptyList(), emptyList(), esSeparado, subItems)
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if(currentConfig.esSalada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, "Comprar", tint = MaterialTheme.colorScheme.surface)
                            Spacer(Modifier.width(8.dp))
                            Text("CONFIRMAR TODO", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        }
    }
}
}


