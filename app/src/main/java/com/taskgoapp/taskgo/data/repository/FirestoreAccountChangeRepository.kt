package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.AccountChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAccountChangeRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val accountChangeRequestsCollection = firestore.collection("account_change_requests")
    
    /**
     * Cria uma solicitação de mudança de modo de conta
     * Calcula automaticamente a data de processamento (1 dia útil)
     */
    suspend fun createAccountChangeRequest(
        userId: String,
        currentAccountType: String,
        requestedAccountType: String
    ): Result<String> {
        return try {
            // Calcular data de processamento (1 dia útil)
            val scheduledDate = calculateNextBusinessDay(Date())
            
            val request = AccountChangeRequest(
                id = "", // Será gerado pelo Firestore
                userId = userId,
                currentAccountType = currentAccountType,
                requestedAccountType = requestedAccountType,
                status = "PENDING",
                requestedAt = Date(),
                scheduledProcessDate = scheduledDate,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val docRef = accountChangeRequestsCollection.add(request).await()
            
            android.util.Log.d("AccountChangeRepo", "Solicitação de mudança criada: ${docRef.id} para usuário $userId")
            Result.success(docRef.id)
        } catch (e: Exception) {
            android.util.Log.e("AccountChangeRepo", "Erro ao criar solicitação: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Busca solicitações pendentes de um usuário
     */
    suspend fun getPendingRequest(userId: String): AccountChangeRequest? {
        return try {
            val snapshot = accountChangeRequestsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "PENDING")
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(AccountChangeRequest::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("AccountChangeRepo", "Erro ao buscar solicitação pendente: ${e.message}", e)
            null
        }
    }
    
    /**
     * Observa solicitações de mudança de conta de um usuário
     */
    fun observeUserRequests(userId: String): Flow<List<AccountChangeRequest>> = callbackFlow {
        try {
            val listenerRegistration = accountChangeRequestsCollection
                .whereEqualTo("userId", userId)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("AccountChangeRepo", "Erro ao observar solicitações: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val requests = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(AccountChangeRequest::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("AccountChangeRepo", "Erro ao converter documento: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(requests)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("AccountChangeRepo", "Erro ao observar solicitações: ${e.message}", e)
            close(e)
        }
    }
    
    /**
     * Atualiza o status de uma solicitação
     */
    suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        processedBy: String? = null,
        rejectionReason: String? = null
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            if (status == "PROCESSED") {
                updateData["processedAt"] = FieldValue.serverTimestamp()
                processedBy?.let { updateData["processedBy"] = it }
            }
            
            if (status == "REJECTED" && rejectionReason != null) {
                updateData["rejectionReason"] = rejectionReason
            }
            
            accountChangeRequestsCollection.document(requestId).update(updateData).await()
            
            android.util.Log.d("AccountChangeRepo", "Status da solicitação $requestId atualizado para $status")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AccountChangeRepo", "Erro ao atualizar status: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Busca todas as solicitações pendentes que devem ser processadas
     * (scheduledProcessDate <= hoje e status = PENDING)
     */
    suspend fun getPendingRequestsToProcess(): List<AccountChangeRequest> {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val snapshot = accountChangeRequestsCollection
                .whereEqualTo("status", "PENDING")
                .whereLessThanOrEqualTo("scheduledProcessDate", today)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(AccountChangeRequest::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("AccountChangeRepo", "Erro ao converter documento: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AccountChangeRepo", "Erro ao buscar solicitações pendentes: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Calcula o próximo dia útil (1 dia útil a partir de hoje)
     * Ignora sábados e domingos
     */
    private fun calculateNextBusinessDay(startDate: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = startDate
            add(Calendar.DAY_OF_MONTH, 1) // Começar do dia seguinte
        }
        
        // Avançar até encontrar um dia útil (segunda a sexta)
        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
               calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Zerar horas para comparar apenas a data
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.time
    }
}

