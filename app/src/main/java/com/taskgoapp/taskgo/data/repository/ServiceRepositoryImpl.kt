package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.ServiceOrderDao
import com.taskgoapp.taskgo.data.local.dao.ProposalDao
import com.taskgoapp.taskgo.data.mapper.ServiceMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.ServiceMapper.toModel
import com.taskgoapp.taskgo.domain.repository.ServiceRepository
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.model.Proposal
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.onSuccess
import com.taskgoapp.taskgo.core.model.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val serviceOrderDao: ServiceOrderDao,
    private val proposalDao: ProposalDao,
    private val functionsService: FirebaseFunctionsService,
    private val orderRepository: FirestoreOrderRepository
) : ServiceRepository {

    override fun observeServiceOrders(): Flow<List<ServiceOrder>> {
        return serviceOrderDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getServiceOrder(id: String): ServiceOrder? {
        return serviceOrderDao.getById(id)?.toModel()
    }

    override suspend fun upsertServiceOrder(order: ServiceOrder) {
        serviceOrderDao.upsert(order.toEntity())
    }

    override suspend fun deleteServiceOrder(id: String) {
        val entity = serviceOrderDao.getById(id) ?: return
        serviceOrderDao.delete(entity)
    }

    override fun observeProposals(orderId: String): Flow<List<Proposal>> {
        return proposalDao.observeByOrderId(orderId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getProposal(id: String): Proposal? {
        return proposalDao.getById(id)?.toModel()
    }

    override suspend fun upsertProposal(proposal: Proposal) {
        proposalDao.upsert(proposal.toEntity())
    }

    override suspend fun acceptProposal(proposalId: String) {
        // 1. Atualizar no cache local (otimista)
        proposalDao.updateAccepted(proposalId, true)
        
        // 2. Buscar proposta para obter orderId
        val proposal = proposalDao.getById(proposalId)?.toModel()
        val orderId = proposal?.orderId
        
        if (orderId != null) {
            // 3. Atualizar no Firestore via Cloud Function
            try {
                val result = functionsService.updateOrderStatus(
                    orderId = orderId,
                    status = "accepted",
                    proposalDetails = null
                )
                result.onSuccess { _: Map<String, Any> ->
                    android.util.Log.d("ServiceRepository", "Proposta aceita com sucesso: $proposalId")
                }.onFailure { exception: Throwable ->
                    android.util.Log.e("ServiceRepository", "Erro ao aceitar proposta: ${exception.message}", exception)
                    // Reverter mudança local em caso de erro
                    kotlinx.coroutines.runBlocking {
                        proposalDao.updateAccepted(proposalId, false)
                    }
                    throw exception
                }
            } catch (e: Exception) {
                android.util.Log.e("ServiceRepository", "Erro ao aceitar proposta: ${e.message}", e)
                // Reverter mudança local
                kotlinx.coroutines.runBlocking {
                    proposalDao.updateAccepted(proposalId, false)
                }
                throw e
            }
        }
    }
    
    /**
     * Rejeita uma proposta
     */
    override suspend fun rejectProposal(proposalId: String) {
        // 1. Buscar proposta para obter orderId
        val proposal = proposalDao.getById(proposalId)?.toModel()
        val orderId = proposal?.orderId
        
        if (orderId != null) {
            // 2. Atualizar status da ordem para cancelled via Cloud Function
            try {
                val result = functionsService.updateOrderStatus(
                    orderId = orderId,
                    status = "cancelled",
                    proposalDetails = null
                )
                result.onSuccess { _: Map<String, Any> ->
                    android.util.Log.d("ServiceRepository", "Proposta rejeitada com sucesso: $proposalId")
                }.onFailure { exception: Throwable ->
                    android.util.Log.e("ServiceRepository", "Erro ao rejeitar proposta: ${exception.message}", exception)
                    throw exception
                }
            } catch (e: Exception) {
                android.util.Log.e("ServiceRepository", "Erro ao rejeitar proposta: ${e.message}", e)
                throw e
            }
        }
    }
}