package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.CardDao
import com.taskgoapp.taskgo.data.mapper.CardMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.CardMapper.toModel
import com.taskgoapp.taskgo.domain.repository.CardRepository
import com.taskgoapp.taskgo.core.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val cardDao: CardDao
) : CardRepository {

    override fun observeCards(): Flow<List<Card>> {
        return cardDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getCard(id: String): Card? {
        return cardDao.getById(id)?.toModel()
    }

    override suspend fun upsertCard(card: Card) {
        cardDao.upsert(card.toEntity())
    }

    override suspend fun deleteCard(id: String) {
        val entity = cardDao.getById(id) ?: return
        cardDao.delete(entity)
    }
}


