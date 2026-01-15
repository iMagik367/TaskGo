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
import kotlinx.coroutines.flow.distinctUntilChanged
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
        val currentUser = authRepository.getCurrentUser()
        val userId = currentUser?.uid
        
        // CRÍTICO: Se não houver usuário autenticado, retornar Flow vazio
        if (userId == null) {
            return kotlinx.coroutines.flow.flowOf(null)
        }
        
        // CRÍTICO: Limpar dados de outros usuários do banco local
        firestoreObserverScope.launch {
            try {
                userProfileDao.clearOtherUsers(userId)
            } catch (e: Exception) {
                android.util.Log.e("UserRepositoryImpl", "Erro ao limpar dados de outros usuários: ${e.message}", e)
            }
        }
        
        return userProfileDao.observeCurrent(userId)
            .flowOn(Dispatchers.IO)
            .map { entity ->
                // CRÍTICO: Verificar se o entity pertence ao usuário atual
                if (entity != null && entity.id != userId) {
                    android.util.Log.w("UserRepositoryImpl", "Entity não pertence ao usuário atual: ${entity.id} != $userId")
                    null
                } else {
                    entity?.toModel()
                }
            }
            .onStart {
                // Quando o Flow é coletado, iniciar observação do Firestore em background
                if (userId != null) {
                    firestoreObserverScope.launch {
                        try {
                            // CRÍTICO: Buscar diretamente do Firestore, não usar cache local
                            val firestoreUser = firestoreUserRepository.getUser(userId)
                            if (firestoreUser != null) {
                                val userProfile = with(com.taskgoapp.taskgo.data.mapper.UserMapper) { firestoreUser.toModel() }
                                // Verificar se o perfil pertence ao usuário atual antes de salvar
                                if (userProfile.id == userId) {
                                    userProfileDao.upsert(userProfile.toEntity())
                                    // Limpar dados de outros usuários
                                    userProfileDao.clearOtherUsers(userId)
                                } else {
                                    android.util.Log.w("UserRepositoryImpl", "Perfil do Firestore não pertence ao usuário atual: ${userProfile.id} != $userId")
                                }
                            }
                            
                            // Observar mudanças do Firestore
                            firestoreUserRepository.observeUser(userId)
                                .distinctUntilChanged { old, new ->
                                    // Comparar apenas campos importantes para evitar logs repetidos
                                    old?.uid == new?.uid && old?.role == new?.role
                                }
                                .collect { firestoreUser ->
                                    firestoreUser?.let { user ->
                                        // CRÍTICO: Verificar se o usuário do Firestore pertence ao usuário atual
                                        if (user.uid == userId) {
                                            // Converter UserFirestore para UserProfile usando o método de extensão
                                            val userProfile = with(com.taskgoapp.taskgo.data.mapper.UserMapper) { user.toModel() }
                                            // Salvar no Room apenas se pertencer ao usuário atual
                                            if (userProfile.id == userId) {
                                                userProfileDao.upsert(userProfile.toEntity())
                                                // Limpar dados de outros usuários
                                                userProfileDao.clearOtherUsers(userId)
                                            }
                                        } else {
                                            android.util.Log.w("UserRepositoryImpl", "Usuário do Firestore não pertence ao usuário atual: ${user.uid} != $userId")
                                        }
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
        val currentUser = authRepository.getCurrentUser()
        val userId = currentUser?.uid
        
        // CRÍTICO: Verificar se o usuário pertence ao usuário autenticado
        if (userId == null || user.id != userId) {
            android.util.Log.w("UserRepositoryImpl", "Tentativa de atualizar perfil de outro usuário: ${user.id} != $userId")
            return
        }
        
        // CRÍTICO: Salvar diretamente no Firestore, não usar cache local
        try {
            val existingUser = firestoreUserRepository.getUser(user.id)
            val role = when (user.accountType) {
                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "partner" // Novo role unificado
                com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> "partner" // Legacy - migrar para partner
                com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR -> "partner" // Legacy - migrar para partner
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
            
            // CRÍTICO: Atualizar banco local apenas após sucesso no Firestore
            // E apenas se o usuário pertencer ao usuário atual
            if (user.id == userId) {
                userProfileDao.upsert(user.toEntity())
                // Limpar dados de outros usuários
                userProfileDao.clearOtherUsers(userId)
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepositoryImpl", "Erro ao atualizar usuário no Firestore: ${e.message}", e)
            // Se falhar, agendar sync para depois
            val userData = mapOf(
                "uid" to user.id,
                "displayName" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "city" to (user.city ?: ""),
                "profession" to (user.profession ?: ""),
                "role" to when (user.accountType) {
                    com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "partner" // Novo role unificado
                    com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> "partner" // Legacy - migrar para partner
                    com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR -> "partner" // Legacy - migrar para partner
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
        val currentUser = authRepository.getCurrentUser()
        val userId = currentUser?.uid
        if (userId == null) {
            android.util.Log.w("UserRepositoryImpl", "Usuário não autenticado ao atualizar avatar")
            return
        }
        
        val current = userProfileDao.getCurrent(userId)
        if (current != null && current.id == userId) {
            // CRÍTICO: Atualizar diretamente no Firestore, não usar cache local
            try {
                val existingUser = firestoreUserRepository.getUser(userId)
                if (existingUser != null) {
                    val updatedUser = existingUser.copy(photoURL = avatarUri, updatedAt = java.util.Date())
                    firestoreUserRepository.updateUser(updatedUser)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepositoryImpl", "Erro ao atualizar avatar no Firestore: ${e.message}", e)
                // Se falhar, agendar sync
                val updateData = mapOf(
                    "photoURL" to avatarUri
                )
                syncManager.scheduleSync(
                    syncType = "user_profile",
                    entityId = userId,
                    operation = "update",
                    data = updateData
                )
            }
        }
    }
}