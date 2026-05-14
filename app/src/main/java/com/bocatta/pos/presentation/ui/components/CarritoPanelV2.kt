package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.presentation.ui.theme.*
import java.math.BigDecimal

@Composable
fun CarritoPanelV2(
    carrito: List<ItemCarritoV2>,
    totalCarrito: BigDecimal,
    descuentoLealtad: Double,
    descuentoPromociones: Double,
    descuentoManual: Double = 0.0,
    clienteSeleccionado: ClienteV2?,
    modifier: Modifier = Modifier,
    onEliminarItem: (ItemCarritoV2) -> Unit,
    onEditarItem: ((ItemCarritoV2) -> Unit)? = null,
    onCobrar: () -> Unit,
    onApplyDiscount: ((Int) -> Unit)? = null,
    onApartar: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        modifier = modifier.fillMaxHeight(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "ORDEN ACTUAL",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
            }
            
            if (clienteSeleccionado != null) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            clienteSeleccionado.nombre.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))

            if (carrito.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BocattaEmptyState(
                        icono = Icons.Default.ShoppingCartCheckout,
                        titulo = "CARRITO VACÍO",
                        descripcion = "Agrega productos del menú para comenzar",
                        modifier = Modifier.alpha(0.5f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(carrito, key = { it.cartId }) { item ->
                        BocattaCartItemRow(
                            item = item,
                            onEliminar = { onEliminarItem(item) },
                            onEditar = onEditarItem?.let { { it(item) } }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Resumen con Glassmorphism
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(0.03f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (descuentoPromociones > 0) {
                        BocattaFilaResumen(
                            "DESCUENTO PROMO",
                            "-$${"%.2f".format(descuentoPromociones)}",
                            colorValor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (descuentoLealtad > 0) {
                        BocattaFilaResumen(
                            "DESCUENTO LEALTAD",
                            "-$${"%.2f".format(descuentoLealtad)}",
                            colorValor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (descuentoManual > 0) {
                        BocattaFilaResumen(
                            "DESCUENTO MANUAL",
                            "-$${"%.2f".format(descuentoManual)}",
                            colorValor = MaterialTheme.colorScheme.error
                        )
                    }

                    val totalFinal = (totalCarrito.toDouble() - descuentoLealtad
                        - descuentoPromociones - descuentoManual).coerceAtLeast(0.0)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("SUBTOTAL", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.3f), letterSpacing = 1.sp)
                            Text("$${"%.2f".format(totalCarrito)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL FINAL", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(0.7f), letterSpacing = 1.sp)
                            Text(
                                "$${"%.2f".format(totalFinal)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Descuento manual
            if (onApplyDiscount != null) {
                Text("DESCUENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30).forEach { pct ->
                        FilterChip(
                            selected = descuentoManual == (totalCarrito.toDouble() - descuentoLealtad - descuentoPromociones).coerceAtLeast(0.0) * pct / 100.0,
                            onClick = { onApplyDiscount(pct) },
                            label = { Text("$pct%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.error,
                                selectedLabelColor = MaterialTheme.colorScheme.onError
                            )
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { onApplyDiscount(0) },
                        label = { Text("Quitar") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(24.dp))
            
            if (onApartar != null && carrito.isNotEmpty()) {
                OutlinedButton(
                    onClick = onApartar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("APARTAR ORDEN", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
            }

            NeonButton(
                texto = "FINALIZAR PEDIDO",
                onClick = onCobrar,
                modifier = Modifier.fillMaxWidth(),
                enabled = carrito.isNotEmpty(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
