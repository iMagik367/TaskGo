package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.UserProfileDao
import com.taskgoapp.taskgo.data.mapper.UserMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.UserMapper.toModel
import com.taskgoapp.taskgo.domain.repository.UserRepository
import com.taskgoapp.taskgo.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserRepository {

    override fun observeCurrentUser(): Flow<UserProfile?> {
        return userProfileDao.observeCurrent()
            .flowOn(Dispatchers.IO)
            .map { entity ->
                entity?.toModel()
            }
    }

    override suspend fun updateUser(user: UserProfile) {
        userProfileDao.upsert(user.toEntity())
    }

    override suspend fun updateAvatar(avatarUri: String) {
        val current = userProfileDao.observeCurrent().first()
        if (current != null) {
            val updatedEntity = current.copy(avatarUri = avatarUri)
            userProfileDao.upsert(updatedEntity)
        }
    }
}