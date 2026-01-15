package com.taskgoapp.taskgo.core.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.local.dao.*
import com.taskgoapp.taskgo.data.repository.*
import com.taskgoapp.taskgo.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de sincronização inicial de dados
 * 
 * No primeiro acesso do usuário após login, baixa todos os dados do Firebase
 * para o cache local para melhorar performance e permitir uso offline
 */
@Singleton
class InitialDataSyncManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val productsRepository: ProductsRepository,
    private val ordersRepository: OrdersRepository,
    private val addressRepository: AddressRepository,
    private val cardRepository: CardRepository,
    private val userRepository: UserRepository,
    private val firestoreUserRepository: com.taskgoapp.taskgo.data.repository.FirestoreUserRepository,
    private val productDao: ProductDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val addressDao: AddressDao,
    private val cardDao: CardDao,
    private val userProfileDao: UserProfileDao
) {
    
    private val TAG = "InitialDataSyncManager"
    
    /**
     * Sincroniza todos os dados do usuário do Firebase para o cache local
     * 
     * @return true se a sincronização foi bem-sucedida, false caso contrário
     */
    suspend fun syncAllUserData(): Boolean = withContext(Dispatchers.IO) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Usuário não autenticado, não é possível sincronizar dados")
            return@withContext false
        }
        
        Log.d(TAG, "=== Iniciando sincronização inicial de dados para usuário: $userId ===")
        
        try {
            // Executar todas as sincronizações em paralelo para melhor performance
            val results = awaitAll(
                async { syncUserProfile(userId) },
                async { syncProducts(userId) },
                async { syncOrders(userId) },
                async { syncAddresses(userId) },
                async { syncCards(userId) }
            )
            
            val allSuccess = results.all { it }
            
            if (allSuccess) {
                Log.d(TAG, "=== Sincronização inicial concluída com sucesso ===")
            } else {
                Log.w(TAG, "=== Sincronização inicial concluída com alguns erros ===")
            }
            
            allSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante sincronização inicial: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincroniza o perfil do usuário
     */
    private suspend fun syncUserProfile(userId: String): Boolean {
        return try {
            Log.d(TAG, "Sincronizando perfil do usuário...")
            
            // Carregar do Firestore e salvar no banco local
            val userFirestore = firestoreUserRepository.getUser(userId)
            
            if (userFirestore != null) {
                val accountType = when (userFirestore.role?.lowercase()) {
                    "partner" -> com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO
                    "provider" -> com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO // Legacy - migrar para PARCEIRO
                    "seller" -> com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO // Legacy - migrar para PARCEIRO
                    "client" -> com.taskgoapp.taskgo.core.model.AccountType.CLIENTE
                    else -> com.taskgoapp.taskgo.core.model.AccountType.CLIENTE
                }
                
                val user = com.taskgoapp.taskgo.core.model.UserProfile(
                    id = userId,
                    name = userFirestore.displayName ?: "",
                    email = userFirestore.email,
                    phone = userFirestore.phone,
                    city = userFirestore.address?.city,
                    profession = null,
                    accountType = accountType,
                    rating = userFirestore.rating,
                    avatarUri = userFirestore.photoURL,
                    profileImages = null
                )
                
                // Salvar no banco local
                userRepository.updateUser(user)
                Log.d(TAG, "Perfil do usuário sincronizado: ${user.name}, accountType: ${user.accountType}")
            } else {
                Log.d(TAG, "Perfil do usuário não encontrado no Firestore")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar perfil: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincroniza produtos do usuário (se for vendedor)
     */
    private suspend fun syncProducts(userId: String): Boolean {
        return try {
            Log.d(TAG, "Sincronizando produtos do usuário...")
            // Dispara busca que já atualiza o cache no repositório
            productsRepository.getMyProducts()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar produtos: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincroniza pedidos do usuário
     */
    private suspend fun syncOrders(userId: String): Boolean {
        return try {
            Log.d(TAG, "Sincronizando pedidos do usuário...")
            // Dispara observação que puxa do Firestore e atualiza o cache
            ordersRepository.observeOrders().first()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar pedidos: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincroniza endereços do usuário
     */
    private suspend fun syncAddresses(userId: String): Boolean {
        return try {
            Log.d(TAG, "Sincronizando endereços do usuário...")
            addressRepository.observeAddresses().first()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar endereços: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sincroniza cartões do usuário
     */
    private suspend fun syncCards(userId: String): Boolean {
        return try {
            Log.d(TAG, "Sincronizando cartões do usuário...")
            cardRepository.observeCards().first()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar cartões: ${e.message}", e)
            false
        }
    }
}

