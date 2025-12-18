package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toModel
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toFirestore
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.CartMapper.toModel
import com.taskgoapp.taskgo.data.mapper.CartMapper.toEntity
import com.taskgoapp.taskgo.data.local.dao.ProductDao
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProductsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val syncManager: com.taskgoapp.taskgo.core.sync.SyncManager,
    private val realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository
) : ProductsRepository {
    
    private val productsCollection = firestore.collection("products")
    private val productErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeProducts(): Flow<List<Product>> {
        // 1. Primeiro, buscar do cache local (instantâneo)
        // Retornar produtos do cache imediatamente
        val cachedProductsFlow = productDao.observeAll().flatMapLatest { entities ->
            flow {
                val products = withContext(Dispatchers.IO) {
                    entities.map { entity ->
                        val images = productDao.images(entity.id).map { it.toModel() }
                        entity.toModel(images)
                    }
                }
                emit(products)
            }
        }
        
        // 2. Sincroniza com Firebase em background quando o Flow é coletado
        @OptIn(ExperimentalCoroutinesApi::class)
        return cachedProductsFlow.onStart {
            try {
                // Tentar cache primeiro, depois servidor
                val snapshot = try {
                    productsCollection
                        .whereEqualTo("active", true)
                        .get(Source.CACHE)
                        .await()
                } catch (e: Exception) {
                    // Se não houver cache, buscar do servidor
                    productsCollection
                    .whereEqualTo("active", true)
                        .get(Source.SERVER)
                    .await()
                }
                
                val firestoreProducts = snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        // Converter manualmente para tratar createdAt como Long ou Date
                        val createdAt = when (val createdAtValue = data["createdAt"]) {
                            is Long -> java.util.Date(createdAtValue)
                            is java.util.Date -> createdAtValue
                            is com.google.firebase.Timestamp -> createdAtValue.toDate()
                            else -> null
                        }
                        val updatedAt = when (val updatedAtValue = data["updatedAt"]) {
                            is Long -> java.util.Date(updatedAtValue)
                            is java.util.Date -> updatedAtValue
                            is com.google.firebase.Timestamp -> updatedAtValue.toDate()
                            else -> null
                        }
                        val productFirestore = ProductFirestore(
                            id = doc.id,
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
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            rating = (data["rating"] as? Number)?.toDouble(),
                            latitude = (data["latitude"] as? Number)?.toDouble(),
                            longitude = (data["longitude"] as? Number)?.toDouble()
                        )
                        productFirestore.toModel()
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                
                // Atualiza cache local com dados do Firebase
                // Quando atualizar, o Flow do Room emitirá automaticamente os novos dados
                firestoreProducts.forEach { product ->
                    productDao.upsert(product.toEntity())
                    productDao.deleteImagesByProductId(product.id)
                    if (product.imageUris.isNotEmpty()) {
                        val imageEntities = product.imageUris.map { uri ->
                            com.taskgoapp.taskgo.data.local.entity.ProductImageEntity(
                                productId = product.id,
                                uri = uri
                            )
                        }
                        productDao.upsertImages(imageEntities)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FirestoreProductsRepo", "Erro ao sincronizar produtos: ${e.message}", e)
                productErrors.emit("Erro ao carregar produtos: ${e.message}")
            }
        }
    }

    override fun observeProductErrors(): Flow<String> = productErrors.asSharedFlow()

    override suspend fun getProduct(id: String): Product? {
        // 1. Tenta buscar do cache local primeiro (instantâneo)
        val cachedEntity = productDao.getById(id)
        if (cachedEntity != null) {
            val images = productDao.images(id).map { it.toModel() }
            return cachedEntity.toModel(images)
        }
        
        // 2. Se não encontrou no cache, busca do Firebase
        return try {
            val document = productsCollection.document(id).get().await()
            val data = document.data ?: return null
            
            // Converter manualmente para tratar createdAt como Long ou Date
            val createdAt = when (val createdAtValue = data["createdAt"]) {
                is Long -> java.util.Date(createdAtValue)
                is java.util.Date -> createdAtValue
                is com.google.firebase.Timestamp -> createdAtValue.toDate()
                else -> null
            }
            val updatedAt = when (val updatedAtValue = data["updatedAt"]) {
                is Long -> java.util.Date(updatedAtValue)
                is java.util.Date -> updatedAtValue
                is com.google.firebase.Timestamp -> updatedAtValue.toDate()
                else -> null
            }
            
            val productFirestore = ProductFirestore(
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
                createdAt = createdAt,
                updatedAt = updatedAt,
                rating = (data["rating"] as? Number)?.toDouble(),
                latitude = (data["latitude"] as? Number)?.toDouble(),
                longitude = (data["longitude"] as? Number)?.toDouble()
            )
            
            val product = productFirestore.toModel()
            
            // Salva no cache para próximas consultas
            productDao.upsert(product.toEntity())
            productDao.deleteImagesByProductId(product.id)
            if (product.imageUris.isNotEmpty()) {
                val imageEntities = product.imageUris.map { uri ->
                    com.taskgoapp.taskgo.data.local.entity.ProductImageEntity(
                        productId = product.id,
                        uri = uri
                    )
                }
                productDao.upsertImages(imageEntities)
            }
            
            product
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao buscar produto $id: ${e.message}", e)
            null
        }
    }

    override suspend fun getMyProducts(): List<Product> {
        val userId = firebaseAuth.currentUser?.uid ?: return emptyList()
        
        // 1. Retorna produtos do cache local primeiro (instantâneo)
        val cachedProducts = productDao.getAll().map { entity ->
            val images = productDao.images(entity.id).map { it.toModel() }
            entity.toModel(images)
        }.filter { 
            // Filtrar apenas produtos do usuário atual (se tiver sellerId no cache)
            true // Por enquanto retornar todos do cache
        }
        
        // 2. Sincroniza com Firebase em background (tentar cache primeiro)
        syncScope.launch {
            try {
                val snapshot = try {
                    // Tentar cache primeiro
                    productsCollection
                        .whereEqualTo("sellerId", userId)
                        .whereEqualTo("active", true)
                        .get(Source.CACHE)
                        .await()
                } catch (e: Exception) {
                    // Se não houver cache, buscar do servidor
                    productsCollection
                    .whereEqualTo("sellerId", userId)
                    .whereEqualTo("active", true)
                        .get(Source.SERVER)
                    .await()
                }
                
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        
                        // Converter manualmente para tratar createdAt como Long ou Date
                        val createdAt = when (val createdAtValue = data["createdAt"]) {
                            is Long -> java.util.Date(createdAtValue)
                            is java.util.Date -> createdAtValue
                            is com.google.firebase.Timestamp -> createdAtValue.toDate()
                            else -> null
                        }
                        val updatedAt = when (val updatedAtValue = data["updatedAt"]) {
                            is Long -> java.util.Date(updatedAtValue)
                            is java.util.Date -> updatedAtValue
                            is com.google.firebase.Timestamp -> updatedAtValue.toDate()
                            else -> null
                        }
                        
                        val productFirestore = ProductFirestore(
                            id = doc.id,
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
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            rating = (data["rating"] as? Number)?.toDouble(),
                            latitude = (data["latitude"] as? Number)?.toDouble(),
                            longitude = (data["longitude"] as? Number)?.toDouble()
                        )
                        
                        val product = productFirestore.toModel()
                        
                        productDao.upsert(product.toEntity())
                        productDao.deleteImagesByProductId(product.id)
                        if (product.imageUris.isNotEmpty()) {
                            val imageEntities = product.imageUris.map { uri ->
                                com.taskgoapp.taskgo.data.local.entity.ProductImageEntity(
                                    productId = product.id,
                                    uri = uri
                                )
                            }
                            productDao.upsertImages(imageEntities)
                        }
                        
                        product
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                        null
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FirestoreProductsRepo", "Erro ao sincronizar meus produtos: ${e.message}", e)
            }
        }
        
        return cachedProducts
    }

    override suspend fun upsertProduct(product: Product) {
        // 1. Salva localmente primeiro (instantâneo)
        productDao.upsert(product.toEntity())
        
        // Atualiza imagens localmente
        productDao.deleteImagesByProductId(product.id)
        if (product.imageUris.isNotEmpty()) {
            val imageEntities = product.imageUris.map { uri ->
                com.taskgoapp.taskgo.data.local.entity.ProductImageEntity(
                    productId = product.id,
                    uri = uri
                )
            }
            productDao.upsertImages(imageEntities)
        }
        
        // 2. Salva imediatamente no Firestore
        val firestoreProduct = product.toFirestore().copy(
            sellerId = firebaseAuth.currentUser?.uid ?: ""
        )
        
        val operation = if (product.id.isNotEmpty()) "update" else "create"
        val entityId = product.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        
        // Preservar createdAt original ao atualizar
        val existingCreatedAt = if (operation == "update") {
            try {
                val existingDoc = productsCollection.document(entityId).get().await()
                val existingData = existingDoc.data
                when (val createdAtValue = existingData?.get("createdAt")) {
                    is Long -> createdAtValue
                    is java.util.Date -> createdAtValue.time
                    is com.google.firebase.Timestamp -> createdAtValue.toDate().time
                    else -> null
                }
            } catch (e: Exception) {
                null // Se não conseguir buscar, usa timestamp atual
            }
        } else {
            null
        }
        
        val productData = mutableMapOf<String, Any>(
            "id" to entityId,
            "sellerId" to firestoreProduct.sellerId,
            "title" to firestoreProduct.title,
            "description" to (firestoreProduct.description ?: ""),
            "price" to firestoreProduct.price,
            "active" to firestoreProduct.active,
            "featured" to firestoreProduct.featured,
            "imageUrls" to firestoreProduct.imageUrls,
            "rating" to (firestoreProduct.rating ?: 0.0),
            "category" to (firestoreProduct.category ?: ""),
            "createdAt" to (existingCreatedAt ?: System.currentTimeMillis()),
            "updatedAt" to System.currentTimeMillis()
        )
            
        firestoreProduct.sellerName?.let { productData["sellerName"] = it }
        firestoreProduct.latitude?.let { productData["latitude"] = it }
        firestoreProduct.longitude?.let { productData["longitude"] = it }
            
        if (operation == "create") {
            firestore.collection("products").document(entityId).set(productData).await()
        } else {
            firestore.collection("products").document(entityId).set(productData, com.google.firebase.firestore.SetOptions.merge()).await()
        }
            
        try {
            realtimeRepository.saveProduct(entityId, productData)
        } catch (e: Exception) {
            // Ignora erro do Realtime Database
        }
    }

    override suspend fun deleteProduct(id: String) {
        // 1. Remove localmente primeiro (instantâneo)
        val entity = productDao.getById(id)
        if (entity != null) {
            productDao.deleteImagesByProductId(id)
            productDao.delete(entity)
        }
        
        // 2. Agenda sincronização com Firebase após 1 minuto
        val firestoreProduct = mapOf("active" to false)
        syncManager.scheduleSync(
            syncType = "product",
            entityId = id,
            operation = "delete",
            data = firestoreProduct
        )
    }

    // Cart is still local
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

