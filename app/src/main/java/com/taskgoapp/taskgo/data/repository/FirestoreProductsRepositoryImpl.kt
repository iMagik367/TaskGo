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
import com.taskgoapp.taskgo.core.location.LocationValidator
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emitAll
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
) : ProductsRepository {

    private val productErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listener: ListenerRegistration? = try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val location = com.taskgoapp.taskgo.core.location.OperationalLocation(
                city = userCity,
                state = userState,
                locationId = locationId,
                source = com.taskgoapp.taskgo.core.location.LocationSource.PROFILE
            )
            
            val collectionToUse = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
            
            collectionToUse
                .whereEqualTo("active", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val products = snapshot?.documents?.mapNotNull { doc ->
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
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(products)
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { listener?.remove() }
    }
    
    override fun observeProductsBySeller(sellerId: String): Flow<List<Product>> = callbackFlow {
        val listener: ListenerRegistration? = try {
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
            val userState = currentUser?.state?.takeIf { it.isNotBlank() }
            
            if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            val collectionToUse = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
            
            collectionToUse
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("active", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val products = snapshot?.documents?.mapNotNull { doc ->
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
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(products)
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { listener?.remove() }
    }

    override fun observeProductErrors(): Flow<String> = productErrors.asSharedFlow()

    override suspend fun getProduct(id: String): Product? {
        return try {
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                return null
            }
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val locationCollection = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
            val document = locationCollection.document(id).get(Source.SERVER).await()
            if (!document.exists()) {
                Log.d("FirestoreProductsRepo", "Produto $id não encontrado em locations/$locationId/products")
                return null
            }
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
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                return emptyList()
            }
            
            val productsList = mutableListOf<Product>()
            val locationCollection = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
            val snapshot = locationCollection
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("active", true)
                .get(Source.SERVER)
                .await()
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            
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
            
            productsList
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "❌ Erro ao buscar meus produtos: ${e.message}", e)
            android.util.Log.e("FirestoreProductsRepo", "Stack trace:", e)
            emptyList()
        }
    }

    override suspend fun upsertProduct(product: Product) {
        val currentUser = userRepository.observeCurrentUser().first()
        val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
        val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
        
        if (userCity.isBlank() || userState.isBlank()) {
            throw Exception("Usuário não tem city/state no perfil")
        }
        
        val locationId = LocationHelper.normalizeLocationId(userCity, userState)
        
        val firestoreProduct = product.toFirestore().copy(
            sellerId = firebaseAuth.currentUser?.uid ?: "",
            active = true
        )
        val operation = if (product.id.isNotEmpty()) "update" else "create"
        val entityId = product.id.ifEmpty { java.util.UUID.randomUUID().toString() }

        val existingCreatedAt = if (operation == "update") {
            try {
                val locationCollection = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
                val existingDoc = locationCollection.document(entityId).get(Source.SERVER).await()
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
            "locationId" to locationId, // CRÍTICO: Adicionar locationId para busca eficiente (SSR, reviews, etc)
            "createdAt" to (existingCreatedAt ?: System.currentTimeMillis()),
            "updatedAt" to System.currentTimeMillis()
        )

        firestoreProduct.sellerName?.let { productData["sellerName"] = it }
        firestoreProduct.latitude?.let { productData["latitude"] = it }
        firestoreProduct.longitude?.let { productData["longitude"] = it }

        // CRÍTICO: Produtos devem ser criados via Cloud Function (createProduct) que salva em locations/{city}_{state}/products
        // Este método está aqui apenas para compatibilidade, mas também salva na coleção por localização
        val locationCollection = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
        
        android.util.Log.d("FirestoreProductsRepo", 
            "📍 upsertProduct salvando em locations/$locationId/products")
        
        if (operation == "create") {
            locationCollection.document(entityId).set(productData).await()
        } else {
            locationCollection.document(entityId).set(
                productData,
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        }
    }

    override suspend fun deleteProduct(id: String) {
        try {
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                return
            }
            
            val locationCollection = LocationHelper.getLocationCollection(firestore, "products", userCity, userState)
            
            locationCollection.document(id).update(
                "active", false,
                "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProductsRepo", "Erro ao marcar produto como inativo: ${e.message}", e)
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
