package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.User
import com.example.taskgo.backend.domain.UserRepository
import java.sql.Connection
import javax.sql.DataSource

class UserRepositoryJdbc(private val dataSource: DataSource) : UserRepository {
    override suspend fun createUser(email: String, passwordHash: String): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement("""
                INSERT INTO users (email, password_hash)
                VALUES (?, ?)
                RETURNING id, email, created_at
            """.trimIndent()
            ).use { ps ->
                ps.setString(1, email)
                ps.setString(2, passwordHash)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return User(
                        id = rs.getLong("id"),
                        email = rs.getString("email"),
                        name = null,
                        createdAt = rs.getTimestamp("created_at").time
                    )
                }
            }
        }
    }

    override suspend fun findUserByEmail(email: String): User? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT id, email, created_at FROM users WHERE email = ? LIMIT 1"
            ).use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) {
                        User(
                            id = rs.getLong("id"),
                            email = rs.getString("email"),
                            name = null,
                            createdAt = rs.getTimestamp("created_at").time
                        )
                    } else null
                }
            }
        }
    }

    override suspend fun updateUser(user: User): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE users SET name = ? WHERE id = ?"
            ).use { ps ->
                ps.setString(1, user.name)
                ps.setLong(2, user.id)
                ps.executeUpdate()
            }
        }
        return user
    }
}


