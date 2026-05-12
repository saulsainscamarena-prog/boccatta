package com.bocatta.pos.domain.unit

/**
 * Motor de conversión de unidades inmutable y agnóstico al giro.
 * Diseñado para ser usado en el dominio, repositories y data layer.
 * Soporta exclusivamente el sistema métrico decimal (México).
 */
object UnitConverter {

    // Registro de unidades soportadas y sus factores a la unidad base
    // MASA -> base: gramos (g)
    // VOLUMEN -> base: mililitros (ml)
    // CONTABLE -> base: pieza (pza)
    private val registry = mapOf(
        // MASA
        "g" to UnitDef("g", Dimension.MASA, 1.0),
        "kg" to UnitDef("kg", Dimension.MASA, 1000.0),
        "lb" to UnitDef("lb", Dimension.MASA, 453.592), // Soporte para proveedores
        
        // VOLUMEN
        "ml" to UnitDef("ml", Dimension.VOLUMEN, 1.0),
        "L" to UnitDef("L", Dimension.VOLUMEN, 1000.0),
        
        // CONTABLE
        "pza" to UnitDef("pza", Dimension.CONTABLE, 1.0),
        "docena" to UnitDef("docena", Dimension.CONTABLE, 12.0)
    )

    enum class Dimension { MASA, VOLUMEN, CONTABLE }
    
    data class UnitDef(
        val code: String,
        val dimension: Dimension,
        val factorToBase: Double
    )

    /**
     * Convierte una cantidad a la unidad base del sistema.
     * Ej: 1.5 kg -> 1500 g
     */
    fun toBase(value: Double, unit: String): Double {
        val def = registry[unit] ?: throw IllegalArgumentException("Unidad no soportada: $unit")
        return value * def.factorToBase
    }

    /**
     * Convierte desde la unidad base a una unidad de visualización/venta.
     * Ej: 1500 g -> 1.5 kg
     */
    fun fromBase(baseValue: Double, targetUnit: String): Double {
        val def = registry[targetUnit] ?: throw IllegalArgumentException("Unidad no soportada: $targetUnit")
        return baseValue / def.factorToBase
    }

    /**
     * Convierte directamente entre dos unidades compatibles.
     * @throws IllegalArgumentException si las unidades no existen o son de dimensiones distintas
     */
    fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        val fromDef = registry[fromUnit] ?: throw IllegalArgumentException("Unidad origen no soportada: $fromUnit")
        val toDef = registry[toUnit] ?: throw IllegalArgumentException("Unidad destino no soportada: $toUnit")

        require(fromDef.dimension == toDef.dimension) {
            "Error de dimensión: No se puede convertir ${fromDef.dimension} a ${toDef.dimension}"
        }

        // Paso intermedio por unidad base para precisión
        val baseValue = value * fromDef.factorToBase
        return baseValue / toDef.factorToBase
    }
    
    /**
     * Verifica si una unidad está registrada en el sistema.
     */
    fun isUnitSupported(unit: String): Boolean = registry.containsKey(unit)
    
    /**
     * Obtiene la dimensión de una unidad registrada.
     */
    fun getDimension(unit: String): Dimension? = registry[unit]?.dimension

    /**
     * Obtiene la unidad base para una dimensión.
     */
    fun getBaseUnit(unit: String): String {
        val def = registry[unit] ?: throw IllegalArgumentException("Unidad no soportada: $unit")
        return when (def.dimension) {
            Dimension.MASA -> "g"
            Dimension.VOLUMEN -> "ml"
            Dimension.CONTABLE -> "pza"
        }
    }
}