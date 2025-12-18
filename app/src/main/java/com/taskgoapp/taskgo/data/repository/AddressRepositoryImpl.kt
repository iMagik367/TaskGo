package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.AddressDao
import com.taskgoapp.taskgo.data.mapper.AddressMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.AddressMapper.toModel
import com.taskgoapp.taskgo.domain.repository.AddressRepository
import com.taskgoapp.taskgo.core.model.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepositoryImpl @Inject constructor(
    private val addressDao: AddressDao,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val syncManager: com.taskgoapp.taskgo.core.sync.SyncManager,
    private val authRepository: FirebaseAuthRepository
) : AddressRepository {
    
    private val addressesCollection = firestore.collection("addresses")

    override fun observeAddresses(): Flow<List<Address>> {
        val userId = authRepository.getCurrentUser()?.uid
        
        // CRÍTICO: Sempre filtrar por userId para garantir isolamento de dados
        return if (userId != null) {
            // Observar do Firestore filtrando por userId e sincronizar com cache local
            kotlinx.coroutines.flow.callbackFlow {
                val listenerRegistration = addressesCollection
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("AddressRepository", "Erro ao observar endereços: ${error.message}", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val addressesList = mutableListOf<com.taskgoapp.taskgo.core.model.Address>()
                            
                            snapshot?.documents?.forEach { doc ->
                                val data = doc.data
                                val address = com.taskgoapp.taskgo.core.model.Address(
                                    id = doc.id,
                                    name = data?.get("name") as? String ?: "",
                                    phone = data?.get("phone") as? String ?: "",
                                    cep = data?.get("cep") as? String ?: "",
                                    street = data?.get("street") as? String ?: "",
                                    district = data?.get("district") as? String ?: "",
                                    city = data?.get("city") as? String ?: "",
                                    state = data?.get("state") as? String ?: "",
                                    number = data?.get("number") as? String ?: "",
                                    complement = data?.get("complement") as? String,
                                    neighborhood = data?.get("neighborhood") as? String ?: "",
                                    zipCode = data?.get("zipCode") as? String ?: ""
                                )
                                addressDao.upsert(address.toEntity())
                                addressesList.add(address)
                            }
                            
                            // Enviar lista atualizada diretamente do Firestore
                            trySend(addressesList)
                        }
                    }
                
                awaitClose { listenerRegistration.remove() }
            }
        } else {
            // Se não há usuário autenticado, retornar lista vazia
            kotlinx.coroutines.flow.flowOf(emptyList<Address>())
        }
    }

    override suspend fun getAddress(id: String): Address? {
        return addressDao.getById(id)?.toModel()
    }

    override suspend fun upsertAddress(address: Address) {
        val userId = authRepository.getCurrentUser()?.uid
            ?: throw IllegalStateException("Usuário não autenticado. Faça login novamente.")
        
        // 1. Salva localmente primeiro (instantâneo)
        addressDao.upsert(address.toEntity())
        
        // 2. Salva diretamente no Firestore com userId garantido
        val addressId = if (address.id.isBlank()) {
            // Gerar novo ID se não tiver
            addressesCollection.document().id
        } else {
            address.id
        }
        
        val addressData = hashMapOf<String, Any>(
            "userId" to userId, // CRÍTICO: Sempre incluir userId
            "name" to (address.name.takeIf { it.isNotBlank() } ?: ""),
            "phone" to (address.phone.takeIf { it.isNotBlank() } ?: ""),
            "cep" to (address.cep.takeIf { it.isNotBlank() } ?: ""),
            "street" to (address.street.takeIf { it.isNotBlank() } ?: ""),
            "district" to (address.district.takeIf { it.isNotBlank() } ?: ""),
            "city" to (address.city.takeIf { it.isNotBlank() } ?: ""),
            "state" to (address.state.takeIf { it.isNotBlank() } ?: ""),
            "number" to (address.number.takeIf { it.isNotBlank() } ?: ""),
            "neighborhood" to (address.neighborhood.takeIf { it.isNotBlank() } ?: ""),
            "zipCode" to (address.zipCode.takeIf { it.isNotBlank() } ?: ""),
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        // Adicionar complemento se existir
        address.complement?.takeIf { it.isNotBlank() }?.let {
            addressData["complement"] = it
        }
        
        try {
            addressesCollection.document(addressId).set(addressData).await()
            android.util.Log.d("AddressRepository", "Endereço salvo no Firestore com userId: $userId")
        } catch (e: Exception) {
            android.util.Log.e("AddressRepository", "Erro ao salvar endereço no Firestore: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteAddress(id: String) {
        // 1. Remove localmente primeiro (instantâneo)
        val entity = addressDao.getById(id) ?: return
        addressDao.delete(entity)
        
        // 2. Agenda sincronização com Firebase após 1 minuto
        syncManager.scheduleSync(
            syncType = "address",
            entityId = id,
            operation = "delete",
            data = emptyMap<String, Any>()
        )
    }
}