package com.example.taskgoapp.core.data.repositories

import com.example.taskgoapp.core.data.models.*
import com.example.taskgoapp.core.data.remote.ProductsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface MarketplaceRepository {
    fun getProducts(): Flow<List<Product>>
    fun getProductById(id: Long): Flow<Product?>
}

@Singleton
class MarketplaceRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi
) : MarketplaceRepository {
    
    override fun getProducts(): Flow<List<Product>> = flow {
        try {
            val response = productsApi.list()
            val products = response.items.map { dto ->
                Product(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    price = dto.price,
                    category = dto.category,
                    seller = null // Backend não retorna seller ainda
                )
            }
            emit(products)
        } catch (e: Exception) {
            // Fallback para dados mock em caso de erro
            emit(emptyList())
        }
    }
    
    override fun getProductById(id: Long): Flow<Product?> = flow {
        try {
            val dto = productsApi.getById(id)
            val product = Product(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                price = dto.price,
                category = dto.category,
                seller = null // Backend não retorna seller ainda
            )
            emit(product)
        } catch (e: Exception) {
            emit(null)
        }
    }
}