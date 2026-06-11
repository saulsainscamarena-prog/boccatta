package com.bocatta.pos.data.repository

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.Tenant
import com.bocatta.pos.domain.model.TenantUser
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await

class TenantOnboardingRepository {
    private val db = FirebaseFirestoreProvider.db

    suspend fun createTenant(tenant: Tenant): Result<String> {
        return try {
            val docRef = if (tenant.id.isEmpty()) {
                db.collection("tenants").document()
            } else {
                db.collection("tenants").document(tenant.id)
            }
            
            val newTenant = tenant.copy(id = docRef.id)
            docRef.set(newTenant).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTenantUser(tenantUser: TenantUser): Result<String> {
        return try {
            val docRef = if (tenantUser.uid.isEmpty()) {
                db.collection("tenant_users").document()
            } else {
                db.collection("tenant_users").document(tenantUser.uid)
            }
            
            val newUser = tenantUser.copy(uid = docRef.id)
            docRef.set(newUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
