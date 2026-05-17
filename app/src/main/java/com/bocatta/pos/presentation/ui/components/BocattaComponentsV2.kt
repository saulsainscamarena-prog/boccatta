package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.presentation.ui.theme.*

@Composable
fun ProductCardV2(
    producto: SalesInventoryProductV2,
    sucursal: String,
    stockAlerta: Double,
    onClick: () -> Unit
) {
    val precio = producto.precioVenta[sucursal.lowercase()] ?: 0.0
    ProductCardPremium(
        nombre = producto.nombre,
        precio = precio,
        emoji = producto.emoji,
        categoria = producto.categoria,
        agotado = stockAlerta <= 0,
        pocoStock = stockAlerta in 0.1..<5.0,
        onClick = onClick
    )
}

/**
 * Tarjeta de Producto Premium con efecto Glassmorphism y Elevación Dinámica.
 */
@Composable
fun ProductCardPremium(
    nombre: String,
    precio: Double,
    emoji: String,
    categoria: String,
    agotado: Boolean,
    pocoStock: Boolean,
    onClick: () -> Unit
) {
    val colorCat = BocattaDesign.getColorPorCategoria(categoria)
    
    Surface(
        onClick = onClick,
        enabled = !agotado,
        shape = RoundedCornerShape(28.dp),
        color = if (agotado) MaterialTheme.colorScheme.onSurface.copy(0.05f) else MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (agotado) listOf(Color.Transparent, Color.Transparent) 
                             else listOf(colorCat.copy(0.4f), Color.Transparent)
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp), // Meridian Spec: 12dp grid
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp) // Meridian Spec: 8dp grid
            ) {
                // Contenedor del Emoji
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) { // Larger for visibility
                    Surface(
                        color = colorCat.copy(0.1f),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize()
                    ) { }
                    Text(text = emoji, fontSize = 32.sp)
                }
                
                Text(
                    text = nombre.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    fontSize = 14.sp,
                    color = if (agotado) MaterialTheme.colorScheme.onSurface.copy(0.3f) else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Badge de Precio - Estilo Meridian Neon
                Surface(
                    color = if (agotado) MaterialTheme.colorScheme.outlineVariant.copy(0.3f) else colorCat.copy(0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if(agotado) Color.Transparent else colorCat.copy(0.5f))
                ) {
                    Text(
                        text = "$${"%.0f".format(precio)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = if(agotado) MaterialTheme.colorScheme.onSurface.copy(0.3f) else colorCat,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            if (agotado) {
                StatusBadgePremium("AGOTADO", MaterialTheme.colorScheme.error, Modifier.align(Alignment.TopEnd))
            } else if (pocoStock) {
                StatusBadgePremium("BAJO", MaterialTheme.colorScheme.error, Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
fun StatusBadgePremium(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.1f), // Meridian Spec: 10% alpha
        shape = RoundedCornerShape(8.dp), // Meridian Spec: 4-8dp
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier.padding(8.dp)
    ) {
        Text(
            text = text,
            color = color, // Meridian Spec: Solid text
            fontSize = 12.sp, // Meridian Spec: 12sp for badges
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun NeonButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(texto, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 16.sp)
    }
}

@Composable
fun BocattaMetricCardPremium(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitulo: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                titulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                valor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            subtitulo?.let {
                Text(
                    it.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(0.6f),
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GastoCard(
    categoria: String,
    monto: Double,
    fecha: String,
    descripcion: String,
    usuario: String,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(categoria.uppercase(), fontWeight = FontWeight.Black, color = color, fontSize = 14.sp, letterSpacing = 1.sp)
                    Text(fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontWeight = FontWeight.Bold)
                }
                Text("$${"%.2f".format(monto)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
            }
            
            Text(descripcion.uppercase(), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f), lineHeight = 16.sp)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("REGISTRADO POR: $usuario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontWeight = FontWeight.Bold)
            }
        }
    }
}
