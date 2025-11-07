package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toModel
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toFirestore
import com.taskgoapp.taskgo.data.mapper.CartMapper.toModel
import com.taskgoapp.taskgo.data.mapper.CartMapper.toEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProductsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao
) : ProductsRepository {
    
    private val productsCollection = firestore.collection("products")

    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val listenerRegistration = productsCollection
            .whereEqualTo("active", true)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)?.toModel()
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getProduct(id: String): Product? {
        return try {
            val document = productsCollection.document(id).get().await()
            document.toObject(ProductFirestore::class.java)?.copy(id = document.id)?.toModel()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMyProducts(): List<Product> {
        val userId = firebaseAuth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = productsCollection
                .whereEqualTo("sellerId", userId)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)?.toModel()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun upsertProduct(product: Product) {
        val firestoreProduct = product.toFirestore().copy(
            sellerId = firebaseAuth.currentUser?.uid ?: ""
        )
        
        try {
            if (product.id.isNotEmpty()) {
                productsCollection.document(product.id).set(firestoreProduct).await()
            } else {
                productsCollection.add(firestoreProduct).await()
            }
        } catch (e: Exception) {
            // Silently fail for now
        }
    }

    override suspend fun deleteProduct(id: String) {
        try {
            productsCollection.document(id).update("active", false).await()
        } catch (e: Exception) {
            // Silently fail for now
        }
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

    override fun observeCart(): Flow<List<CartItem>> {
        return cartDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun clearCart() {
        cartDao.clearAll()
    }
}

