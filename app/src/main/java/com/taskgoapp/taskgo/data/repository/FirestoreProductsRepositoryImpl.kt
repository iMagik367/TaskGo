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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProductsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao
) : ProductsRepository {

    private val productsCollection = firestore.collection("products")
    private val productErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listener: ListenerRegistration? = try {
            productsCollection
                .whereEqualTo("active", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreProductsRepo", "Erro no listener de produtos: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
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
                            android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    trySend(products)
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
            val document = productsCollection.document(id).get(Source.SERVER).await()
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
            val snapshot = productsCollection
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("active", true)
                .get(Source.SERVER)
                .await()
            snapshot.documents.mapNotNull { doc ->
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
                    android.util.Log.e("FirestoreProductsRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                    null
                }
            }
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
                val existingDoc = productsCollection.document(entityId).get(Source.SERVER).await()
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

        if (operation == "create") {
            firestore.collection("products").document(entityId).set(productData).await()
        } else {
            firestore.collection("products").document(entityId).set(
                productData,
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        }
    }

    override suspend fun deleteProduct(id: String) {
        try {
            productsCollection.document(id).update(
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
