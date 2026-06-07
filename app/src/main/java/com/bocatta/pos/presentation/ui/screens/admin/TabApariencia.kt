package com.bocatta.pos.presentation.ui.screens.admin

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ThemeConfigV2
import com.bocatta.pos.presentation.ui.components.BocattaButton
import com.bocatta.pos.presentation.ui.components.BocattaSectionTitle
import com.bocatta.pos.presentation.ui.theme.parseHexColor
import com.bocatta.pos.presentation.ui.theme.previewCustomColorScheme
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import com.bocatta.pos.presentation.viewmodel.ThemeViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import org.koin.androidx.compose.koinViewModel

private data class ThemePreset(
    val nombre: String,
    val primary: String,
    val secondary: String,
    val tertiary: String
)

private val presets = listOf(
    ThemePreset("Bocatta", "#FFB394", "#5B4035", "#F2C078"),
    ThemePreset("Cacao", "#D8A37B", "#6B4A3A", "#E4C46F"),
    ThemePreset("Menta", "#7CCAA7", "#49655A", "#F0C36A"),
    ThemePreset("Frambuesa", "#F09AAB", "#65414A", "#E8BE74"),
    ThemePreset("Alto contraste", "#FFD0BC", "#D0C3BD", "#F4C95D")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabApariencia(
    session: SessionViewModel,
    vm: ThemeViewModel = koinViewModel()
) {
    val config by vm.config.collectAsStateWithLifecycle()
    var primary by remember(config) { mutableStateOf(config.primaryHex) }
    var secondary by remember(config) { mutableStateOf(config.secondaryHex) }
    var tertiary by remember(config) { mutableStateOf(config.tertiaryHex) }
    val previewConfig = ThemeConfigV2(
        enabled = true,
        primaryHex = primary,
        secondaryHex = secondary,
        tertiaryHex = tertiary
    )
    val previewScheme = previewCustomColorScheme(previewConfig, darkTheme = true)
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BocattaSectionTitle("Apariencia global")
            Text(
                "Estos colores afectan toda la app. Se guardan como colores base y el sistema genera los tonos Material 3 para mantener contraste.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Esquemas", fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    primary = preset.primary
                                    secondary = preset.secondary
                                    tertiary = preset.tertiary
                                },
                                label = { Text(preset.nombre) },
                                leadingIcon = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        ColorDot(preset.primary)
                                        ColorDot(preset.secondary)
                                        ColorDot(preset.tertiary)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            ColorField(
                title = "Color principal",
                description = "Botones importantes, pestanas activas, precio destacado y acciones como Cobrar.",
                value = primary,
                fallback = "#FFB394",
                onValueChange = { primary = it }
            )
            ColorField(
                title = "Color secundario",
                description = "Cards, contenedores suaves, chips y fondos de apoyo.",
                value = secondary,
                fallback = "#5B4035",
                onValueChange = { secondary = it }
            )
            ColorField(
                title = "Color terciario",
                description = "Acentos visuales, metricas, badges y resaltados secundarios.",
                value = tertiary,
                fallback = "#F2C078",
                onValueChange = { tertiary = it }
            )

            MaterialTheme(
                colorScheme = previewScheme,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes
            ) {
                PreviewCard()
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        primary = "#FFB394"
                        secondary = "#5B4035"
                        tertiary = "#F2C078"
                        vm.restaurar()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restaurar")
                }
                BocattaButton(
                    texto = "Guardar",
                    onClick = { vm.guardar(primary, secondary, tertiary, session.nombreUsuario) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    cargando = vm.cargando,
                    icono = Icons.Default.Save
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ColorField(
    title: String,
    description: String,
    value: String,
    fallback: String,
    onValueChange: (String) -> Unit
) {
    val parsed = parseHexColor(value)
    var mostrarPaleta by remember { mutableStateOf(false) }
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(parsed ?: parseHexColor(fallback) ?: MaterialTheme.colorScheme.outline, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { text ->
                        val cleaned = text.trim().uppercase()
                        onValueChange(if (cleaned.startsWith("#")) cleaned else "#$cleaned")
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("#RRGGBB") },
                    isError = parsed == null,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedButton(
                    onClick = { mostrarPaleta = !mostrarPaleta },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (mostrarPaleta) "Cerrar" else "Paleta")
                }
            }

            if (mostrarPaleta) {
                ColorWheelPicker(
                    selectedHex = parsed?.let { value } ?: fallback,
                    onColorSelected = onValueChange,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    "Toca la rueda para elegir tono e intensidad. El codigo se actualiza arriba.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorWheelPicker(
    selectedHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wheelSize = 228.dp
    val density = LocalDensity.current
    val sizePx = with(density) { wheelSize.roundToPx() }.coerceAtLeast(2)
    val wheel = remember(sizePx) { createColorWheelBitmap(sizePx) }
    val selectedOffset = remember(selectedHex, sizePx) {
        selectedWheelOffset(selectedHex, sizePx.toFloat())
    }

    Canvas(
        modifier = modifier
            .size(wheelSize)
            .pointerInput(sizePx) {
                fun selectColor(offset: Offset) {
                    val radius = sizePx / 2f
                    val dx = offset.x - radius
                    val dy = offset.y - radius
                    val distance = hypot(dx, dy)
                    if (distance <= radius) {
                        val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                        val saturation = (distance / radius).coerceIn(0f, 1f)
                        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f))
                        onColorSelected("#%06X".format(0xFFFFFF and argb))
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    selectColor(down.position)
                    val pointerId = down.id
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.first()
                        if (change.pressed) {
                            selectColor(change.position)
                            change.consume()
                        }
                    } while (event.changes.any { it.id == pointerId && it.pressed })
                }
            }
    ) {
        drawImage(wheel)
        drawCircle(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            radius = 10.dp.toPx(),
            center = selectedOffset,
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            radius = 13.dp.toPx(),
            center = selectedOffset,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun createColorWheelBitmap(size: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val radius = size / 2f
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = x - radius
            val dy = y - radius
            val distance = hypot(dx, dy)
            if (distance <= radius) {
                val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                val saturation = (distance / radius).coerceIn(0f, 1f)
                bitmap.setPixel(x, y, AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f)))
            } else {
                bitmap.setPixel(x, y, AndroidColor.TRANSPARENT)
            }
        }
    }
    return bitmap.asImageBitmap()
}

private fun selectedWheelOffset(hex: String, sizePx: Float): Offset {
    val clean = hex.trim().removePrefix("#")
    if (!Regex("^[0-9a-fA-F]{6}$").matches(clean)) {
        return Offset(sizePx / 2f, sizePx / 2f)
    }
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV((0xFF000000 or clean.toLong(16)).toInt(), hsv)
    val radius = sizePx / 2f * hsv[1].coerceIn(0f, 1f)
    val angle = Math.toRadians(hsv[0].toDouble())
    return Offset(
        x = sizePx / 2f + cos(angle).toFloat() * radius,
        y = sizePx / 2f + sin(angle).toFloat() * radius
    )
}

@Composable
private fun PreviewCard() {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Vista previa", fontWeight = FontWeight.Bold)
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Botones y acciones principales",
                    Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("Cobrar")
                }
                AssistChip(onClick = {}, label = { Text("Chip") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Card apoyo", Modifier.padding(12.dp))
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Metrica", Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ColorDot(hex: String) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(parseHexColor(hex) ?: MaterialTheme.colorScheme.outline, CircleShape)
    )
}
