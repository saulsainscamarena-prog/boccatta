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
        shape = RoundedCornerShape(28.dp), // Meridian Spec: 28dp (píldora) for product cards
        color = if (agotado) MaterialTheme.colorScheme.surfaceVariant.copy(0.3f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (agotado) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Meridian Spec: 1:1
            .padding(4.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (agotado) listOf(Color.Transparent, Color.Transparent) 
                             else listOf(colorCat.copy(0.3f), Color.Transparent)
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
                    fontWeight = FontWeight.SemiBold, // Meridian Spec: SemiBold
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    fontSize = 16.sp, // Meridian Spec: 16sp
                    color = if (agotado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Badge de Precio - Estilo Meridian (Pastilla Negra/Primary)
                Surface(
                    color = if (agotado) Color.Gray else BocattaPrimary, // Using BocattaPrimary for active prices
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$${"%.0f".format(precio)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 20.sp, // Meridian Spec: 20sp for prices
                        fontWeight = FontWeight.SemiBold // Meridian Spec: SemiBold
                    )
                }
            }
            
            // Indicadores de Estado (Meridian Spec: 10% alpha bg)
            if (agotado) {
                StatusBadgePremium("AGOTADO", BocattaDanger, Modifier.align(Alignment.TopStart))
            } else if (pocoStock) {
                StatusBadgePremium("BAJO", BocattaWarning, Modifier.align(Alignment.TopStart))
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
        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
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
    color: Color = BocattaPrimary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp).shadow(if(enabled) 12.dp else 0.dp, RoundedCornerShape(16.dp), ambientColor = color, spotColor = color),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(texto, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 16.sp)
    }
}
