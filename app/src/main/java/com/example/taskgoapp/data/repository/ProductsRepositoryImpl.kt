package com.example.taskgoapp.data.repository

import com.example.taskgoapp.data.local.dao.ProductDao
import com.example.taskgoapp.data.local.dao.CartDao
import com.example.taskgoapp.data.local.entity.ProductImageEntity
import com.example.taskgoapp.data.mapper.ProductMapper.toEntity
import com.example.taskgoapp.data.mapper.ProductMapper.toModel
import com.example.taskgoapp.data.mapper.CartMapper.toEntity
import com.example.taskgoapp.data.mapper.CartMapper.toModel
import com.example.taskgoapp.domain.repository.ProductsRepository
import com.example.taskgoapp.core.model.Product
import com.example.taskgoapp.core.model.CartItem
import kotlinx.coroutines.flow.Flow
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

    private fun generateId(): String {
        return "product_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}