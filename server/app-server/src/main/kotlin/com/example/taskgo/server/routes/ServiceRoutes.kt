package com.example.taskgo.server.routes

import com.example.taskgo.server.domain.InMemoryProposalRepository
import com.example.taskgo.server.domain.InMemoryReviewRepository
import com.example.taskgo.server.domain.InMemoryServiceRepository
import com.example.taskgo.server.domain.ProposalRepository
import com.example.taskgo.server.domain.ReviewRepository
import com.example.taskgo.server.domain.ServiceRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@kotlinx.serialization.Serializable
data class CreateServiceRequest(val title: String, val description: String, val category: String, val priceFrom: Double)

@kotlinx.serialization.Serializable
data class CreateProposalRequest(val serviceId: Long, val message: String)

@kotlinx.serialization.Serializable
data class CreateReviewRequest(val serviceId: Long, val rating: Int, val comment: String)

fun Route.serviceRoutes(
    services: ServiceRepository = InMemoryServiceRepository(),
    proposals: ProposalRepository = InMemoryProposalRepository(),
    reviews: ReviewRepository = InMemoryReviewRepository()
) {
    authenticate("auth-jwt") {
        route("/services") {
            get {
                val search = call.request.queryParameters["search"]
                val category = call.request.queryParameters["category"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                call.respond(services.list(category, search, page, size))
            }
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val req = call.receive<CreateServiceRequest>()
                call.respond(services.create(email, req.title, req.description, req.category, req.priceFrom))
            }
        }
        route("/proposals") {
            get {
                val serviceId = call.request.queryParameters["serviceId"]?.toLongOrNull()
                if (serviceId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "serviceId required"))
                    return@get
                }
                call.respond(proposals.listByService(serviceId))
            }
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val req = call.receive<CreateProposalRequest>()
                call.respond(proposals.create(req.serviceId, email, req.message))
            }
        }
        route("/reviews") {
            get {
                val serviceId = call.request.queryParameters["serviceId"]?.toLongOrNull()
                if (serviceId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "serviceId required"))
                    return@get
                }
                call.respond(reviews.listByService(serviceId))
            }
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val req = call.receive<CreateReviewRequest>()
                call.respond(reviews.create(req.serviceId, email, req.rating, req.comment))
            }
        }
    }
}


