package com.example.taskgo.backend.domain

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val active: Boolean = true,
    val provider: User? = null,
    val photos: List<String> = emptyList()
)

interface ServiceRepository {
    suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Service>
    suspend fun getById(id: Long): Service?
    suspend fun create(service: ServiceCreate): Service
    suspend fun update(id: Long, update: ServiceUpdate): Service?
    suspend fun delete(id: Long): Boolean
}

@Serializable
data class ServiceCreate(
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val active: Boolean = true,
    val photos: List<String> = emptyList()
)

@Serializable
data class ServiceUpdate(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val active: Boolean? = null,
    val photos: List<String>? = null
)

class InMemoryServiceRepository : ServiceRepository {
    private val dataMut = mutableListOf(
        Service(1, "Instalação de TV", "Fixação e configuração de TV na parede", 150.0, "Instalação", true, null, listOf("https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=400", "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400")),
        Service(2, "Montagem de Móveis", "Montagem de guarda-roupa e estante", 200.0, "Montagem", true, null, listOf("https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400")),
        Service(3, "Reparo Elétrico", "Troca de tomada e disjuntor", 90.0, "Elétrica", true, null, listOf("https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=400"))
    )

    override suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Service> {
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

    override suspend fun getById(id: Long): Service? = dataMut.find { it.id == id }

    override suspend fun create(service: ServiceCreate): Service {
        val nextId = (dataMut.maxOfOrNull { it.id } ?: 0L) + 1
        val created = Service(
            id = nextId,
            name = service.name,
            description = service.description,
            price = service.price,
            category = service.category,
            active = service.active,
            photos = service.photos
        )
        dataMut += created
        return created
    }

    override suspend fun update(id: Long, update: ServiceUpdate): Service? {
        val idx = dataMut.indexOfFirst { it.id == id }
        if (idx == -1) return null
        val current = dataMut[idx]
        val updated = current.copy(
            name = update.name ?: current.name,
            description = update.description ?: current.description,
            price = update.price ?: current.price,
            category = update.category ?: current.category,
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



