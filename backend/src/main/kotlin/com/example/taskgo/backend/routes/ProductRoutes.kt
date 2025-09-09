package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.InMemoryProductRepository
import com.example.taskgo.backend.domain.ProductRepository
import com.example.taskgo.backend.repository.ProductRepositoryJdbc
import com.example.taskgo.backend.db.Database
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes(repo: ProductRepository? = null) {
    val resolvedRepo: ProductRepository = repo
        ?: run {
            val useDb = System.getenv("DB_ENABLE")?.equals("true", ignoreCase = true) == true
            if (useDb) {
                val dataSource = try {
                    Database.dataSource
                } catch (e: Exception) {
                    Database.init()
                }
                ProductRepositoryJdbc(dataSource)
            } else {
                InMemoryProductRepository()
            }
        }
    route("/products") {
        get {
            val search = call.request.queryParameters["search"]
            val category = call.request.queryParameters["category"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            val items = resolvedRepo.list(search, category, page, size)
            call.respond(mapOf("items" to items, "page" to page, "size" to size))
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                return@get
            }
            val product = resolvedRepo.getById(id)
            if (product == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "not found"))
            } else {
                call.respond(product)
            }
        }
    }
}

