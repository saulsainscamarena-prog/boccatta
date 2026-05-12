package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.presentation.ui.theme.BocattaPrimary
import com.bocatta.pos.presentation.ui.theme.BocattaSuccess
import java.math.BigDecimal

@Composable
fun CarritoPanelV2(
    carrito: List<ItemCarritoV2>,
    totalCarrito: BigDecimal,
    descuentoLealtad: Double,
    descuentoPromociones: Double,
    clienteSeleccionado: ClienteV2?,
    modifier: Modifier = Modifier,
    onEliminarItem: (ItemCarritoV2) -> Unit,
    onCobrar: () -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "Orden en curso",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (clienteSeleccionado != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = BocattaSuccess.copy(0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Cliente", tint = BocattaSuccess, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        clienteSeleccionado.nombre,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BocattaSuccess
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (carrito.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BocattaEmptyState(
                    icono = Icons.Default.ShoppingCartCheckout,
                    titulo = "Sin productos",
                    descripcion = "Toca un producto para agregarlo"
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(carrito, key = { it.cartId }) { item ->
                    BocattaCartItemRow(
                        item = item,
                        onEliminar = { onEliminarItem(item) }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (descuentoPromociones > 0) {
            BocattaFilaResumen(
                "Descuento promo",
                "-$${"%.2f".format(descuentoPromociones)}",
                colorValor = BocattaSuccess
            )
        }
        if (descuentoLealtad > 0) {
            BocattaFilaResumen(
                "Descuento lealtad",
                "-$${"%.2f".format(descuentoLealtad)}",
                colorValor = BocattaSuccess
            )
        }

        val totalFinal = (totalCarrito.toDouble() - descuentoLealtad
            - descuentoPromociones).coerceAtLeast(0.0)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "$${"%.2f".format(totalFinal)}",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = BocattaPrimary
            )
        }

        Spacer(Modifier.height(12.dp))
        BocattaButton(
            texto = "Finalizar pedido",
            onClick = onCobrar,
            modifier = Modifier.fillMaxWidth(),
            enabled = carrito.isNotEmpty()
        )
    }
}
