package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.CardDao
import com.taskgoapp.taskgo.data.mapper.CardMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.CardMapper.toModel
import com.taskgoapp.taskgo.domain.repository.CardRepository
import com.taskgoapp.taskgo.core.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val syncManager: com.taskgoapp.taskgo.core.sync.SyncManager,
    private val authRepository: FirebaseAuthRepository
) : CardRepository {
    
    private val cardsCollection = firestore.collection("cards")

    override fun observeCards(): Flow<List<Card>> {
        val userId = authRepository.getCurrentUser()?.uid
        
        // CRÍTICO: Sempre filtrar por userId para garantir isolamento de dados
        return if (userId != null) {
            // Observar do Firestore filtrando por userId e sincronizar com cache local
            kotlinx.coroutines.flow.callbackFlow {
                val listenerRegistration = cardsCollection
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("CardRepository", "Erro ao observar cartões: ${error.message}", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val cardsList = mutableListOf<com.taskgoapp.taskgo.core.model.Card>()
                            
                            snapshot?.documents?.forEach { doc ->
                                val data = doc.data
                                val card = com.taskgoapp.taskgo.core.model.Card(
                                    id = doc.id,
                                    holder = data?.get("holder") as? String ?: "",
                                    numberMasked = data?.get("numberMasked") as? String ?: "",
                                    brand = data?.get("brand") as? String ?: "",
                                    expMonth = (data?.get("expMonth") as? Number)?.toInt() ?: 0,
                                    expYear = (data?.get("expYear") as? Number)?.toInt() ?: 0,
                                    type = data?.get("type") as? String ?: ""
                                )
                                cardDao.upsert(card.toEntity())
                                cardsList.add(card)
                            }
                            
                            // Enviar lista atualizada diretamente do Firestore
                            trySend(cardsList)
                        }
                    }
                
                awaitClose { listenerRegistration.remove() }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<Card>())
        }
    }

    override suspend fun getCard(id: String): Card? {
        return cardDao.getById(id)?.toModel()
    }

    override suspend fun upsertCard(card: Card) {
        val userId = authRepository.getCurrentUser()?.uid
            ?: throw IllegalStateException("Usuário não autenticado. Faça login novamente.")
        
        // 1. Salva localmente primeiro (instantâneo)
        cardDao.upsert(card.toEntity())
        
        // 2. Salva diretamente no Firestore com userId garantido
        val cardId = if (card.id.isBlank()) {
            cardsCollection.document().id
        } else {
            card.id
        }
        
        val cardData = hashMapOf<String, Any>(
            "userId" to userId, // CRÍTICO: Sempre incluir userId
            "holder" to card.holder,
            "numberMasked" to card.numberMasked,
            "brand" to card.brand,
            "expMonth" to card.expMonth,
            "expYear" to card.expYear,
            "type" to card.type,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        try {
            cardsCollection.document(cardId).set(cardData).await()
            android.util.Log.d("CardRepository", "Cartão salvo no Firestore com userId: $userId")
        } catch (e: Exception) {
            android.util.Log.e("CardRepository", "Erro ao salvar cartão no Firestore: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteCard(id: String) {
        // 1. Remove localmente primeiro (instantâneo)
        val entity = cardDao.getById(id) ?: return
        cardDao.delete(entity)
        
        // 2. Agenda sincronização com Firebase após 1 minuto
        syncManager.scheduleSync(
            syncType = "card",
            entityId = id,
            operation = "delete",
            data = emptyMap<String, Any>()
        )
    }
}


