package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.Cart
import com.example.taskgo.backend.domain.CartItem
import com.example.taskgo.backend.domain.CartRepository
import java.sql.Connection
import javax.sql.DataSource

class CartRepositoryJdbc(private val dataSource: DataSource) : CartRepository {

    override suspend fun getCart(userEmail: String): Cart {
        return dataSource.connection.use { conn ->
            val items = selectItems(conn, userEmail)
            Cart(userEmail = userEmail, items = items)
        }
    }

    override suspend fun addItem(userEmail: String, productId: Long, quantity: Int): Cart {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                ensureCart(conn, userEmail)
                upsertItem(conn, userEmail, productId, quantity)
                conn.commit()
            } catch (e: Exception) {
                runCatching { conn.rollback() }
                throw e
            } finally {
                runCatching { conn.autoCommit = true }
            }
        }
        return getCart(userEmail)
    }

    override suspend fun removeItem(userEmail: String, productId: Long): Cart {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM cart_items WHERE user_email = ? AND product_id = ?").use { ps ->
                ps.setString(1, userEmail)
                ps.setLong(2, productId)
                ps.executeUpdate()
            }
        }
        return getCart(userEmail)
    }

    override suspend fun clearCart(userEmail: String): Cart {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM cart_items WHERE user_email = ?").use { ps ->
                ps.setString(1, userEmail)
                ps.executeUpdate()
            }
        }
        return getCart(userEmail)
    }

    private fun ensureCart(conn: Connection, userEmail: String) {
        conn.prepareStatement("INSERT OR IGNORE INTO carts (user_email) VALUES (?)").use { ps ->
            ps.setString(1, userEmail)
            ps.executeUpdate()
        }
    }

    private fun upsertItem(conn: Connection, userEmail: String, productId: Long, quantity: Int) {
        // If exists, increment; else insert
        conn.prepareStatement(
            "SELECT quantity FROM cart_items WHERE user_email = ? AND product_id = ?"
        ).use { sel ->
            sel.setString(1, userEmail)
            sel.setLong(2, productId)
            sel.executeQuery().use { rs ->
                if (rs.next()) {
                    val newQty = rs.getInt("quantity") + quantity
                    conn.prepareStatement(
                        "UPDATE cart_items SET quantity = ? WHERE user_email = ? AND product_id = ?"
                    ).use { upd ->
                        upd.setInt(1, newQty)
                        upd.setString(2, userEmail)
                        upd.setLong(3, productId)
                        upd.executeUpdate()
                    }
                } else {
                    conn.prepareStatement(
                        "INSERT INTO cart_items (user_email, product_id, quantity) VALUES (?, ?, ?)"
                    ).use { ins ->
                        ins.setString(1, userEmail)
                        ins.setLong(2, productId)
                        ins.setInt(3, quantity)
                        ins.executeUpdate()
                    }
                }
            }
        }
    }

    private fun selectItems(conn: Connection, userEmail: String): List<CartItem> {
        conn.prepareStatement(
            "SELECT product_id, quantity FROM cart_items WHERE user_email = ? ORDER BY rowid DESC"
        ).use { ps ->
            ps.setString(1, userEmail)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<CartItem>()
                while (rs.next()) {
                    list += CartItem(
                        productId = rs.getLong("product_id"),
                        quantity = rs.getInt("quantity")
                    )
                }
                return list
            }
        }
    }
}






