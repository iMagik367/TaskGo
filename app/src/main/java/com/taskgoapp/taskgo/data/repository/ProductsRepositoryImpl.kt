package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.ProductDao
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.data.local.entity.ProductImageEntity
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toModel
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

    override fun observeProducts(): Flow<List<Product>> {
        return productDao.observeAll().map { entities ->
            entities.map { entity ->
                val images = productDao.images(entity.id).map { it.toModel() }
                entity.toModel(images)
            }
        }
    }

    override fun observeProductErrors(): Flow<String> = emptyFlow()

    override suspend fun getProduct(id: String): Product? {
        val entity = productDao.getById(id) ?: return null
        val images = productDao.images(id).map { it.toModel() }
        return entity.toModel(images)
    }

    override suspend fun getMyProducts(): List<Product> {
        return productDao.getAll().map { entity ->
            val images = productDao.images(entity.id).map { it.toModel() }
            entity.toModel(images)
        }
    }

    override suspend fun upsertProduct(product: Product) {
        productDao.upsert(product.toEntity())
        
        // Update images
        productDao.deleteImagesByProductId(product.id)
        if (product.imageUris.isNotEmpty()) {
            val imageEntities = product.imageUris.map { uri ->
                ProductImageEntity(
                    productId = product.id,
                    uri = uri
                )
            }
            productDao.upsertImages(imageEntities)
        }
    }

    override suspend fun deleteProduct(id: String) {
        val entity = productDao.getById(id) ?: return
        productDao.deleteImagesByProductId(id)
        productDao.delete(entity)
    }

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