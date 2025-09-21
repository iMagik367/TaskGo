package com.example.taskgo.backend.auth

import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.domain.UserRole
import com.example.taskgo.backend.domain.User
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.net.URL
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun Route.oauthRoutes(userRepository: UserRepository) {
    route("/auth/oauth") {
        /**
         * POST /auth/oauth/google { token }
         * Recebe o token do Google, valida, retorna JWT do sistema
         */
        post("/google") {
            val params = call.receiveParameters()
            val token = params["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "token required"))
            val userInfo = verifyGoogleToken(token)
            if (userInfo == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid google token"))
                return@post
            }
            // Busca ou cria usuário
            val user = userRepository.findUserByEmail(userInfo.email)
                ?: userRepository.createUser(userInfo.email, "", UserRole.CUSTOMER)
            // Gera JWT
            val jwt = com.example.taskgo.backend.auth.JwtConfig.generateToken(user.email, user.role)
            call.respond(mapOf("token" to jwt, "user" to user))
        }
        // TODO: Adicionar rota para Facebook
    }
}

private fun verifyGoogleToken(token: String): OAuthUserInfo? {
    val url = URL("https://oauth2.googleapis.com/tokeninfo?id_token=$token")
    val conn = url.openConnection() as HttpsURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 5000
    conn.readTimeout = 5000
    return try {
        if (conn.responseCode != 200) return null
        val json = conn.inputStream.bufferedReader().readText()
        val obj = Json.parseToJsonElement(json).jsonObject
        val email = obj["email"]?.jsonPrimitive?.content ?: return null
        val name = obj["name"]?.jsonPrimitive?.content
        val picture = obj["picture"]?.jsonPrimitive?.content
        val sub = obj["sub"]?.jsonPrimitive?.content ?: ""
        OAuthUserInfo(email, name, picture, "google", sub)
    } catch (e: Exception) {
        null
    } finally {
        conn.disconnect()
    }
}
