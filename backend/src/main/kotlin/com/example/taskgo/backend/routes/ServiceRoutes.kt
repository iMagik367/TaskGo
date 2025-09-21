package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.ServiceRepository
import com.example.taskgo.backend.domain.ServiceCreate
import com.example.taskgo.backend.domain.ServiceUpdate
import com.example.taskgo.backend.domain.Service
import kotlinx.serialization.Serializable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.example.taskgo.backend.ErrorResponse
import com.example.taskgo.backend.realtime.EventBus

@Serializable
data class ServiceListResponse(
    val items: List<Service>,
    val page: Int,
    val size: Int
)

fun Route.serviceRoutes(repo: ServiceRepository) {
    route("/services") {
        get {
            val search = call.request.queryParameters["search"]
            val category = call.request.queryParameters["category"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            val items = repo.list(search, category, page, size)
            call.respond(ServiceListResponse(items, page, size))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid id")); return@get }
            val s = repo.getById(id)
            if (s == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("not found")) else call.respond(s)
        }

        authenticate("auth-jwt") {
            route("/admin") {
                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val role = principal.payload.getClaim("role").asString()
                    if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden")); return@post }
                    val body = try { call.receive<ServiceCreate>() } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid body")); return@post
                    }
                    val created = repo.create(body)
                    call.respond(HttpStatusCode.Created, created)
                try { EventBus.publish("services", "created:${created.id}") } catch (_: Exception) {}
                }

                patch("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val role = principal.payload.getClaim("role").asString()
                    if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden")); return@patch }
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid id")); return@patch }
                    val body = try { call.receive<ServiceUpdate>() } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid body")); return@patch
                    }
                    val updated = repo.update(id, body)
                    if (updated == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("not found")) else call.respond(updated)
                    if (updated != null) { try { EventBus.publish("services", "updated:${updated.id}") } catch (_: Exception) {} }
                }

                delete("/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val role = principal.payload.getClaim("role").asString()
                    if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden")); return@delete }
                    val id = call.parameters["id"]?.toLongOrNull()
                    if (id == null) { call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid id")); return@delete }
                    val ok = repo.delete(id)
                    if (ok) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ErrorResponse("not found"))
                    if (ok) { try { EventBus.publish("services", "deleted:${id}") } catch (_: Exception) {} }
                }
            }
        }
    }
}


