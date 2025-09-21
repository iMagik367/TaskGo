package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.Service
import com.example.taskgo.backend.domain.ServiceCreate
import com.example.taskgo.backend.domain.ServiceRepository
import com.example.taskgo.backend.domain.ServiceUpdate
import javax.sql.DataSource

class ServiceRepositoryJdbc(private val dataSource: DataSource) : ServiceRepository {
    override suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Service> {
        val params = mutableListOf<Any>()
        val where = buildString {
            val parts = mutableListOf<String>()
            if (!search.isNullOrBlank()) {
                parts += "(LOWER(name) LIKE ? OR LOWER(description) LIKE ?)"
                val like = "%${search.lowercase()}%"
                params += like
                params += like
            }
            if (!category.isNullOrBlank()) {
                parts += "LOWER(category) = ?"
                params += category.lowercase()
            }
            parts += "active = TRUE"
            if (parts.isNotEmpty()) append(" WHERE ").append(parts.joinToString(" AND "))
        }
        val offset = (page - 1).coerceAtLeast(0) * size
        val sql = """
            SELECT id, name, description, price, category, active
            FROM services
            $where
            ORDER BY id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                var idx = 1
                params.forEach { ps.setObject(idx++, it) }
                ps.setInt(idx++, size)
                ps.setInt(idx, offset)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Service>()
                    while (rs.next()) {
                        list += Service(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            price = rs.getDouble("price"),
                            category = rs.getString("category"),
                            active = rs.getBoolean("active"),
                            provider = null
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun getById(id: Long): Service? {
        val sql = """
            SELECT id, name, description, price, category, active
            FROM services
            WHERE id = ? AND active = TRUE
            LIMIT 1
        """.trimIndent()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        Service(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            price = rs.getDouble("price"),
                            category = rs.getString("category"),
                            active = rs.getBoolean("active"),
                            provider = null
                        )
                    } else null
                }
            }
        }
    }

    override suspend fun create(service: ServiceCreate): Service {
        val sql = """
            INSERT INTO services (name, description, price, category, active)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, name, description, price, category, active
        """.trimIndent()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, service.name)
                ps.setString(2, service.description)
                ps.setDouble(3, service.price)
                ps.setString(4, service.category)
                ps.setBoolean(5, service.active)
                ps.executeQuery().use { rs ->
                    rs.next()
                    Service(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        description = rs.getString("description"),
                        price = rs.getDouble("price"),
                        category = rs.getString("category"),
                        active = rs.getBoolean("active"),
                        provider = null
                    )
                }
            }
        }
    }

    override suspend fun update(id: Long, update: ServiceUpdate): Service? {
        val sets = mutableListOf<String>()
        val params = mutableListOf<Any?>()
        update.name?.let { sets += "name = ?"; params += it }
        update.description?.let { sets += "description = ?"; params += it }
        update.price?.let { sets += "price = ?"; params += it }
        update.category?.let { sets += "category = ?"; params += it }
        update.active?.let { sets += "active = ?"; params += it }
        if (sets.isEmpty()) return getById(id)
        val sql = "UPDATE services SET ${sets.joinToString(", ")} WHERE id = ?"
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                var idx = 1
                params.forEach { ps.setObject(idx++, it) }
                ps.setLong(idx, id)
                ps.executeUpdate()
            }
        }
        return getById(id)
    }

    override suspend fun delete(id: Long): Boolean {
        val sql = "DELETE FROM services WHERE id = ?"
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate() > 0
            }
        }
    }
}






