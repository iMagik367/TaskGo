package com.example.taskgo.server.db

import com.example.taskgo.server.domain.User
import com.example.taskgo.server.domain.UserRepository
import java.sql.Connection

class UserRepositoryJdbc(private val connectionProvider: () -> Connection) : UserRepository {
    override suspend fun create(name: String, email: String, passwordHash: String): User {
        connectionProvider().use { conn ->
            conn.prepareStatement("INSERT INTO users(name,email,password_hash) VALUES (?,?,?) RETURNING id").use { ps ->
                ps.setString(1, name)
                ps.setString(2, email.lowercase())
                ps.setString(3, passwordHash)
                val rs = ps.executeQuery()
                rs.next()
                val id = rs.getLong(1)
                return User(id, name, email.lowercase(), passwordHash)
            }
        }
    }

    override suspend fun findByEmail(email: String): User? {
        connectionProvider().use { conn ->
            conn.prepareStatement("SELECT id,name,email,password_hash FROM users WHERE email=?").use { ps ->
                ps.setString(1, email.lowercase())
                val rs = ps.executeQuery()
                return if (rs.next()) User(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    passwordHash = rs.getString("password_hash")
                ) else null
            }
        }
    }

    override suspend fun updateNameAndPassword(email: String, name: String, passwordHash: String): User? {
        connectionProvider().use { conn ->
            conn.prepareStatement("UPDATE users SET name=?, password_hash=? WHERE email=? RETURNING id,name,email,password_hash").use { ps ->
                ps.setString(1, name)
                ps.setString(2, passwordHash)
                ps.setString(3, email.lowercase())
                val rs = ps.executeQuery()
                return if (rs.next()) User(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    email = rs.getString("email"),
                    passwordHash = rs.getString("password_hash")
                ) else null
            }
        }
    }
}
