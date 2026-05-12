package com.bocatta.pos.di

import com.bocatta.pos.domain.usecase.SalesFlowUseCase
import com.bocatta.pos.domain.usecase.GenerarTicketWhatsAppUseCase
import com.bocatta.pos.domain.usecase.PromocionesEngine
import com.bocatta.pos.data.repository.PromocionesRepository
import com.bocatta.pos.domain.repository.SalesRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.repository.IInventoryRepository

data class SalesDependencies(
    val productRepo: IProductRepository,
    val inventoryRepo: IInventoryRepository,
    val salesFlowUseCase: SalesFlowUseCase,
    val repository: SalesRepository,
    val generarTicketWhatsAppUseCase: GenerarTicketWhatsAppUseCase,
    val promocionesEngine: PromocionesEngine,
    val promocionesRepository: PromocionesRepository
)
