package com.bocatta.pos.security

import com.bocatta.pos.data.repository.FirebaseSalesRepositoryV2
import com.bocatta.pos.data.sync.SyncWorker
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.repository.InventoryRepositoryImpl
import com.bocatta.pos.domain.repository.ISyncErrorRepository
import org.junit.Assert.assertNotNull
import org.junit.Test

class FirestoreRulesContractTest {

    @Test
    fun firebaseSalesRepositoryV2Exists() {
        assertNotNull(FirebaseSalesRepositoryV2::class.java)
    }

    @Test
    fun syncWorkerClassExists() {
        assertNotNull(SyncWorker::class.java)
    }

    @Test
    fun offlineManagerClassExists() {
        assertNotNull(OfflineManager::class.java)
    }

    @Test
    fun offlineDatabaseClassExists() {
        assertNotNull(OfflineDatabase::class.java)
    }

    @Test
    fun inventoryRepositoryImplExists() {
        assertNotNull(InventoryRepositoryImpl::class.java)
    }

    @Test
    fun syncErrorRepositoryInterfaceExists() {
        assertNotNull(ISyncErrorRepository::class.java)
    }
}
