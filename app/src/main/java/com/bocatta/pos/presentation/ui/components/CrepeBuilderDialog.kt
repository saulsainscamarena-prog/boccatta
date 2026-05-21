package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2
import com.bocatta.pos.presentation.ui.theme.*
import java.math.BigDecimal
import java.util.Locale

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
    val bases: List<String> = emptyList(),
    val aderezos: List<String> = emptyList(),
    val toppings: List<String> = emptyList(),
    val esSalada: Boolean = false
)

// Datos reales del menu Bocatta conectados a opciones vendibles.
private val BASES_DULCES = listOf(
    "Nutella", "Lechera", "Zarzamora", "Mermelada Fresa", "Philadelphia"
)
private val BASES_SALADAS = listOf(
    "Tomate", "Philadelphia"
)
private val TOPPINGS_DULCES = listOf(
    "Durazno", "Fresa Natural", "Coco Rayado",
    "Granillo Chocolate", "Granillo Colores"
)
private val TOPPINGS_PREMIUM = listOf(
    "Oreo", "Bombon", "Nuez"
)
private val TOPPINGS_SALADOS = listOf(
    "Jamon", "Pina", "Peperoni"
)
private val ADEREZOS_SALADOS = listOf(
    "BBQ", "Buffalo", "Blue Cheese", "Valentina",
    "Queso Amarillo", "Catsup", "Mayonesa"
)
private val PRESETS_SALADOS = mapOf(
    "Hawaiana" to ConfigCrepa(bases = listOf("Tomate"), toppings = listOf("Jamon", "Pina"), esSalada = true),
    "Peperoni" to ConfigCrepa(bases = listOf("Tomate"), toppings = listOf("Peperoni"), esSalada = true),
    "Jamon con Philadelphia" to ConfigCrepa(bases = listOf("Philadelphia"), toppings = listOf("Jamon"), esSalada = true)
)

private fun basesDesdeTexto(base: String?): List<String> =
    base.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }.take(2)

private fun tiposCrepaParaProducto(producto: SalesInventoryProductV2): List<Boolean> {
    val nombre = producto.nombre.uppercase(Locale.ROOT)
    val categoria = producto.categoria.uppercase(Locale.ROOT)
    return when {
        producto.esCombo && (nombre.contains("DUO") || (nombre.contains("DULCE") && nombre.contains("SALADA"))) ->
            listOf(false, true)
        producto.esCombo && nombre.contains("SALADA") ->
            listOf(true, true)
        producto.esCombo && nombre.contains("DULCE") ->
            listOf(false, false)
        categoria.contains("SALADA") || nombre.contains("SALADA") ->
            listOf(true)
        else ->
            listOf(false)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrepeBuilderDialog(
    producto: SalesInventoryProductV2,
    vmV2: SalesViewModelV2,
    sucursal: String,
    itemInicial: ItemCarritoV2? = null,
    onDismiss: () -> Unit,
    onAddToCart: (SalesInventoryProductV2, String?, List<String>, List<String>, Boolean, List<ItemCarritoV2>) -> Unit
) {
    val tiposCrepa = remember(producto.id, producto.nombre, producto.categoria, producto.esCombo) {
        tiposCrepaParaProducto(producto)
    }
    val numConfiguraciones = tiposCrepa.size

    var currentConfigIndex by remember { mutableIntStateOf(0) }
    val configs = remember(tiposCrepa, itemInicial?.cartId) {
        mutableStateListOf<ConfigCrepa>().apply {
            val iniciales = itemInicial?.componentesCombo?.takeIf { it.isNotEmpty() } ?: itemInicial?.let { listOf(it) }.orEmpty()
            tiposCrepa.forEachIndexed { index, esSalada ->
                val item = iniciales.getOrNull(index)
                add(
                    ConfigCrepa(
                        bases = basesDesdeTexto(item?.base),
                        aderezos = item?.aderezos ?: emptyList(),
                        toppings = item?.toppings ?: emptyList(),
                        esSalada = esSalada
                    )
                )
            }
        }
    }
    LaunchedEffect(configs.size) {
        if (configs.isNotEmpty() && currentConfigIndex > configs.lastIndex) {
            currentConfigIndex = configs.lastIndex
        }
    }

    var esSeparado by remember(itemInicial?.cartId) { mutableStateOf(itemInicial?.esSeparado ?: false) }
    val scrollState = rememberScrollState()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(8.dp).fillMaxWidth().heightIn(max = 720.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(18.dp).verticalScroll(scrollState)) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val configActual = configs[currentConfigIndex.coerceIn(0, configs.lastIndex)]
                    Surface(
                        color = if (configActual.esSalada) MaterialTheme.colorScheme.primary.copy(0.1f)
                        else MaterialTheme.colorScheme.tertiary.copy(0.1f),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (configActual.esSalada) Icons.Default.LunchDining else Icons.Default.Icecream,
                                null,
                                tint = if (configActual.esSalada) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (numConfiguraciones > 1)
                                "CREPA ${currentConfigIndex + 1} DE $numConfiguraciones"
                            else "PERSONALIZA TU ORDEN",
                            fontWeight = FontWeight.Black, fontSize = 11.sp,
                            color = if (configActual.esSalada) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary,
                            letterSpacing = 1.sp
                        )
                        Text(producto.nombre.uppercase(), fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Indicadores de tabs para combos
                if (numConfiguraciones > 1) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(numConfiguraciones) { i ->
                            val active = currentConfigIndex == i
                            Surface(
                                onClick = { currentConfigIndex = i },
                                color = if (active) {
                                    if (configs[i].esSalada) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.tertiary
                                } else MaterialTheme.colorScheme.onSurface.copy(0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(4.dp)
                            ) {}
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                val safeIndex = currentConfigIndex.coerceIn(0, configs.lastIndex)
                val currentConfig = configs[safeIndex]

                // TIPO — toggle dulce/salada por crepa
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TIPO:", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = !currentConfig.esSalada,
                        onClick = { },
                        label = { Text("Dulce") },
                        enabled = !currentConfig.esSalada,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = currentConfig.esSalada,
                        onClick = { },
                        label = { Text("Salada") },
                        enabled = currentConfig.esSalada,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (currentConfig.esSalada) {
                    SectionTitle("PREPARACION",
                        MaterialTheme.colorScheme.primary)
                    FlowRow(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PRESETS_SALADOS.forEach { (nombre, preset) ->
                            val isSelected = currentConfig.bases == preset.bases &&
                                currentConfig.toppings.containsAll(preset.toppings)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    configs[safeIndex] = currentConfig.copy(
                                        bases = preset.bases,
                                        toppings = preset.toppings,
                                        esSalada = true
                                    )
                                },
                                label = { Text(nombre) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // BASE
                val basesOpts = if (currentConfig.esSalada) BASES_SALADAS else BASES_DULCES
                SectionTitle("BASE / UNTABLE",
                    if (currentConfig.esSalada) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary)
                FlowRow(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    basesOpts.forEach { b ->
                        val isSelected = currentConfig.bases.contains(b)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val nuevasBases = when {
                                    isSelected -> currentConfig.bases - b
                                    currentConfig.bases.size < 2 -> currentConfig.bases + b
                                    else -> currentConfig.bases
                                }
                                configs[safeIndex] = currentConfig.copy(bases = nuevasBases)
                            },
                            label = { Text(b) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (currentConfig.esSalada)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ADEREZOS — multiselección real
                if (currentConfig.esSalada) {
                    SectionTitle("ADEREZOS (elige varios)",
                        MaterialTheme.colorScheme.primary)
                    FlowRow(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ADEREZOS_SALADOS.forEach { a ->
                            val isSelected = currentConfig.aderezos.contains(a)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newList = if (isSelected) currentConfig.aderezos - a
                                    else currentConfig.aderezos + a
                                    configs[safeIndex] = currentConfig.copy(aderezos = newList)
                                },
                                label = { Text(a) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // TOPPINGS — datos reales del menú, no hardcoded
                val toppingsOpts = if (currentConfig.esSalada) TOPPINGS_SALADOS
                else TOPPINGS_DULCES + TOPPINGS_PREMIUM

                SectionTitle("TOPPINGS",
                    if (currentConfig.esSalada) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    toppingsOpts.forEach { topping ->
                        val isSelected = currentConfig.toppings.contains(topping)
                        val isPremium = TOPPINGS_PREMIUM.contains(topping)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newList = if (isSelected) currentConfig.toppings - topping
                                else currentConfig.toppings + topping
                                configs[safeIndex] = currentConfig.copy(toppings = newList)
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(topping, fontSize = 13.sp)
                                    if (isPremium) {
                                        Spacer(Modifier.width(4.dp))
                                        Box(
                                            Modifier
                                                .background(MaterialTheme.colorScheme.tertiary.copy(0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "★",
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Opción separar crepas en combos
                if (producto.esCombo && numConfiguraciones > 1) {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Servir por separado", fontSize = 14.sp)
                        Switch(checked = esSeparado, onCheckedChange = { esSeparado = it })
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Botones de acción
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (numConfiguraciones > 1 && currentConfigIndex < numConfiguraciones - 1) {
                        Button(
                            onClick = { currentConfigIndex++ },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("CONFIGURAR CREPA ${currentConfigIndex + 2}",
                                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.surface)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente",
                                tint = MaterialTheme.colorScheme.surface)
                        }
                    } else {
                        OutlinedButton(onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(
                                "Cerrar",
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (numConfiguraciones == 1) {
                                    val conf = configs[0]
                                    val baseTexto = conf.bases.joinToString(", ").ifBlank { null }
                                    onAddToCart(producto, baseTexto, conf.aderezos,
                                        conf.toppings, esSeparado, emptyList())
                                } else {
                                    val subItems = configs.mapIndexed { index, conf ->
                                        val baseTexto = conf.bases.joinToString(", ")
                                        ItemCarritoV2(
                                            producto = producto.copy(
                                                id = "${producto.id}_crepa_${index + 1}",
                                                nombre = "Crepa ${if (conf.esSalada) "Salada" else "Dulce"}"
                                            ),
                                            nombre = buildString {
                                                append("Crepa ${if (conf.esSalada) "Salada" else "Dulce"}")
                                                if (baseTexto.isNotBlank()) append(" c/$baseTexto")
                                                if (conf.toppings.isNotEmpty())
                                                    append(" + ${conf.toppings.joinToString(", ")}")
                                            },
                                            precioFinal = BigDecimal.ZERO,
                                            base = baseTexto,
                                            aderezos = conf.aderezos,
                                            toppings = conf.toppings
                                        )
                                    }
                                    onAddToCart(producto, null, emptyList(),
                                        emptyList(), esSeparado, subItems)
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentConfig.esSalada)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, "Agregar",
                                tint = MaterialTheme.colorScheme.surface)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "AGREGAR",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.surface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
