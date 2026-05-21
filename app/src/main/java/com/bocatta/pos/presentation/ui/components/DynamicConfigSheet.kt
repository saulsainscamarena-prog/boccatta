package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ConfigFieldType
import com.bocatta.pos.domain.model.ConfigOptionGroup
import com.bocatta.pos.domain.model.ConfigResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DynamicConfigSheet(
    configGroups: List<ConfigOptionGroup>,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onConfirm: (ConfigResult) -> Unit,
    onDismiss: () -> Unit
) {
    // State: key → list of selected values
    val selections = remember(configGroups) {
        mutableStateMapOf<String, List<String>>().apply {
            configGroups.forEach { group ->
                val initial = if (group.defaultValue != null) listOf(group.defaultValue) else emptyList()
                put(group.key, initial)
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(8.dp).fillMaxWidth().heightIn(max = 720.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())
            ) {
                configGroups.forEach { group ->
                    ConfigGroupSection(
                        group = group,
                        selected = selections[group.key] ?: emptyList(),
                        accentColor = accentColor,
                        onSelectionChanged = { values -> selections[group.key] = values }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("CANCELAR") }
                    Button(
                        onClick = { onConfirm(selections.toMap()) },
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, "Comprar", tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("CONFIRMAR", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigGroupSection(
    group: ConfigOptionGroup,
    selected: List<String>,
    accentColor: Color,
    onSelectionChanged: (List<String>) -> Unit
) {
    Text(
        group.title.uppercase(),
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        color = accentColor,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(accentColor.copy(0.2f)))
    Spacer(Modifier.height(10.dp))

    when (group.type) {
        ConfigFieldType.SINGLE_CHIP -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.options.forEach { opt ->
                    val isSelected = selected.contains(opt)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectionChanged(listOf(opt)) },
                        label = { Text(opt) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    )
                }
            }
        }

        ConfigFieldType.MULTI_CHIP -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.options.forEach { opt ->
                    val isSelected = selected.contains(opt)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newList = if (isSelected) selected - opt
                            else if (group.multiMax != null && selected.size >= group.multiMax) selected
                            else selected + opt
                            onSelectionChanged(newList)
                        },
                        label = { Text(opt) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            labelColor = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    )
                }
            }
        }

        ConfigFieldType.MULTI_CHECKBOX -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.options.forEach { opt ->
                    val isSelected = selected.contains(opt)
                    Surface(
                        onClick = {
                            val newList = if (isSelected) selected - opt
                            else if (group.multiMax != null && selected.size >= group.multiMax) selected
                            else selected + opt
                            onSelectionChanged(newList)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSelected) accentColor.copy(0.1f) else MaterialTheme.colorScheme.onSurface.copy(0.03f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(0.4f)
                        )
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(opt, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        ConfigFieldType.TEXT -> {
            var text by remember(group.key) { mutableStateOf(selected.firstOrNull() ?: "") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; onSelectionChanged(listOf(it)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    focusedLabelColor = accentColor
                )
            )
        }
    }
}


