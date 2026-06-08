package com.bocatta.pos.domain.usecase

object BusinessLogicFactory {

    fun getProductionEngine(giro: String): ProductionEngine {
        return when (giro.uppercase(java.util.Locale.getDefault())) {
            "FOOD" -> FoodProductionEngine()
            "RETAIL" -> RetailKitEngine()
            "SERVICE" -> ServiceTaskEngine()
            else -> FoodProductionEngine()
        }
    }

    fun getProductionEngineOrDefault(giro: String?): ProductionEngine {
        return getProductionEngine(giro ?: "FOOD")
    }
}

