package com.example.taskgo.backend.domain

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val bannerUrl: String? = null,
    val active: Boolean = true,
    val seller: User? = null,
    val photos: List<String> = emptyList()
)

interface ProductRepository {
    suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Product>
    suspend fun getById(id: Long): Product?
    suspend fun create(product: ProductCreate): Product
    suspend fun update(id: Long, update: ProductUpdate): Product?
    suspend fun delete(id: Long): Boolean
}

@Serializable
data class ProductCreate(
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val bannerUrl: String? = null,
    val active: Boolean = true,
    val photos: List<String> = emptyList()
)

@Serializable
data class ProductUpdate(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val bannerUrl: String? = null,
    val active: Boolean? = null,
    val photos: List<String>? = null
)

class InMemoryProductRepository : ProductRepository {
    private val dataMut = mutableListOf(
        Product(1, "Guarda Roupa 6 Portas", "Guarda roupa com espelho, MDF.", 899.90, "Móveis", "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400", true, null, listOf("https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400", "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400")),
        Product(2, "Furadeira sem fio 18V", "Com 2 baterias.", 299.90, "Ferramentas", "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=400", true, null, listOf("https://images.unsplash.com/photo-1504148455328-c376907d081c?w=400", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400")),
        Product(3, "Forno de Embutir 30L", "Elétrico 30L, grill.", 599.90, "Eletrodomésticos", "https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=400", true, null, listOf("https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=400", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400")),
        Product(4, "Martelo 500g", "Cabo de madeira.", 45.90, "Ferramentas", "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=400", true, null, listOf("https://images.unsplash.com/photo-1504148455328-c376907d081c?w=400"))
    )

    override suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Product> {
        val filtered = dataMut.asSequence()
            .filter { it.active }
            .filter { search.isNullOrBlank() || it.name.contains(search!!, true) || it.description.contains(search!!, true) }
            .filter { category.isNullOrBlank() || it.category.equals(category, true) }
            .toList()
        val from = (page - 1).coerceAtLeast(0) * size
        val to = (from + size).coerceAtMost(filtered.size)
        if (from >= filtered.size) return emptyList()
        return filtered.subList(from, to)
    }

    override suspend fun getById(id: Long): Product? = dataMut.find { it.id == id }

    override suspend fun create(product: ProductCreate): Product {
        val nextId = (dataMut.maxOfOrNull { it.id } ?: 0L) + 1
        val created = Product(
            id = nextId,
            name = product.name,
            description = product.description,
            price = product.price,
            category = product.category,
            bannerUrl = product.bannerUrl,
            active = product.active,
            photos = product.photos
        )
        dataMut += created
        return created
    }

    override suspend fun update(id: Long, update: ProductUpdate): Product? {
        val idx = dataMut.indexOfFirst { it.id == id }
        if (idx == -1) return null
        val current = dataMut[idx]
        val updated = current.copy(
            name = update.name ?: current.name,
            description = update.description ?: current.description,
            price = update.price ?: current.price,
            category = update.category ?: current.category,
            bannerUrl = update.bannerUrl ?: current.bannerUrl,
            active = update.active ?: current.active,
            photos = update.photos ?: current.photos
        )
        dataMut[idx] = updated
        return updated
    }

    override suspend fun delete(id: Long): Boolean {
        return dataMut.removeIf { it.id == id }
    }
}

