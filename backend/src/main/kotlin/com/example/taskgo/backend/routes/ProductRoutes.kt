package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.ProductRepository
import com.example.taskgo.backend.ProductListResponse
import com.example.taskgo.backend.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes(repo: ProductRepository) {
    route("/products") {
        get {
            try {
                val search = call.request.queryParameters["search"]
                val category = call.request.queryParameters["category"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val items = repo.list(search, category, page, size)
                call.respond(ProductListResponse(items, page, size))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unknown error"))
            }
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid id"))
                return@get
            }
            val product = repo.getById(id)
            if (product == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
            } else {
                call.respond(product)
            }
        }
    }
}

