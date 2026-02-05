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
                                .collect { firestoreUser ->
                                    firestoreUser?.let { user ->
                                        // CRÍTICO: Verificar se o usuário do Firestore pertence ao usuário atual
                                        if (user.uid == userId) {
                                            android.util.Log.d("UserRepositoryImpl", "🔄 Usuário atualizado no Firestore: role=${user.role}, pendingAccountType=${user.pendingAccountType}")
                                            
                                            // Converter UserFirestore para UserProfile usando o método de extensão
                                            val userProfile = with(com.taskgoapp.taskgo.data.mapper.UserMapper) { user.toModel() }
                                            
                                            // CRÍTICO: Verificar se o role mudou antes de atualizar
                                            val existingProfile = userProfileDao.getCurrent(userId)
                                            // Converter accountType do Entity (String) para AccountType (enum) para comparação
                                            val existingAccountType = existingProfile?.let {
                                                // Mapear String para AccountType de forma segura (suporta valores legacy)
                                                when (it.accountType.uppercase()) {
                                                    "PRESTADOR" -> com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO // Legacy
                                                    "VENDEDOR" -> com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO // Legacy
                                                    "PARCEIRO" -> com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO
                                                    "CLIENTE" -> com.taskgoapp.taskgo.core.model.AccountType.CLIENTE
                                                    else -> com.taskgoapp.taskgo.core.model.AccountType.CLIENTE // Default seguro
                                                }
                                            }
                                            val roleChanged = existingAccountType != userProfile.accountType
                                            
                                            if (roleChanged) {
                                                android.util.Log.d("UserRepositoryImpl", "🔵 Role mudou: ${existingProfile?.accountType} -> ${userProfile.accountType}")
                                            }
                                            
                                            // Salvar no Room apenas se pertencer ao usuário atual
                                            if (userProfile.id == userId) {
                                                userProfileDao.upsert(userProfile.toEntity())
                                                // Limpar dados de outros usuários
                                                userProfileDao.clearOtherUsers(userId)
                                                android.util.Log.d("UserRepositoryImpl", "✅ Perfil atualizado no banco local: role=${userProfile.accountType}")
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
                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "partner"
                com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> "client"
            }
            
            // Lei 1: city/state são salvos APENAS diretamente no documento (user.city, user.state)
            // NÃO salvar em address - isso causa inconsistência
            // Backend lê APENAS de user.city/user.state - NÃO há fallback para address
            val address = existingUser?.address?.copy(
                // Manter outros campos do address, mas city/state serão lidos da raiz
                street = existingUser.address?.street ?: "",
                number = existingUser.address?.number ?: "",
                complement = existingUser.address?.complement,
                neighborhood = existingUser.address?.neighborhood ?: "",
                zipCode = existingUser.address?.zipCode ?: "",
                country = existingUser.address?.country ?: "Brasil"
            ) ?: if (user.city != null && user.state != null) {
                com.taskgoapp.taskgo.core.model.Address(
                    id = "",
                    name = "",
                    phone = "",
                    cep = "",
                    street = "",
                    district = "",
                    city = user.city ?: "",
                    state = user.state ?: "",
                    number = "",
                    complement = null,
                    neighborhood = "",
                    zipCode = "",
                    country = "Brasil"
                )
            } else null
            
            // Lei 1: city e state DEVEM estar na raiz do documento users/{userId}
            // ✅ REMOVIDO: LocationUpdateService não atualiza mais city/state via GPS
            // City/state vêm APENAS do cadastro do usuário no Firestore
            val userFirestore = existingUser?.copy(
                displayName = user.name,
                email = user.email,
                phone = user.phone,
                role = role,
                photoURL = user.avatarUri,
                address = address,
                city = user.city, // Lei 1: Na raiz do documento
                state = user.state, // Lei 1: Na raiz do documento
                updatedAt = java.util.Date()
            ) ?: com.taskgoapp.taskgo.data.firestore.models.UserFirestore(
                uid = user.id,
                email = user.email,
                displayName = user.name,
                phone = user.phone,
                role = role,
                photoURL = user.avatarUri,
                address = address,
                city = user.city, // Lei 1: Na raiz do documento
                state = user.state, // Lei 1: Na raiz do documento
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
                    com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "partner"
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