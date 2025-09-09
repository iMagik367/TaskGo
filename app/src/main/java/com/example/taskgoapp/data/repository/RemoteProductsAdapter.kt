package com.example.taskgoapp.data.repository

import com.example.taskgoapp.BuildConfig
import com.example.taskgoapp.core.data.remote.CartApi
import com.example.taskgoapp.core.data.remote.ProductsApi
import com.example.taskgoapp.domain.repository.ProductsRepository
import com.example.taskgoapp.core.model.Product
import com.example.taskgoapp.core.model.CartItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProductsAdapter @Inject constructor(
    private val productsApi: ProductsApi,
    private val cartApi: CartApi
) : ProductsRepository {
    override fun observeProducts() = throw UnsupportedOperationException("Not observable remotely yet")

    override suspend fun getProduct(id: String): Product? {
        val dto = productsApi.getById(id.toLong())
        return Product(
            id = dto.id.toString(),
            title = dto.name,
            description = dto.description,
            price = dto.price,
            sellerName = null,
            imageUris = emptyList()
        )
    }

    override suspend fun getMyProducts(): List<Product> {
        return productsApi.list().map { dto ->
            Product(
                id = dto.id.toString(),
                title = dto.name,
                description = dto.description,
                price = dto.price,
                sellerName = null,
                imageUris = emptyList()
            )
        }
    }

    override suspend fun upsertProduct(product: Product) {
        throw UnsupportedOperationException("Upsert remoto não implementado")
    }

    override suspend fun deleteProduct(id: String) {
        throw UnsupportedOperationException("Delete remoto não implementado")
    }

    override suspend fun addToCart(productId: String, qtyDelta: Int) {
        if (!BuildConfig.USE_REMOTE_API) return
        if (qtyDelta > 0) cartApi.add(com.example.taskgoapp.core.data.remote.AddCartItemRequest(productId.toLong(), qtyDelta))
        else cartApi.remove(productId.toLong())
    }

    override fun observeCart() = throw UnsupportedOperationException("Not observable remotely yet")

    override suspend fun clearCart() {
        // No endpoint de limpar direto; poderia iterar removendo, ou via DELETE /cart (futuro)
    }
}


