package com.bocatta.pos.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.HeldOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeldOrderRepositoryInstrumentedTest {

    private val repository: HeldOrderRepository
        get() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            return HeldOrderRepository(OfflineDatabase.getInstance(context))
        }

    @Test
    fun assignToMesa_preservesOrderIdAndUpdatesMesaAtomically() {
        val id = "held_move_${System.nanoTime()}"
        val original = HeldOrder(
            id = id,
            carritoJson = "[]",
            clienteJson = null,
            nota = "test",
            fecha = System.currentTimeMillis(),
            sucursal = "test",
            total = 120.0,
            modalidad = "PARA_LLEVAR",
            mesaId = null
        )
        repository.save(original)

        val updated = repository.assignToMesa(id, "mesa_7")

        assertNotNull(updated)
        assertEquals(id, updated?.id)
        assertEquals("LOCAL", updated?.modalidad)
        assertEquals("mesa_7", updated?.mesaId)

        val persisted = repository.getById(id)
        assertEquals(id, persisted?.id)
        assertEquals("mesa_7", persisted?.mesaId)
        assertEquals("LOCAL", persisted?.modalidad)
    }

    @Test
    fun assignToMesa_missingOrderDoesNotCreateReplacement() {
        val missingId = "missing_${System.nanoTime()}"

        val updated = repository.assignToMesa(missingId, "mesa_9")

        assertNull(updated)
        assertNull(repository.getById(missingId))
    }

    @Test
    fun getById_doesNotDeleteHeldOrder() {
        val id = "held_get_${System.nanoTime()}"
        repository.save(
            HeldOrder(
                id = id,
                carritoJson = "[]",
                fecha = System.currentTimeMillis(),
                sucursal = "test",
                total = 80.0,
                modalidad = "PARA_LLEVAR"
            )
        )

        val firstRead = repository.getById(id)
        val secondRead = repository.getById(id)

        assertNotNull(firstRead)
        assertNotNull(secondRead)
        assertEquals(id, secondRead?.id)
    }

    @Test
    fun delete_removesHeldOrderOnlyWhenExplicitlyCalled() {
        val id = "held_delete_${System.nanoTime()}"
        repository.save(
            HeldOrder(
                id = id,
                carritoJson = "[]",
                fecha = System.currentTimeMillis(),
                sucursal = "test",
                total = 50.0,
                modalidad = "LOCAL",
                mesaId = "mesa_3"
            )
        )
        assertNotNull(repository.getById(id))

        repository.delete(id)

        assertNull(repository.getById(id))
    }
}
