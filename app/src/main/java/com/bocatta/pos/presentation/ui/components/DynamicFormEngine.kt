package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.InventoryProductV2

sealed class FormField(
    val key: String,
    val label: String,
    val defaultValue: Any?
) {
    class TextField(key: String, label: String, defaultValue: String? = null) : FormField(key, label, defaultValue)
    class NumberField(key: String, label: String, defaultValue: Double? = null) : FormField(key, label, defaultValue)
    class BooleanField(key: String, label: String, defaultValue: Boolean? = null) : FormField(key, label, defaultValue)
    class SelectField(key: String, label: String, val options: List<String>, defaultValue: String? = null) : FormField(key, label, defaultValue)
    class ListField(key: String, label: String, val itemLabel: String = "Item") : FormField(key, label, emptyList<String>())
}

data class GiroSchema(
    val giro: String,
    val label: String,
    val fields: List<FormField>
)

object DynamicFormEngine {

    private val schemas: Map<String, GiroSchema> = mapOf(
        "FOOD" to GiroSchema(
            giro = "FOOD",
            label = "Alimentos",
            fields = listOf(
                FormField.TextField("emoji", "Emoji", "🍩"),
                FormField.BooleanField("esCombo", "Es Combo", false),
                FormField.TextField("recetaId", "ID Receta", null),
                FormField.NumberField("toppingsIncluidos", "Toppings Incluidos", 2.0),
                FormField.NumberField("costoToppingExtra", "Costo Topping Extra", 10.0),
                FormField.BooleanField("esProductoTopping", "Es Topping", false)
            )
        ),
        "RETAIL" to GiroSchema(
            giro = "RETAIL",
            label = "Tienda / Kit",
            fields = listOf(
                FormField.TextField("sku", "SKU", ""),
                FormField.BooleanField("esKit", "Es Kit", false),
                FormField.TextField("barcode", "Código de Barras", null),
                FormField.NumberField("taxRate", "Tasa de Impuesto %", 16.0),
                FormField.SelectField("unitType", "Unidad de Venta", listOf("pza", "kg", "L", "m"), "pza")
            )
        ),
        "SERVICE" to GiroSchema(
            giro = "SERVICE",
            label = "Servicios",
            fields = listOf(
                FormField.NumberField("durationMinutes", "Duración (min)", 60.0),
                FormField.BooleanField("requiresAppointment", "Requiere Cita", true),
                FormField.SelectField("serviceLocation", "Ubicación", listOf("local", "domicilio", "remoto"), "local"),
                FormField.BooleanField("trackMaterials", "Rastrear Materiales", false),
                FormField.TextField("notes", "Notas", null)
            )
        )
    )

    fun getSchema(giro: String): GiroSchema = schemas[giro.uppercase()] ?: schemas["FOOD"]!!

    fun getSupportedGiros(): List<GiroSchema> = schemas.values.toList()

    fun extractValues(product: InventoryProductV2): Map<String, Any?> {
        val schema = getSchema(product.giro)
        val values = mutableMapOf<String, Any?>()
        for (field in schema.fields) {
            // Asegurar que extraemos los valores de attributes o usamos el default
            val rawValue = product.attributes[field.key]
            values[field.key] = rawValue ?: field.defaultValue
        }
        return values
    }

    fun buildAttributes(schema: GiroSchema, values: Map<String, Any?>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for (field in schema.fields) {
            val value = values[field.key] ?: field.defaultValue
            if (value != null) {
                result[field.key] = when (field) {
                    is FormField.NumberField -> {
                        when (value) {
                            is Number -> value.toDouble()
                            is String -> value.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                    }
                    is FormField.BooleanField -> value as? Boolean ?: false
                    is FormField.TextField -> value.toString()
                    is FormField.SelectField -> value.toString()
                    is FormField.ListField -> @Suppress("UNCHECKED_CAST") (value as? List<String>) ?: emptyList<String>()
                }
            }
        }
        return result
    }
}

fun sanitizeLabel(label: String): String = label.replace(Regex("[\\x00-\\x1F\\x7F]"), "")

@Composable
fun DynamicProductForm(
    giro: String,
    initialValues: Map<String, Any?>,
    onValuesChanged: (Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val schema = DynamicFormEngine.getSchema(giro)
    val currentValues = remember(initialValues) { mutableStateMapOf<String, Any?>().apply { putAll(initialValues) } }

    LaunchedEffect(currentValues.toMap()) {
        onValuesChanged(currentValues.toMap())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Atributos: ${sanitizeLabel(schema.label)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        for (field in schema.fields) {
            val label = sanitizeLabel(field.label)
            when (field) {
                is FormField.TextField -> TextFormField(
                    label = label,
                    value = (currentValues[field.key] as? String) ?: field.defaultValue as? String ?: "",
                    onValueChange = { currentValues[field.key] = it },
                    readOnly = readOnly
                )
                is FormField.NumberField -> NumberFormField(
                    label = label,
                    value = (currentValues[field.key] as? Number)?.toDouble() ?: field.defaultValue as? Double ?: 0.0,
                    onValueChange = { currentValues[field.key] = it },
                    readOnly = readOnly
                )
                is FormField.BooleanField -> SwitchFormField(
                    label = label,
                    checked = (currentValues[field.key] as? Boolean) ?: field.defaultValue as? Boolean ?: false,
                    onCheckedChange = { currentValues[field.key] = it },
                    readOnly = readOnly
                )
                is FormField.SelectField -> SelectFormField(
                    label = label,
                    options = field.options,
                    selected = (currentValues[field.key] as? String) ?: field.defaultValue as? String ?: field.options.first(),
                    onSelected = { currentValues[field.key] = it },
                    readOnly = readOnly
                )
                is FormField.ListField -> {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Configuración multi-item disponible en edición completa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        enabled = !readOnly,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}

@Composable
private fun NumberFormField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    readOnly: Boolean
) {
    val textValue = remember(value) {
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
    var textState by remember(value) { mutableStateOf(textValue) }

    OutlinedTextField(
        value = textState,
        onValueChange = { newText ->
            textState = newText
            newText.toDoubleOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        readOnly = readOnly,
        enabled = !readOnly,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun SwitchFormField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    readOnly: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = !readOnly
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectFormField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    readOnly: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!readOnly) expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiroSelector(
    currentGiro: String,
    onGiroSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val schemas = DynamicFormEngine.getSupportedGiros()
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = schemas.find { it.giro == currentGiro }?.label ?: currentGiro

    Column(modifier = modifier) {
        Text(
            text = "Giro del Negocio",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                schemas.forEach { schema ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(schema.label, fontWeight = FontWeight.Medium)
                                Text(
                                    "${schema.fields.size} atributos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                )
                            }
                        },
                        onClick = {
                            onGiroSelected(schema.giro)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

