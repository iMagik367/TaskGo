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
    val active: Boolean = true
)

interface ProductRepository {
    suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Product>
    suspend fun getById(id: Long): Product?
}

class InMemoryProductRepository : ProductRepository {
    private val data: List<Product> = listOf(
        Product(1, "Guarda Roupa 6 Portas", "Guarda roupa com espelho, MDF.", 899.90, "Móveis"),
        Product(2, "Furadeira sem fio 18V", "Com 2 baterias.", 299.90, "Ferramentas"),
        Product(3, "Forno de Embutir 30L", "Elétrico 30L, grill.", 599.90, "Eletrodomésticos"),
        Product(4, "Martelo 500g", "Cabo de madeira.", 45.90, "Ferramentas")
    )

    override suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Product> {
        val filtered = data.asSequence()
            .filter { it.active }
            .filter { search.isNullOrBlank() || it.name.contains(search!!, true) || it.description.contains(search!!, true) }
            .filter { category.isNullOrBlank() || it.category.equals(category, true) }
            .toList()
        val from = (page - 1).coerceAtLeast(0) * size
        val to = (from + size).coerceAtMost(filtered.size)
        if (from >= filtered.size) return emptyList()
        return filtered.subList(from, to)
    }

    override suspend fun getById(id: Long): Product? = data.find { it.id == id && it.active }
}

