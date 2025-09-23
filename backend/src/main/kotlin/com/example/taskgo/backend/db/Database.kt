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
        val jdbcUrl = System.getenv("JDBC_DATABASE_URL") ?: 
            "jdbc:postgresql://ep-bold-brook-ad1ppgew-pooler.c-2.us-east-1.aws.neon.tech/TaskGo?sslmode=require"
        val user = System.getenv("DB_USER") ?: "admin"
        val pass = System.getenv("DB_PASSWORD") ?: "kOgq9FYh61Bh"

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = user
            password = pass
            maximumPoolSize = 2
            minimumIdle = 1
            connectionTimeout = 60000 // 60 segundos
            idleTimeout = 600000 // 10 minutos
            maxLifetime = 1800000 // 30 minutos
            validationTimeout = 10000 // 10 segundos
            initializationFailTimeout = -1
            leakDetectionThreshold = (System.getenv("DB_LEAK_DETECTION_THRESHOLD_MS") ?: "60000").toLong()
            addDataSourceProperty("socketTimeout", "30")
            addDataSourceProperty("loginTimeout", "30")
            addDataSourceProperty("connectTimeout", "30")
            addDataSourceProperty("tcpKeepAlive", "true")
            addDataSourceProperty("application_name", "TaskGo-Backend")
        }

        val ds = HikariDataSource(config)
        // Executar migrations manualmente para debug
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                // Dropar schema se necessário
                if (System.getenv("DB_RECREATE")?.equals("true", ignoreCase = true) == true) {
                    println("🗑️ Dropping existing tables...")
                    try {
                        stmt.execute("DROP TABLE IF EXISTS reviews, tasks, services, users CASCADE")
                    } catch (e: Exception) {
                        println("Warning: Failed to drop tables: ${e.message}")
                    }
                }

                println("🔨 Creating tables...")
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGSERIAL PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        name VARCHAR(255),
                        role TEXT NOT NULL DEFAULT 'USER',
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                    )
                """)
            }
        }
        return ds
    }

    private fun initializeSqlite(sqlitePath: String): DataSource {
        // Usamos HikariCP com SQLite
        val config = HikariConfig().apply {
            jdbcUrl = if (sqlitePath == ":memory:") "jdbc:sqlite::memory:" else "jdbc:sqlite:$sqlitePath"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 1 // SQLite suporta apenas uma conexão por vez
            minimumIdle = 1
            connectionTimeout = 30000
            idleTimeout = 300000
            maxLifetime = 1800000
            validationTimeout = 5000
        }
        
        val ds = HikariDataSource(config)
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      email TEXT NOT NULL UNIQUE,
                      password_hash TEXT NOT NULL,
                      name TEXT,
                      role TEXT NOT NULL DEFAULT 'CUSTOMER',
                      created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    );
                """.trimIndent())
                
                stmt.execute("""
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
                """.trimIndent())
                
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS carts (
                      user_email TEXT PRIMARY KEY
                    );
                """.trimIndent())
                
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS cart_items (
                      user_email TEXT NOT NULL,
                      product_id INTEGER NOT NULL,
                      quantity INTEGER NOT NULL,
                      PRIMARY KEY (user_email, product_id),
                      FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_email TEXT NOT NULL,
                      total REAL NOT NULL DEFAULT 0,
                      status TEXT NOT NULL DEFAULT 'CONFIRMED',
                      created_at TEXT NOT NULL DEFAULT (datetime('now'))
                    );
                """.trimIndent())
                
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS order_items (
                      order_id INTEGER NOT NULL,
                      product_id INTEGER NOT NULL,
                      quantity INTEGER NOT NULL,
                      price REAL NOT NULL,
                      FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
                    );
                """.trimIndent())
            }
            
            // Seed minimal products if table empty
            conn.prepareStatement("SELECT COUNT(1) as c FROM products").use { ps ->
                ps.executeQuery().use { rs ->
                    if (rs.next() && rs.getInt("c") == 0) {
                        conn.prepareStatement(
                            "INSERT INTO products (name, description, price, category) VALUES (?, ?, ?, ?)"
                        ).use { ins ->
                            fun addProduct(name: String, description: String, price: Double, category: String) {
                                ins.setString(1, name)
                                ins.setString(2, description)
                                ins.setDouble(3, price)
                                ins.setString(4, category)
                                ins.executeUpdate()
                            }
                            
                            addProduct("Guarda Roupa 6 Portas", "Guarda roupa com espelho, MDF.", 899.90, "Móveis")
                            addProduct("Furadeira sem fio 18V", "Com 2 baterias.", 299.90, "Ferramentas")
                            addProduct("Forno de Embutir 30L", "Elétrico 30L, grill.", 599.90, "Eletrodomésticos")
                            addProduct("Martelo 500g", "Cabo de madeira.", 45.90, "Ferramentas")
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


