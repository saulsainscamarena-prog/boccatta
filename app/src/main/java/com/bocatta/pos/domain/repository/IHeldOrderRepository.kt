package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.HeldOrder

interface IHeldOrderRepository {
    fun save(order: HeldOrder)
    fun getAll(): List<HeldOrder>
    fun getById(id: String): HeldOrder?
    fun delete(id: String)
    fun clear()
}

