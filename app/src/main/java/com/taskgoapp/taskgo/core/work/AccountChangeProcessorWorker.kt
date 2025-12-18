package com.taskgoapp.taskgo.core.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskgoapp.taskgo.data.repository.FirestoreAccountChangeRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@HiltWorker
class AccountChangeProcessorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountChangeRepository: FirestoreAccountChangeRepository,
    private val firestoreUserRepository: FirestoreUserRepository
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        private const val TAG = "AccountChangeProcessor"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Iniciando processamento de solicitações de mudança de conta")
            
            // Buscar todas as solicitações pendentes que devem ser processadas
            val pendingRequests = accountChangeRepository.getPendingRequestsToProcess()
            
            if (pendingRequests.isEmpty()) {
                Log.d(TAG, "Nenhuma solicitação pendente para processar")
                return@withContext Result.success()
            }
            
            Log.d(TAG, "Encontradas ${pendingRequests.size} solicitações para processar")
            
            var successCount = 0
            var failureCount = 0
            
            for (request in pendingRequests) {
                try {
                    // Buscar usuário atual
                    val user = firestoreUserRepository.getUser(request.userId)
                    if (user == null) {
                        Log.e(TAG, "Usuário não encontrado: ${request.userId}")
                        accountChangeRepository.updateRequestStatus(
                            requestId = request.id,
                            status = "REJECTED",
                            rejectionReason = "Usuário não encontrado"
                        )
                        failureCount++
                        continue
                    }
                    
                    // Mapear AccountType para role
                    val newRole = when (request.requestedAccountType) {
                        "PRESTADOR" -> "provider"
                        "VENDEDOR" -> "seller"
                        "CLIENTE" -> "client"
                        else -> {
                            Log.e(TAG, "Tipo de conta inválido: ${request.requestedAccountType}")
                            accountChangeRepository.updateRequestStatus(
                                requestId = request.id,
                                status = "REJECTED",
                                rejectionReason = "Tipo de conta inválido"
                            )
                            failureCount++
                            continue
                        }
                    }
                    
                    // Atualizar role do usuário
                    val updatedUser = user.copy(
                        role = newRole,
                        updatedAt = Date()
                    )
                    
                    val updateResult = firestoreUserRepository.updateUser(updatedUser)
                    updateResult.fold(
                        onSuccess = {
                            // Marcar solicitação como processada
                            accountChangeRepository.updateRequestStatus(
                                requestId = request.id,
                                status = "PROCESSED",
                                processedBy = "system"
                            )
                            Log.d(TAG, "Conta do usuário ${request.userId} alterada de ${user.role} para $newRole")
                            successCount++
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Erro ao atualizar usuário ${request.userId}: ${exception.message}", exception)
                            accountChangeRepository.updateRequestStatus(
                                requestId = request.id,
                                status = "REJECTED",
                                rejectionReason = "Erro ao atualizar usuário: ${exception.message}"
                            )
                            failureCount++
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar solicitação ${request.id}: ${e.message}", e)
                    accountChangeRepository.updateRequestStatus(
                        requestId = request.id,
                        status = "REJECTED",
                        rejectionReason = "Erro inesperado: ${e.message}"
                    )
                    failureCount++
                }
            }
            
            Log.d(TAG, "Processamento concluído: $successCount sucessos, $failureCount falhas")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erro no processamento de solicitações: ${e.message}", e)
            Result.retry()
        }
    }
}

