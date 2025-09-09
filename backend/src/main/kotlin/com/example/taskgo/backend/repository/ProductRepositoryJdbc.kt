package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.Product
import com.example.taskgo.backend.domain.ProductRepository
import javax.sql.DataSource

class ProductRepositoryJdbc(private val dataSource: DataSource) : ProductRepository {
    override suspend fun list(search: String?, category: String?, page: Int, size: Int): List<Product> {
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
            SELECT id, name, description, price, category, banner_url, active
            FROM products
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
                    val list = mutableListOf<Product>()
                    while (rs.next()) {
                        list += Product(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            price = rs.getDouble("price"),
                            category = rs.getString("category"),
                            bannerUrl = rs.getString("banner_url"),
                            active = rs.getBoolean("active"),
                            seller = null
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun getById(id: Long): Product? {
        val sql = """
            SELECT id, name, description, price, category, banner_url, active
            FROM products
            WHERE id = ? AND active = TRUE
            LIMIT 1
        """.trimIndent()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        Product(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            price = rs.getDouble("price"),
                            category = rs.getString("category"),
                            bannerUrl = rs.getString("banner_url"),
                            active = rs.getBoolean("active"),
                            seller = null
                        )
                    } else null
                }
            }
        }
    }
}


