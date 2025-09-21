package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.ProductRepository
import com.example.taskgo.backend.ProductListResponse
import com.example.taskgo.backend.ErrorResponse
import com.example.taskgo.backend.domain.ProductCreate
import com.example.taskgo.backend.domain.ProductUpdate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import com.example.taskgo.backend.realtime.EventBus

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

        // Exemplo de rota protegida por papel (somente PROVIDER e ADMIN podem ver produtos inativos)
        authenticate("auth-jwt") {
            get("/admin/list-all") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN" && role != "PROVIDER") {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden"))
                    return@get
                }
                val search = call.request.queryParameters["search"]
                val category = call.request.queryParameters["category"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val items = repo.list(search, category, page, size)
                call.respond(ProductListResponse(items, page, size))
            }

            // CRUD admin de produtos
            post("/admin") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden")); return@post }
                val body = try { call.receive<ProductCreate>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid body")); return@post
                }
                val created = repo.create(body)
                call.respond(HttpStatusCode.Created, created)
                try { EventBus.publish("products", "created:${created.id}") } catch (_: Exception) {}
            }

            patch("/admin/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden")); return@patch }
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid id")); return@patch }
                val body = try { call.receive<ProductUpdate>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid body")); return@patch
                }
                val updated = repo.update(id, body)
                if (updated == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("not found")) else call.respond(updated)
                if (updated != null) { try { EventBus.publish("products", "updated:${updated.id}") } catch (_: Exception) {} }
            }

            delete("/admin/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden")); return@delete }
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid id")); return@delete }
                val ok = repo.delete(id)
                if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                if (ok) { try { EventBus.publish("products", "deleted:${id}") } catch (_: Exception) {} }
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

