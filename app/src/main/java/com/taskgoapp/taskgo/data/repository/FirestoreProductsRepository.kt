package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProductsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val productsCollection = firestore.collection("products")

    fun observeAllProducts(): Flow<List<ProductFirestore>> = callbackFlow {
        val listenerRegistration = productsCollection
            .whereEqualTo("active", true)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    fun observeProductsBySeller(sellerId: String): Flow<List<ProductFirestore>> = callbackFlow {
        val listenerRegistration = productsCollection
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("active", true)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProductFirestore::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(products)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getProduct(productId: String): ProductFirestore? {
        return try {
            val document = productsCollection.document(productId).get().await()
            document.toObject(ProductFirestore::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createProduct(product: ProductFirestore): Result<String> {
        return try {
            val docRef = productsCollection.add(product).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(productId: String, product: ProductFirestore): Result<Unit> {
        return try {
            productsCollection.document(productId).set(product).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productsCollection.document(productId).update(
                "active", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProducts(query: String): Flow<List<ProductFirestore>> = callbackFlow {
        val listenerRegistration = productsCollection
            .whereEqualTo("active", true)
            .orderBy("title")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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

