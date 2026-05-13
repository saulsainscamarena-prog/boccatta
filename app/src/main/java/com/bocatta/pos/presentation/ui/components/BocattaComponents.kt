package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.presentation.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BocattaComponents — Biblioteca de componentes compartidos v1.0
 *
 * Regla: NINGÚN componente aquí debe importar nombres de pantalla específica.
 * Son bloques genéricos reutilizables en cualquier giro de negocio.
 */

// ── TOP BAR ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BocattaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                subtitle?.let {
                    Text(
                        it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
    )
}

// ── METRIC CARD ───────────────────────────────────────────────────────────────

/**
 * Tarjeta de métrica única. Reemplaza MetricCardPremium en CierreCajaScreen
 * y ReportScreen — que tenían el mismo nombre y causaban conflicto de scope.
 */
@Composable
fun BocattaMetricCard(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitulo: String? = null
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.55f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            subtitulo?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ── BOTÓN PRIMARIO ────────────────────────────────────────────────────────────

@Composable
fun BocattaButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !cargando,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            icono?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ── EMPTY STATE ───────────────────────────────────────────────────────────────

@Composable
fun BocattaEmptyState(
    icono: ImageVector,
    titulo: String,
    descripcion: String? = null,
    accion: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.35f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            titulo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
        )
        descripcion?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        accion?.invoke()
    }
}

// ── SECTION TITLE ─────────────────────────────────────────────────────────────

@Composable
fun BocattaSectionTitle(
    texto: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        accion?.invoke()
    }
}

// ── BADGE CHIP ────────────────────────────────────────────────────────────────

@Composable
fun BocattaBadge(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(0.12f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            texto.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

// ── LOADING OVERLAY ───────────────────────────────────────────────────────────

@Composable
fun BocattaLoadingDialog(mensaje: String = "Procesando...") {
    AlertDialog(
        onDismissRequest = {},
        title = null,
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
                Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {}
    )
}

// ── FILA DE RESUMEN (para cierres de caja, resúmenes) ─────────────────────────

@Composable
fun BocattaFilaResumen(
    etiqueta: String,
    valor: String,
    colorValor: Color = MaterialTheme.colorScheme.onSurface,
    negrita: Boolean = false,
    separador: Boolean = false
) {
    if (separador) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(if (negrita) 1f else 0.7f)
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (negrita) FontWeight.Black else FontWeight.SemiBold,
            color = colorValor
        )
    }
}

// ── NORMALIZACIÓN DE CATEGORÍAS (central, no duplicada) ──────────────────────

/**
 * Punto único de normalización de nombres de categoría para mostrar en UI.
 * SalesScreen la usa para los chips de filtro.
 * Elimina los dos when-expressions idénticos que existían antes.
 */
fun normalizarCategoria(categoria: String): String {
    return when (categoria.lowercase().trim()) {
        "crepas_dulces" -> "Crepas Dulces"
        "crepas_saladas" -> "Crepas Saladas"
        "crepa", "crepas" -> "Crepas"
        "snack", "snacks" -> "Snacks"
        "postre", "postres" -> "Postres"
        "combo", "combos", "paquetes" -> "Combos"
        "bebida", "bebidas", "frappes", "frappe" -> "Bebidas"
        "servicio", "servicios" -> "Servicios"
        else -> categoria.trim().replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun BocattaCartItemRow(
    item: ItemCarritoV2,
    onEliminar: () -> Unit,
    onEditar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(0.03f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.nombre.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                if (item.nota.isNotBlank()) {
                    Text(
                        item.nota.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp
                    )
                }
            }
            Text(
                "$${"%.2f".format(item.precioFinal.toDouble() * item.cantidad)}",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(8.dp))
            if (onEditar != null) {
                IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Create, contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.size(16.dp))
                }
            }
            IconButton(
                onClick = onEliminar,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(0.6f),
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun BocattaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Buscar...",
    debounceMs: Long = 300L,
    modifier: Modifier = Modifier,
    onSearch: ((String) -> Unit)? = null
) {
    var localQuery by remember(query) { mutableStateOf(query) }
    val scope = rememberCoroutineScope()
    val debounceJob = remember { mutableStateOf<Job?>(null) }

    OutlinedTextField(
        value = localQuery,
        onValueChange = { newValue ->
            localQuery = newValue
            debounceJob.value?.cancel()
            debounceJob.value = scope.launch {
                delay(debounceMs)
                onQueryChange(newValue)
                onSearch?.invoke(newValue)
            }
        },
        placeholder = {
            Text(placeholder.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.3f), letterSpacing = 1.sp)
        },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        leadingIcon = {
            Icon(Icons.Default.Search, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (localQuery.isNotEmpty()) {
                IconButton(onClick = {
                    localQuery = ""
                    onQueryChange("")
                    onSearch?.invoke("")
                }) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.White.copy(0.1f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = Color.White.copy(0.05f),
            focusedContainerColor = Color.White.copy(0.05f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}
