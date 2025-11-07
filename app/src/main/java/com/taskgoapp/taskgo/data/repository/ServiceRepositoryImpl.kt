package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.ServiceOrderDao
import com.taskgoapp.taskgo.data.local.dao.ProposalDao
import com.taskgoapp.taskgo.data.mapper.ServiceMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.ServiceMapper.toModel
import com.taskgoapp.taskgo.domain.repository.ServiceRepository
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.model.Proposal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val serviceOrderDao: ServiceOrderDao,
    private val proposalDao: ProposalDao
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
        proposalDao.updateAccepted(proposalId, true)
    }
}