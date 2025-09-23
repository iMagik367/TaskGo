package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.User
import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.domain.UserRole
import com.example.taskgo.backend.domain.UserDetailsUpdate
import javax.sql.DataSource

class UserRepositoryJdbc(private val dataSource: DataSource) : UserRepository {

    override suspend fun updatePasswordByEmail(email: String, newPasswordHash: String): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE users SET password_hash = ? WHERE email = ?").use { ps ->
                ps.setString(1, newPasswordHash)
                ps.setString(2, email)
                return ps.executeUpdate() > 0
            }
        }
    }
    override suspend fun createUser(email: String, passwordHash: String, role: UserRole): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO users (email, password_hash, role, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)"
            ).use { ps ->
                ps.setString(1, email)
                ps.setString(2, passwordHash)
                ps.setString(3, role.name)
                ps.executeUpdate()
            }
            conn.prepareStatement("SELECT id, email, name, role, created_at FROM users WHERE email = ? LIMIT 1")
                .use { ps ->
                    ps.setString(1, email)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return User(
                            id = rs.getLong("id"),
                            email = rs.getString("email"),
                            name = rs.getString("name") ?: rs.getString("email"),
                            createdAt = rs.getTimestamp("created_at").time,
                            role = UserRole.valueOf(rs.getString("role").uppercase())
                        )
                    }
                }
        }
    }

    override suspend fun findUserByEmail(email: String): User? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "SELECT id, email, name, role, created_at FROM users WHERE email = ? LIMIT 1"
            ).use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs ->
                    return if (rs.next()) {
                        User(
                            id = rs.getLong("id"),
                            email = rs.getString("email"),
                            name = rs.getString("name") ?: rs.getString("email"),
                            createdAt = rs.getTimestamp("created_at").time,
                            role = UserRole.valueOf(rs.getString("role").uppercase())
                        )
                    } else null
                }
            }
        }
    }

    override suspend fun updateUser(user: User): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE users SET name = ?, role = ? WHERE id = ?"
            ).use { ps ->
                ps.setString(1, user.name)
                ps.setString(2, user.role.name)
                ps.setLong(3, user.id)
                ps.executeUpdate()
            }
        }
        return user
    }

    override suspend fun validatePassword(email: String, password: String): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT password_hash FROM users WHERE email = ? LIMIT 1").use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) {
                        println("DEBUG: No user found with email $email")
                        return false
                    }
                    val stored = rs.getString("password_hash")
                    val candidate = com.example.taskgo.backend.util.PasswordUtil.hashPassword(password)
                    println("DEBUG: Password validation for $email")
                    println("DEBUG: Stored hash:      $stored")
                    println("DEBUG: Candidate hash:   $candidate")
                    println("DEBUG: Raw password:     $password")
                    println("DEBUG: Match:           ${stored == candidate}")
                    return stored == candidate
                }
            }
        }
    }

    override suspend fun listAll(): List<User> {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, email, name, role, created_at FROM users ORDER BY id DESC").use { ps ->
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<User>()
                    while (rs.next()) {
                        list += User(
                            id = rs.getLong("id"),
                            email = rs.getString("email"),
                            name = rs.getString("name"),
                            createdAt = rs.getTimestamp("created_at").time,
                            role = UserRole.valueOf(rs.getString("role"))
                        )
                    }
                    return list
                }
            }
        }
    }

    override suspend fun updateUserRole(userId: Long, newRole: UserRole): User? {
        dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE users SET role = ? WHERE id = ?").use { ps ->
                ps.setString(1, newRole.name)
                ps.setLong(2, userId)
                ps.executeUpdate()
            }
            conn.prepareStatement("SELECT id, email, name, role, created_at FROM users WHERE id = ?")
                .use { ps ->
                    ps.setLong(1, userId)
                    ps.executeQuery().use { rs ->
                        return if (rs.next()) {
                            User(
                                id = rs.getLong("id"),
                                email = rs.getString("email"),
                                name = rs.getString("name"),
                                createdAt = rs.getTimestamp("created_at").time,
                                role = UserRole.valueOf(rs.getString("role"))
                            )
                        } else null
                    }
                }
        }
    }

    override suspend fun updateUserDetails(userId: Long, details: UserDetailsUpdate): User? {
        dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE users SET name = ?, phone = ?, address = ?, documents = ?, profile_photo = ? WHERE id = ?").use { ps ->
                ps.setString(1, details.name)
                ps.setString(2, details.phone)
                ps.setString(3, details.address)
                ps.setString(4, details.documents?.joinToString(","))
                ps.setString(5, details.profilePhoto)
                ps.setLong(6, userId)
                ps.executeUpdate()
            }
            conn.prepareStatement("SELECT id, email, name, role, created_at FROM users WHERE id = ?")
                .use { ps ->
                    ps.setLong(1, userId)
                    ps.executeQuery().use { rs ->
                        return if (rs.next()) {
                            User(
                                id = rs.getLong("id"),
                                email = rs.getString("email"),
                                name = rs.getString("name"),
                                createdAt = rs.getTimestamp("created_at").time,
                                role = UserRole.valueOf(rs.getString("role"))
                            )
                        } else null
                    }
                }
        }
    }
}


