package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProposalUseCase @Inject constructor(
    private val functionsService: FirebaseFunctionsService,
    private val orderRepository: FirestoreOrderRepository
) {
    /**
     * Aceita uma proposta de serviço
     * Atualiza o status da ordem para "accepted"
     */
    suspend fun acceptProposal(orderId: String): Result<Unit> {
        return try {
            val result = functionsService.updateOrderStatus(
                orderId = orderId,
                status = "accepted",
                proposalDetails = null
            )
            result.map { _: Map<String, Any> -> Unit }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Rejeita uma proposta de serviço
     * Atualiza o status da ordem para "cancelled"
     */
    suspend fun rejectProposal(orderId: String): Result<Unit> {
        return try {
            val result = functionsService.updateOrderStatus(
                orderId = orderId,
                status = "cancelled",
                proposalDetails = null
            )
            result.map { _: Map<String, Any> -> Unit }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

