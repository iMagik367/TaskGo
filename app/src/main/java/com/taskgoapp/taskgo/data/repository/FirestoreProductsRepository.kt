package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório Firebase puro (sem cache local) para produtos.
 * - Sempre lê/escreve diretamente no Firestore.
 * - CRÍTICO: Usa coleção por localização locations/{city}_{state}/products para dados públicos.
 * - Usa subcoleção users/{uid}/products para dados privados.
 * - Filtra por active=true.
 */
@Singleton
class FirestoreProductsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository
) {
    // Coleção pública para queries (visualização de produtos por outros usuários)
    // CRÍTICO: Agora usamos coleções por localização, mas mantemos esta para compatibilidade
    private val publicProductsCollection = firestore.collection("products")
    
    // Helper para obter subcoleção do usuário
    private fun getUserProductsCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("products")

    /** 
     * Observa todos os produtos ativos (query pública)
     * CRÍTICO: Usa coleção por localização locations/{city}_{state}/products
     * @param city Cidade (opcional, se não fornecido usa coleção global)
     * @param state Estado (opcional, se não fornecido usa coleção global)
     */
    fun observeAllProducts(city: String? = null, state: String? = null): Flow<List<ProductFirestore>> = callbackFlow {
        // CRÍTICO: Se cidade e estado forem fornecidos, usar coleção por localização
        if (city != null && state != null && city.isNotBlank() && state.isNotBlank()) {
            android.util.Log.d("FirestoreProductsRepo", "🔵 Observando produtos por localização: city=$city, state=$state")
            val locationProductsCollection = LocationHelper.getLocationCollection(firestore, "products", city, state)
            
            val listenerRegistration: ListenerRegistration = locationProductsCollection
                .whereEqualTo("active", true)
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreProductsRepo", "❌ Erro ao observar produtos por localização: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val products = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    
                    android.util.Log.d("FirestoreProductsRepo", "📦 ${products.size} produtos encontrados na localização $city, $state")
                    trySend(products)
                }
            
            awaitClose { listenerRegistration.remove() }
        } else {
            // Fallback: usar coleção global se localização não fornecida
            android.util.Log.w("FirestoreProductsRepo", "⚠️ Localização não fornecida, usando coleção global (compatibilidade)")
            val listenerRegistration: ListenerRegistration = publicProductsCollection
                .whereEqualTo("active", true)
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro ao observar produtos: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val products = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    
                    trySend(products)
                }
            
            awaitClose { listenerRegistration.remove() }
        }
    }

    /** Observa produtos de um vendedor específico (subcoleção users/{uid}/products) */
    fun observeProductsBySeller(sellerId: String): Flow<List<ProductFirestore>> = callbackFlow {
        try {
            val userProductsCollection = getUserProductsCollection(sellerId)
            val listenerRegistration = userProductsCollection
                .whereEqualTo("active", true)
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro ao observar produtos do vendedor: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val products = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    
                    trySend(products)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao configurar listener de produtos do vendedor: ${e.message}", e)
            trySend(emptyList())
            close()
        }
    }

    /** Busca um produto por ID na coleção pública */
    suspend fun getProduct(productId: String): ProductFirestore? {
        return try {
            val document = publicProductsCollection.document(productId).get().await()
            if (document.exists()) {
                document.toObject(ProductFirestore::class.java)?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao buscar produto: ${e.message}", e)
            null
        }
    }

    /** Cria um novo produto em subcoleção do usuário e na coleção pública */
    suspend fun createProduct(product: ProductFirestore): Result<String> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            if (product.sellerId != currentUserId) {
                return Result.failure(Exception("sellerId não corresponde ao usuário atual"))
            }
            
            val userProductsCollection = getUserProductsCollection(product.sellerId)
            val docRef = userProductsCollection.add(product).await()
            val productId = docRef.id
            
            try {
                publicProductsCollection.document(productId).set(product).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreProductsRepo", "Erro ao salvar na coleção pública: ${e.message}")
            }
            
            Result.success(productId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao criar produto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Atualiza produto em subcoleção e na coleção pública */
    suspend fun updateProduct(productId: String, product: ProductFirestore): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            if (product.sellerId != currentUserId) {
                return Result.failure(Exception("Não é possível atualizar produto de outro usuário"))
            }
            
            val userProductsCollection = getUserProductsCollection(product.sellerId)
            userProductsCollection.document(productId).set(product).await()
            
            try {
                publicProductsCollection.document(productId).set(product).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreProductsRepo", "Erro ao atualizar na coleção pública: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao atualizar produto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Soft delete: marca active=false em subcoleção e pública */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            val product = getProduct(productId)
            if (product == null) {
                return Result.failure(Exception("Produto não encontrado"))
            }
            if (product.sellerId != currentUserId) {
                return Result.failure(Exception("Não é possível deletar produto de outro usuário"))
            }
            
            val userProductsCollection = getUserProductsCollection(product.sellerId)
            userProductsCollection.document(productId).update(
                "active", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            try {
                publicProductsCollection.document(productId).update(
                    "active", false,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreProductsRepo", "Erro ao atualizar na coleção pública: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao deletar produto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Busca produtos por query de texto, somente ativos */
    suspend fun searchProducts(query: String): Flow<List<ProductFirestore>> = callbackFlow {
        val listenerRegistration = publicProductsCollection
            .whereEqualTo("active", true)
            .orderBy("title")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreProductsRepo", "Erro ao buscar produtos: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
}

