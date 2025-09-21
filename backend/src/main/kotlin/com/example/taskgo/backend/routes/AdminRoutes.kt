package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.domain.UserRole
import com.example.taskgo.backend.auth.JwtConfig
import com.example.taskgo.backend.domain.ProductRepository
import com.example.taskgo.backend.domain.ServiceRepository
import com.example.taskgo.backend.domain.UserDetailsUpdate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import com.example.taskgo.backend.realtime.EventBus

@Serializable
data class UpdateRoleRequest(val role: String)

@Serializable
data class AdminLoginRequest(val username: String, val password: String)

@Serializable
data class UpdateUserRequest(val name: String? = null, val role: String? = null)

@Serializable
data class UpdateUserDetailsRequest(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val documents: List<String>? = null,
    val profilePhoto: String? = null
)

@Serializable
data class AdminStatsResponse(
    val users_total: Int,
    val users_admin: Int,
    val users_provider: Int,
    val users_customer: Int,
    val products_total: Int,
    val products_by_category: Map<String, Int> = emptyMap(),
    val services_total: Int = 0,
    val services_by_category: Map<String, Int> = emptyMap()
)

fun Route.adminRoutes(userRepository: UserRepository, productRepository: ProductRepository, serviceRepository: ServiceRepository? = null) {
    // Login do painel admin por usuário/senha
    route("/admin") {
        post("/login") {
            val body = try { call.receive<AdminLoginRequest>() } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body")); return@post
            }

            val adminEmailEnv = System.getenv("ADMIN_EMAIL") ?: "admin@example.com"
            val usernameLower = body.username.trim().lowercase()
            val targetEmail = when (usernameLower) {
                "admin" -> adminEmailEnv
                else -> adminEmailEnv // por enquanto, aceitamos apenas o usuário "admin"
            }

            val user = userRepository.findUserByEmail(targetEmail)
            if (user == null || user.role != UserRole.ADMIN) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials")); return@post
            }

            val isValid = userRepository.validatePassword(targetEmail, body.password)
            if (!isValid) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials")); return@post
            }

            val token = JwtConfig.generateToken(targetEmail, UserRole.ADMIN)
            call.respond(mapOf("token" to token))
        }

        authenticate("auth-jwt") {
            get("/stats") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden")); return@get }

                val users = userRepository.listAll()
                val usersTotal = users.size
                val admins = users.count { it.role == UserRole.ADMIN }
                val providers = users.count { it.role == UserRole.PROVIDER }
                val customers = users.count { it.role == UserRole.CUSTOMER }

                // Obter um número razoável de produtos (funciona com JDBC e in-memory)
                val products = try { productRepository.list(null, null, 1, 1000) } catch (_: Exception) { emptyList() }
                val productsTotal = products.size
                val productsByCat = products.groupingBy { it.category.ifBlank { "(sem categoria)" } }.eachCount()

                // Serviços (opcional, se rep disponível)
                val services = try { serviceRepository?.list(null, null, 1, 1000) ?: emptyList() } catch (_: Exception) { emptyList() }
                val servicesTotal = services.size
                val servicesByCat = services.groupingBy { it.category.ifBlank { "(sem categoria)" } }.eachCount()

                call.respond(
                    AdminStatsResponse(
                        users_total = usersTotal,
                        users_admin = admins,
                        users_provider = providers,
                        users_customer = customers,
                        products_total = productsTotal,
                        products_by_category = productsByCat,
                        services_total = servicesTotal,
                        services_by_category = servicesByCat
                    )
                )
            }
        }
    }

    authenticate("auth-jwt") {
        route("/admin") {
            get("/users") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden")); return@get }
                val users = userRepository.listAll()
                call.respond(users)
            }

            patch("/users/{id}/role") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden")); return@patch }
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id")); return@patch }
                val req = try { call.receive<UpdateRoleRequest>() } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body")); return@patch }
                val newRole = try { UserRole.valueOf(req.role.uppercase()) } catch (_: Exception) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid role")); return@patch }
                val updated = userRepository.updateUserRole(id, newRole)
                if (updated == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found")) else call.respond(updated)
                if (updated != null) { try { EventBus.publish("users", "role:${updated.id}:${updated.role}") } catch (_: Exception) {} }
            }

            // Atualizar dados cadastrais do usuário (nome e/ou role)
            patch("/users/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden")); return@patch }
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id")); return@patch }
                val req = try { call.receive<UpdateUserRequest>() } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body")); return@patch }

                val users = userRepository.listAll()
                val existing = users.find { it.id == id }
                if (existing == null) { call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found")); return@patch }

                val newRole = req.role?.let { r -> try { UserRole.valueOf(r.uppercase()) } catch (_: Exception) { null } } ?: existing.role
                val updated = userRepository.updateUser(existing.copy(name = req.name ?: existing.name, role = newRole))
                call.respond(updated)
                try { EventBus.publish("users", "updated:${updated.id}") } catch (_: Exception) {}
            }

            // Endpoint para editar detalhes completos do usuário
            patch("/users/{id}/details") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden")); return@patch }
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id")); return@patch }
                val req = try { call.receive<UpdateUserDetailsRequest>() } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body")); return@patch
                }

                val details = UserDetailsUpdate(
                    name = req.name,
                    phone = req.phone,
                    address = req.address,
                    documents = req.documents,
                    profilePhoto = req.profilePhoto
                )
                val updated = userRepository.updateUserDetails(id, details)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
                } else {
                    call.respond(updated)
                    try { EventBus.publish("users", "details_updated:${updated.id}") } catch (_: Exception) {}
                }
            }

            // Endpoint para upload de arquivos
            post("/upload") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") { call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden")); return@post }
                
                try {
                    val multipart = call.receiveMultipart()
                    val uploadedFiles = mutableListOf<String>()
                    
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName ?: "file_${System.currentTimeMillis()}"
                                val fileBytes = part.streamProvider().readBytes()
                                
                                // Simular upload - em produção, salvaria em storage real
                                val fileUrl = "https://via.placeholder.com/400x300/00BD48/FFFFFF?text=${fileName.replace(" ", "+")}"
                                uploadedFiles.add(fileUrl)
                            }
                            else -> {}
                        }
                    }
                    
                    call.respond(mapOf("success" to true, "files" to uploadedFiles))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Upload failed: ${e.message}"))
                }
            }
        }
    }
}




