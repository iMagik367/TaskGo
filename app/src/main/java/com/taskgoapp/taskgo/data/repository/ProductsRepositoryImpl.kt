package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.ProductDao
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.data.mapper.CartMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.CartMapper.toModel
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val cartDao: CartDao
) : ProductsRepository {

    // Cache local desabilitado para produtos; fonte = Firestore
    override fun observeProducts(): Flow<List<Product>> = emptyFlow()
    override fun observeProductsBySeller(sellerId: String): Flow<List<Product>> = emptyFlow()
    override fun observeProductErrors(): Flow<String> = emptyFlow()
    override suspend fun getProduct(id: String): Product? = null
    override suspend fun getMyProducts(): List<Product> = emptyList()
    override suspend fun upsertProduct(product: Product) { /* no-op */ }
    override suspend fun deleteProduct(id: String) { /* no-op */ }

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

    override suspend fun removeFromCart(productId: String) {
        cartDao.deleteByProductId(productId)
    }

    private fun generateId(): String {
        return "product_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}