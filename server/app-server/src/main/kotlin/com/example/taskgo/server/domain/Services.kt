package com.example.taskgo.server.domain

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: Long,
    val ownerEmail: String,
    val title: String,
    val description: String,
    val category: String,
    val priceFrom: Double,
    val active: Boolean = true
)

@Serializable
data class Proposal(
    val id: Long,
    val serviceId: Long,
    val customerEmail: String,
    val message: String,
    val createdAt: Long
)

@Serializable
data class Review(
    val id: Long,
    val serviceId: Long,
    val authorEmail: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long
)

interface ServiceRepository {
    suspend fun list(category: String?, search: String?, page: Int, size: Int): List<Service>
    suspend fun create(ownerEmail: String, title: String, description: String, category: String, priceFrom: Double): Service
}

class InMemoryServiceRepository : ServiceRepository {
    private var seq = 1L
    private val data = mutableListOf<Service>()

    override suspend fun list(category: String?, search: String?, page: Int, size: Int): List<Service> =
        data.filter { s ->
            (category == null || s.category.equals(category, ignoreCase = true)) &&
            (search == null || s.title.contains(search, true) || s.description.contains(search, true))
        }.drop((page - 1) * size).take(size)

    override suspend fun create(ownerEmail: String, title: String, description: String, category: String, priceFrom: Double): Service {
        val s = Service(seq++, ownerEmail, title, description, category, priceFrom)
        data.add(s)
        return s
    }
}

interface ProposalRepository {
    suspend fun listByService(serviceId: Long): List<Proposal>
    suspend fun create(serviceId: Long, customerEmail: String, message: String): Proposal
}

class InMemoryProposalRepository : ProposalRepository {
    private var seq = 1L
    private val data = mutableListOf<Proposal>()

    override suspend fun listByService(serviceId: Long): List<Proposal> = data.filter { it.serviceId == serviceId }

    override suspend fun create(serviceId: Long, customerEmail: String, message: String): Proposal {
        val p = Proposal(seq++, serviceId, customerEmail, message, System.currentTimeMillis())
        data.add(p)
        return p
    }
}

interface ReviewRepository {
    suspend fun listByService(serviceId: Long): List<Review>
    suspend fun create(serviceId: Long, authorEmail: String, rating: Int, comment: String): Review
}

class InMemoryReviewRepository : ReviewRepository {
    private var seq = 1L
    private val data = mutableListOf<Review>()

    override suspend fun listByService(serviceId: Long): List<Review> = data.filter { it.serviceId == serviceId }

    override suspend fun create(serviceId: Long, authorEmail: String, rating: Int, comment: String): Review {
        val r = Review(seq++, serviceId, authorEmail, rating.coerceIn(1, 5), comment, System.currentTimeMillis())
        data.add(r)
        return r
    }
}


