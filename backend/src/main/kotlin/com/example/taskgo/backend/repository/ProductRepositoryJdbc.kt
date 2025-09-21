package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.Product
import com.example.taskgo.backend.domain.ProductRepository
import com.example.taskgo.backend.domain.ProductCreate
import com.example.taskgo.backend.domain.ProductUpdate
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

    override suspend fun create(product: ProductCreate): Product {
        val sql = """
            INSERT INTO products (name, description, price, category, banner_url, active)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, name, description, price, category, banner_url, active
        """.trimIndent()
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, product.name)
                ps.setString(2, product.description)
                ps.setDouble(3, product.price)
                ps.setString(4, product.category)
                ps.setString(5, product.bannerUrl)
                ps.setBoolean(6, product.active)
                ps.executeQuery().use { rs ->
                    rs.next()
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
                }
            }
        }
    }

    override suspend fun update(id: Long, update: ProductUpdate): Product? {
        // construir update dinâmico
        val sets = mutableListOf<String>()
        val params = mutableListOf<Any?>()
        update.name?.let { sets += "name = ?"; params += it }
        update.description?.let { sets += "description = ?"; params += it }
        update.price?.let { sets += "price = ?"; params += it }
        update.category?.let { sets += "category = ?"; params += it }
        update.bannerUrl?.let { sets += "banner_url = ?"; params += it }
        update.active?.let { sets += "active = ?"; params += it }
        if (sets.isEmpty()) return getById(id)

        val sql = "UPDATE products SET ${sets.joinToString(", ")} WHERE id = ?"
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
        val sql = "DELETE FROM products WHERE id = ?"
        return dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate() > 0
            }
        }
    }
}


