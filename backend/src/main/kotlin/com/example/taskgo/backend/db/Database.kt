package com.example.taskgo.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database {
    @Volatile
    private var dataSource: DataSource? = null

    fun init(): DataSource {
        return dataSource ?: synchronized(this) {
            dataSource ?: initializeDataSource().also { dataSource = it }
        }
    }

    private fun initializeDataSource(): DataSource {
        val enabled = System.getenv("DB_ENABLE")?.equals("true", ignoreCase = true) == true
        if (!enabled) {
            throw IllegalStateException("Database is disabled. Use in-memory repositories instead.")
        }

        val sqlitePath = System.getenv("DB_SQLITE_PATH")
        if (!sqlitePath.isNullOrBlank()) {
            return initializeSqlite(sqlitePath)
        }

        return initializePostgres()
    }

    private fun initializePostgres(): HikariDataSource {
        val jdbcUrl = System.getenv("DB_URL") ?: error("DB_URL not set")
        val user = System.getenv("DB_USER") ?: error("DB_USER not set")
        val pass = System.getenv("DB_PASS") ?: error("DB_PASS not set")

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = user
            password = pass
            maximumPoolSize = (System.getenv("DB_POOL_MAX") ?: "3").toInt()
            minimumIdle = (System.getenv("DB_POOL_MIN") ?: "1").toInt()
            connectionTimeout = (System.getenv("DB_CONN_TIMEOUT_MS") ?: "30000").toLong()
            idleTimeout = (System.getenv("DB_IDLE_TIMEOUT_MS") ?: "300000").toLong()
            maxLifetime = (System.getenv("DB_MAX_LIFETIME_MS") ?: "1800000").toLong()
            validationTimeout = (System.getenv("DB_VALIDATION_TIMEOUT_MS") ?: "5000").toLong()
            initializationFailTimeout = -1
            leakDetectionThreshold = (System.getenv("DB_LEAK_DETECTION_THRESHOLD_MS") ?: "60000").toLong()
            addDataSourceProperty("socketTimeout", "30")
            addDataSourceProperty("loginTimeout", "30")
            addDataSourceProperty("connectTimeout", "30")
            addDataSourceProperty("tcpKeepAlive", "true")
            addDataSourceProperty("application_name", "TaskGo-Backend")
        }

        val ds = HikariDataSource(config)
        try {
            Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        } catch (e: Exception) {
            println("Flyway migration failed, but continuing with existing schema: ${e.message}")
        }
        return ds
    }

    private fun initializeSqlite(sqlitePath: String): DataSource {
        val sqliteUrl = if (sqlitePath == ":memory:") "jdbc:sqlite::memory:" else "jdbc:sqlite:$sqlitePath"
        val ds = org.sqlite.SQLiteDataSource().apply { url = sqliteUrl }
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      email TEXT NOT NULL UNIQUE,
                      password_hash TEXT NOT NULL,
                      name TEXT,
                      role TEXT NOT NULL DEFAULT 'CUSTOMER',
                      created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    );
                    """.trimIndent()
                )
                stmt.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS products (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      name TEXT NOT NULL,
                      description TEXT NOT NULL,
                      price REAL NOT NULL,
                      category TEXT NOT NULL,
                      banner_url TEXT,
                      active INTEGER NOT NULL DEFAULT 1,
                      created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    );
                    """.trimIndent()
                )
                stmt.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS carts (
                      user_email TEXT PRIMARY KEY
                    );
                    """.trimIndent()
                )
                stmt.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS cart_items (
                      user_email TEXT NOT NULL,
                      product_id INTEGER NOT NULL,
                      quantity INTEGER NOT NULL,
                      PRIMARY KEY (user_email, product_id),
                      FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
                    );
                    """.trimIndent()
                )
                stmt.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS orders (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_email TEXT NOT NULL,
                      total REAL NOT NULL DEFAULT 0,
                      status TEXT NOT NULL DEFAULT 'CONFIRMED',
                      created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    );
                    """.trimIndent()
                )
                stmt.addBatch(
                    """
                    CREATE TABLE IF NOT EXISTS order_items (
                      order_id INTEGER NOT NULL,
                      product_id INTEGER NOT NULL,
                      quantity INTEGER NOT NULL,
                      price REAL NOT NULL,
                      FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
                    );
                    """.trimIndent()
                )
                stmt.executeBatch()
            }
            // Seed minimal products if table empty
            conn.prepareStatement("SELECT COUNT(1) as c FROM products").use { ps ->
                ps.executeQuery().use { rs ->
                    if (rs.next() && rs.getInt("c") == 0) {
                        conn.prepareStatement(
                            "INSERT INTO products (name, description, price, category) VALUES (?, ?, ?, ?)"
                        ).use { ins ->
                            fun add(n:String, d:String, p:Double, c:String) { ins.setString(1,n); ins.setString(2,d); ins.setDouble(3,p); ins.setString(4,c); ins.addBatch() }
                            add("Guarda Roupa 6 Portas", "Guarda roupa com espelho, MDF.", 899.90, "Móveis")
                            add("Furadeira sem fio 18V", "Com 2 baterias.", 299.90, "Ferramentas")
                            add("Forno de Embutir 30L", "Elétrico 30L, grill.", 599.90, "Eletrodomésticos")
                            add("Martelo 500g", "Cabo de madeira.", 45.90, "Ferramentas")
                            ins.executeBatch()
                        }
                    }
                }
            }
        }
        return ds
    }

    fun shutdown() {
        (dataSource as? HikariDataSource)?.close()
        dataSource = null
    }
}


