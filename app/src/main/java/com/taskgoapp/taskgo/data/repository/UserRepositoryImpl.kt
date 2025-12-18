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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val syncManager: com.taskgoapp.taskgo.core.sync.SyncManager,
    private val authRepository: FirebaseAuthRepository
) : UserRepository {
    
    // Scope para observação do Firestore em background
    private val firestoreObserverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeCurrentUser(): Flow<UserProfile?> {
        return userProfileDao.observeCurrent()
            .flowOn(Dispatchers.IO)
            .map { entity ->
                entity?.toModel()
            }
            .onStart {
                // Quando o Flow é coletado, iniciar observação do Firestore em background
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    firestoreObserverScope.launch {
                        try {
                            firestoreUserRepository.observeUser(currentUser.uid).collect { firestoreUser ->
                                firestoreUser?.let { user ->
                                    // Converter UserFirestore para UserProfile usando o método de extensão
                                    val userProfile = with(com.taskgoapp.taskgo.data.mapper.UserMapper) { user.toModel() }
                                    // Salvar no Room
                                    userProfileDao.upsert(userProfile.toEntity())
                                    android.util.Log.d("UserRepositoryImpl", "AccountType atualizado no Room: ${userProfile.accountType}, role do Firestore: ${user.role}")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("UserRepositoryImpl", "Erro ao observar usuário do Firestore: ${e.message}", e)
                        }
                    }
                }
            }
    }

    override suspend fun updateUser(user: UserProfile) {
        // 1. Salva localmente primeiro (instantâneo)
        userProfileDao.upsert(user.toEntity())
        
        // 2. Salvar imediatamente no Firestore para garantir sincronização instantânea
        // (especialmente importante para AccountType)
        try {
            val existingUser = firestoreUserRepository.getUser(user.id)
            val role = when (user.accountType) {
                com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> "provider"
                com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR -> "seller"
                com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> "client"
            }
            
            val userFirestore = existingUser?.copy(
                displayName = user.name,
                email = user.email,
                phone = user.phone,
                role = role,
                photoURL = user.avatarUri,
                updatedAt = java.util.Date()
            ) ?: com.taskgoapp.taskgo.data.firestore.models.UserFirestore(
                uid = user.id,
                email = user.email,
                displayName = user.name,
                phone = user.phone,
                role = role,
                photoURL = user.avatarUri,
                profileComplete = true,
                verified = false,
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )
            
            firestoreUserRepository.updateUser(userFirestore)
        } catch (e: Exception) {
            // Se falhar, agendar sync para depois
            val userData = mapOf(
                "uid" to user.id,
                "displayName" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "city" to (user.city ?: ""),
                "profession" to (user.profession ?: ""),
                "role" to when (user.accountType) {
                    com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> "provider"
                    com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR -> "seller"
                    com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> "client"
                },
                "photoURL" to (user.avatarUri ?: ""),
                "rating" to (user.rating ?: 0.0)
            )
            
            syncManager.scheduleSync(
                syncType = "user_profile",
                entityId = user.id,
                operation = "update",
                data = userData
            )
        }
    }

    override suspend fun updateAvatar(avatarUri: String) {
        val current = userProfileDao.observeCurrent().first()
        if (current != null) {
            // 1. Atualiza localmente primeiro (instantâneo)
            val updatedEntity = current.copy(avatarUri = avatarUri)
            userProfileDao.upsert(updatedEntity)
            
            // 2. Agenda sincronização com Firebase após 1 minuto
            val updateData = mapOf(
                "photoURL" to avatarUri
            )
            
            syncManager.scheduleSync(
                syncType = "user_profile",
                entityId = current.id,
                operation = "update",
                data = updateData
            )
        }
    }
}