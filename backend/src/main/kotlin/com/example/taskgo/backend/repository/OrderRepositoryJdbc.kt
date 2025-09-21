package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.CartItem
import com.example.taskgo.backend.domain.Order
import com.example.taskgo.backend.domain.OrderItem
import com.example.taskgo.backend.domain.OrderRepository
import java.sql.Connection
import javax.sql.DataSource

class OrderRepositoryJdbc(private val dataSource: DataSource) : OrderRepository {

    override suspend fun createFromCart(
        userEmail: String,
        items: List<CartItem>,
        productsPricer: suspend (Long) -> Double
    ): Order {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                val orderId = insertOrder(conn, userEmail)
                var total = 0.0
                for (ci in items) {
                    val price = productsPricer(ci.productId)
                    total += price * ci.quantity
                    insertOrderItem(conn, orderId, ci.productId, ci.quantity, price)
                }
                updateOrderTotal(conn, orderId, total)
                conn.commit()
                return getOrder(conn, orderId)
            } catch (e: Exception) {
                runCatching { conn.rollback() }
                throw e
            } finally {
                runCatching { conn.autoCommit = true }
            }
        }
    }

    override suspend fun listByUser(userEmail: String): List<Order> {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id FROM orders WHERE user_email = ? ORDER BY id DESC").use { ps ->
                ps.setString(1, userEmail)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Order>()
                    while (rs.next()) list += getOrder(conn, rs.getLong("id"))
                    return list
                }
            }
        }
    }

    private fun insertOrder(conn: Connection, userEmail: String): Long {
        conn.prepareStatement("INSERT INTO orders (user_email, status) VALUES (?, 'CONFIRMED')", java.sql.Statement.RETURN_GENERATED_KEYS).use { ps ->
            ps.setString(1, userEmail)
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                if (rs.next()) return rs.getLong(1)
            }
        }
        error("Failed to insert order")
    }

    private fun insertOrderItem(conn: Connection, orderId: Long, productId: Long, quantity: Int, price: Double) {
        conn.prepareStatement("INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)").use { ps ->
            ps.setLong(1, orderId)
            ps.setLong(2, productId)
            ps.setInt(3, quantity)
            ps.setDouble(4, price)
            ps.executeUpdate()
        }
    }

    private fun updateOrderTotal(conn: Connection, orderId: Long, total: Double) {
        conn.prepareStatement("UPDATE orders SET total = ? WHERE id = ?").use { ps ->
            ps.setDouble(1, total)
            ps.setLong(2, orderId)
            ps.executeUpdate()
        }
    }

    private fun getOrder(conn: Connection, orderId: Long): Order {
        conn.prepareStatement("SELECT id, user_email, total, status, created_at FROM orders WHERE id = ?").use { ps ->
            ps.setLong(1, orderId)
            ps.executeQuery().use { rs ->
                if (!rs.next()) error("Order not found")
                val id = rs.getLong("id")
                val userEmail = rs.getString("user_email")
                val total = rs.getDouble("total")
                val status = rs.getString("status")
                val createdAt = rs.getString("created_at")
                val items = mutableListOf<OrderItem>()
                conn.prepareStatement("SELECT product_id, quantity, price FROM order_items WHERE order_id = ?").use { psi ->
                    psi.setLong(1, id)
                    psi.executeQuery().use { rsi ->
                        while (rsi.next()) {
                            items += OrderItem(
                                productId = rsi.getLong("product_id"),
                                quantity = rsi.getInt("quantity"),
                                price = rsi.getDouble("price")
                            )
                        }
                    }
                }
                return Order(id, userEmail, items, total, status, createdAt)
            }
        }
    }
}






