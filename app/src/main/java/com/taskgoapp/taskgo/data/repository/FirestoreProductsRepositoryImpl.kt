package com.taskgoapp.taskgo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.data.mapper.CartMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.CartMapper.toModel
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toFirestore
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toModel
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.core.location.LocationStateManager
import com.taskgoapp.taskgo.core.location.LocationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProductsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao,
    private val userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository,
    private val locationStateManager: LocationStateManager
) : ProductsRepository {

    // DEBUG ONLY - Coleção global mantida apenas para compatibilidade durante migração
    // REMOVER APÓS VALIDAÇÃO COMPLETA
    private val productsCollectionGlobal = firestore.collection("products")
    private val productErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeProducts(): Flow<List<Product>> = locationStateManager.locationState
        .flatMapLatest { locationState ->
            when (locationState) {
                is LocationState.Loading -> {
                    Log.w("BLOCKED_QUERY", "Firestore query blocked: location not ready (Loading)")
                    flowOf(emptyList())
                }
                is LocationState.Error -> {
                    Log.e("BLOCKED_QUERY", "Firestore query blocked: location error - ${locationState.reason}")
                    flowOf(emptyList())
                }
                is LocationState.Ready -> {
                    // ✅ Localização pronta - fazer query Firestore
                    val locationId = locationState.locationId
                    
                    // 🚨 PROTEÇÃO: Nunca permitir "unknown" como locationId válido
                    if (locationId == "unknown" || locationId.isBlank()) {
                        Log.e("FATAL_LOCATION", "Attempted Firestore query with invalid locationId: $locationId")
                        flowOf(emptyList())
                    } else {
                        observeProductsFromFirestore(locationState)
                    }
                }
            }
        }
    
    private fun observeProductsFromFirestore(locationState: LocationState.Ready): Flow<List<Product>> = callbackFlow {
        val listener: ListenerRegistration? = try {
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "products",
                locationState.city,
                locationState.state
            )
            
            Log.d("FirestoreProductsRepo", """
                📍 Querying Firestore with location:
                City: ${locationState.city}
                State: ${locationState.state}
                LocationId: ${locationState.locationId}
                Firestore Path: locations/${locationState.locationId}/products
            """.trimIndent())
            
            // Configurar listener
            // ⚠️ ETAPA 5: Ajustar filtros para evitar exclusões silenciosas
            // Usar filtro mais permissivo temporariamente para estabilizar
            collectionToUse
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirestoreProductsRepo", 
                            "❌ Erro no listener de produtos: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        Log.w("FirestoreProductsRepo", 
                            "⚠️ Snapshot vazio (sem produtos encontrados)")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    Log.d("FirestoreProductsRepo", 
                        "Snapshot recebido: size=${snapshot.size()}, collection=locations/${locationState.locationId}/products")
                    
                    // 📍 SNAPSHOT PROOF - Logar TUDO que vem do Firestore
                    Log.d("FirestoreSnapshot", """
                        📍 FRONTEND SNAPSHOT PROOF
                        Collection path: ${collectionToUse.path}
                        Snapshot empty: ${snapshot.isEmpty}
                        Snapshot size: ${snapshot.size()}
                        Documents count: ${snapshot.documents.size}
                    """.trimIndent())
                    
                    snapshot.documents.forEachIndexed { index, doc ->
                        Log.d("FirestoreSnapshot", """
                            📍 FRONTEND SNAPSHOT PROOF - Document $index
                            Doc ID: ${doc.id}
                            Doc data keys: ${doc.data?.keys?.joinToString(", ") ?: "null"}
                            Doc has createdAt: ${doc.data?.containsKey("createdAt")}
                            Doc has active: ${doc.data?.get("active")}
                            Doc has status: ${doc.data?.get("status")}
                        """.trimIndent())
                    }
                    
                    val products = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val createdAt = when (val v = data["createdAt"]) {
                                is Long -> java.util.Date(v)
                                is java.util.Date -> v
                                is com.google.firebase.Timestamp -> v.toDate()
                                else -> null
                            }
                            val updatedAt = when (val v = data["updatedAt"]) {
                                is Long -> java.util.Date(v)
                                is java.util.Date -> v
                                is com.google.firebase.Timestamp -> v.toDate()
                                else -> null
                            }
                            ProductFirestore(
                                id = doc.id,
                                title = data["title"] as? String ?: "",
                                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                                description = data["description"] as? String,
                                sellerId = data["sellerId"] as? String ?: "",
                                sellerName = data["sellerName"] as? String,
                                imageUrls = ((data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } 
                                    ?: (data["images"] as? List<*>)?.mapNotNull { it as? String } 
                                    ?: emptyList()),
                                category = data["category"] as? String,
                                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                // ⚠️ ETAPA 5: Permitir ausência de campo no parsing (evitar exclusões silenciosas)
                                active = data["active"] as? Boolean ?: true,
                                status = data["status"] as? String ?: "active",
                                featured = data["featured"] as? Boolean ?: false,
                                discountPercentage = (data["discountPercentage"] as? Number)?.toDouble(),
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                rating = (data["rating"] as? Number)?.toDouble(),
                                latitude = (data["latitude"] as? Number)?.toDouble(),
                                longitude = (data["longitude"] as? Number)?.toDouble()
                            ).toModel()
                        } catch (e: Exception) {
                            Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    
                    // ⚠️ ETAPA 5: Filtrar produtos após receber (evitar exclusões silenciosas)
                    // Filtrar apenas produtos ativos e com status "active" (produtos públicos)
                    // Permitir ausência de campo no parsing (evitar exclusões silenciosas)
                    val filteredProducts = products.filterIndexed { index, product ->
                        val doc = snapshot.documents.getOrNull(index)
                        val isActive = doc?.data?.get("active") as? Boolean ?: true
                        val status = doc?.data?.get("status") as? String ?: "active"
                        
                        isActive && status == "active"
                    }
                    
                    Log.d("FirestoreProductsRepo", 
                        "Produtos filtrados: total=${products.size}, ativos=${filteredProducts.size}")
                    
                    trySend(filteredProducts)
                }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao configurar listener: ${e.message}", e)
            trySend(emptyList())
            null
        }
        awaitClose { listener?.remove() }
    }

    override fun observeProductErrors(): Flow<String> = productErrors.asSharedFlow()

    override suspend fun getProduct(id: String): Product? {
        return try {
            // DEBUG ONLY - Tentar buscar da coleção global primeiro (compatibilidade)
            // TODO: Buscar de todas as locations se necessário, ou receber city/state como parâmetro
            val document = productsCollectionGlobal.document(id).get(Source.SERVER).await()
            val data = document.data ?: return null
            val createdAt = when (val v = data["createdAt"]) {
                is Long -> java.util.Date(v)
                is java.util.Date -> v
                is com.google.firebase.Timestamp -> v.toDate()
                else -> null
            }
            val updatedAt = when (val v = data["updatedAt"]) {
                is Long -> java.util.Date(v)
                is java.util.Date -> v
                is com.google.firebase.Timestamp -> v.toDate()
                else -> null
            }
            ProductFirestore(
                id = document.id,
                title = data["title"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                description = data["description"] as? String,
                sellerId = data["sellerId"] as? String ?: "",
                sellerName = data["sellerName"] as? String,
                imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                category = data["category"] as? String,
                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                active = data["active"] as? Boolean ?: true,
                featured = data["featured"] as? Boolean ?: false,
                discountPercentage = (data["discountPercentage"] as? Number)?.toDouble(),
                createdAt = createdAt,
                updatedAt = updatedAt,
                rating = (data["rating"] as? Number)?.toDouble(),
                latitude = (data["latitude"] as? Number)?.toDouble(),
                longitude = (data["longitude"] as? Number)?.toDouble()
            ).toModel()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao buscar produto: ${e.message}", e)
            null
        }
    }

    override suspend fun getMyProducts(): List<Product> {
        val userId = firebaseAuth.currentUser?.uid ?: return emptyList()
        return try {
            // CRÍTICO: Buscar produtos do usuário da coleção por localização
            // Primeiro, obter localização do usuário
            val user = withTimeoutOrNull(2000) {
                userRepository.observeCurrentUser().firstOrNull()
            }
            val userCity = user?.city?.takeIf { it.isNotBlank() }
            val userState = user?.state?.takeIf { it.isNotBlank() } ?: ""
            
            val productsList = mutableListOf<Product>()
            
            if (userCity != null && userState.isNotBlank()) {
                // Buscar da coleção por localização
                val locationCollection = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
                val snapshot = locationCollection
                    .whereEqualTo("sellerId", userId)
                    .whereEqualTo("active", true)
                    .get(Source.SERVER)
                    .await()
                
                snapshot.documents.forEach { doc ->
                    try {
                        val data = doc.data ?: return@forEach
                        val createdAt = when (val v = data["createdAt"]) {
                            is Long -> java.util.Date(v)
                            is java.util.Date -> v
                            is com.google.firebase.Timestamp -> v.toDate()
                            else -> null
                        }
                        val updatedAt = when (val v = data["updatedAt"]) {
                            is Long -> java.util.Date(v)
                            is java.util.Date -> v
                            is com.google.firebase.Timestamp -> v.toDate()
                            else -> null
                        }
                        val product = ProductFirestore(
                            id = doc.id,
                            title = data["title"] as? String ?: "",
                            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                            description = data["description"] as? String,
                            sellerId = data["sellerId"] as? String ?: "",
                            sellerName = data["sellerName"] as? String,
                            imageUrls = ((data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } 
                                ?: (data["images"] as? List<*>)?.mapNotNull { it as? String } 
                                ?: emptyList()),
                            category = data["category"] as? String,
                            tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            active = data["active"] as? Boolean ?: true,
                            featured = data["featured"] as? Boolean ?: false,
                            discountPercentage = (data["discountPercentage"] as? Number)?.toDouble(),
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            rating = (data["rating"] as? Number)?.toDouble(),
                            latitude = (data["latitude"] as? Number)?.toDouble(),
                            longitude = (data["longitude"] as? Number)?.toDouble()
                        ).toModel()
                        productsList.add(product)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                    }
                }
            }
            
            // Fallback: buscar da coleção global (compatibilidade)
            val globalSnapshot = productsCollectionGlobal
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("active", true)
                .get(Source.SERVER)
                .await()
            
            globalSnapshot.documents.forEach { doc ->
                // Evitar duplicatas
                if (productsList.none { it.id == doc.id }) {
                    try {
                        val data = doc.data ?: return@forEach
                        val createdAt = when (val v = data["createdAt"]) {
                            is Long -> java.util.Date(v)
                            is java.util.Date -> v
                            is com.google.firebase.Timestamp -> v.toDate()
                            else -> null
                        }
                        val updatedAt = when (val v = data["updatedAt"]) {
                            is Long -> java.util.Date(v)
                            is java.util.Date -> v
                            is com.google.firebase.Timestamp -> v.toDate()
                            else -> null
                        }
                        val product = ProductFirestore(
                            id = doc.id,
                            title = data["title"] as? String ?: "",
                            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                            description = data["description"] as? String,
                            sellerId = data["sellerId"] as? String ?: "",
                            sellerName = data["sellerName"] as? String,
                            imageUrls = ((data["imageUrls"] as? List<*>)?.mapNotNull { it as? String } 
                                ?: (data["images"] as? List<*>)?.mapNotNull { it as? String } 
                                ?: emptyList()),
                            category = data["category"] as? String,
                            tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            active = data["active"] as? Boolean ?: true,
                            featured = data["featured"] as? Boolean ?: false,
                            discountPercentage = (data["discountPercentage"] as? Number)?.toDouble(),
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            rating = (data["rating"] as? Number)?.toDouble(),
                            latitude = (data["latitude"] as? Number)?.toDouble(),
                            longitude = (data["longitude"] as? Number)?.toDouble()
                        ).toModel()
                        productsList.add(product)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                    }
                }
            }
            
            productsList
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao buscar meus produtos: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun upsertProduct(product: Product) {
        val firestoreProduct = product.toFirestore().copy(
            sellerId = firebaseAuth.currentUser?.uid ?: "",
            active = true
        )
        val operation = if (product.id.isNotEmpty()) "update" else "create"
        val entityId = product.id.ifEmpty { java.util.UUID.randomUUID().toString() }

        val existingCreatedAt = if (operation == "update") {
            try {
                // DEBUG ONLY - Buscar da coleção global (compatibilidade)
                val existingDoc = productsCollectionGlobal.document(entityId).get(Source.SERVER).await()
                val existingData = existingDoc.data
                when (val createdAtValue = existingData?.get("createdAt")) {
                    is Long -> createdAtValue
                    is java.util.Date -> createdAtValue.time
                    is com.google.firebase.Timestamp -> createdAtValue.toDate().time
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        } else null

        val productData = mutableMapOf<String, Any>(
            "id" to entityId,
            "sellerId" to firestoreProduct.sellerId,
            "title" to firestoreProduct.title,
            "description" to (firestoreProduct.description ?: ""),
            "price" to firestoreProduct.price,
            "active" to true,
            "featured" to firestoreProduct.featured,
            "discountPercentage" to (firestoreProduct.discountPercentage ?: 0.0),
            "imageUrls" to firestoreProduct.imageUrls,
            "rating" to (firestoreProduct.rating ?: 0.0),
            "category" to (firestoreProduct.category ?: ""),
            "createdAt" to (existingCreatedAt ?: System.currentTimeMillis()),
            "updatedAt" to System.currentTimeMillis()
        )

        firestoreProduct.sellerName?.let { productData["sellerName"] = it }
        firestoreProduct.latitude?.let { productData["latitude"] = it }
        firestoreProduct.longitude?.let { productData["longitude"] = it }

        // DEBUG ONLY - upsertProduct salva na coleção global para compatibilidade
        // CRÍTICO: Produtos devem ser criados via Cloud Function (createProduct) que salva em locations/{city}_{state}/products
        // Este método está aqui apenas para compatibilidade durante migração
        android.util.Log.w("FirestoreProductsRepo", 
            "⚠️ upsertProduct salva na coleção global. Use Cloud Function createProduct para salvar em locations/{city}_{state}/products")
        
        if (operation == "create") {
            productsCollectionGlobal.document(entityId).set(productData).await()
        } else {
            productsCollectionGlobal.document(entityId).set(
                productData,
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        }
    }

    override suspend fun deleteProduct(id: String) {
        try {
            // DEBUG ONLY - Marcar como inativo na coleção global (compatibilidade)
            // TODO: Marcar como inativo em todas as locations se necessário
            productsCollectionGlobal.document(id).update(
                "active", false,
                "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao marcar inativo no público: ${e.message}", e)
        }

        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId != null) {
            try {
                firestore.collection("users").document(currentUserId)
                    .collection("products").document(id).update(
                        "active", false,
                        "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ).await()
            } catch (e: Exception) {
                android.util.Log.e("FirestoreProductsRepo", "Erro ao marcar inativo na subcoleção: ${e.message}", e)
            }
        }
    }

    // Carrinho permanece local
    override suspend fun addToCart(productId: String, qtyDelta: Int) {
        val existing = cartDao.getByProductId(productId)
        if (existing != null) {
            val newQty = existing.qty + qtyDelta
            if (newQty <= 0) {
                cartDao.deleteByProductId(productId)
            } else {
                cartDao.upsert(existing.copy(qty = newQty))
            }
        } else if (qtyDelta > 0) {
            cartDao.upsert(CartItem(productId, qtyDelta).toEntity())
        }
    }

    override suspend fun removeFromCart(productId: String) {
        cartDao.deleteByProductId(productId)
    }

    override fun observeCart(): Flow<List<CartItem>> {
        return cartDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun clearCart() {
        cartDao.clearAll()
    }
}
