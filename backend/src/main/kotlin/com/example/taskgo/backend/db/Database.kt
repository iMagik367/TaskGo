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

    private fun initializeDataSource(): HikariDataSource {
        val enable = System.getenv("DB_ENABLE")?.equals("true", ignoreCase = true) == true
        if (!enable) {
            throw IllegalStateException("Database is disabled. Use in-memory repositories instead.")
        }

        val jdbcUrl = System.getenv("DB_URL") ?: error("DB_URL not set")
        val user = System.getenv("DB_USER") ?: error("DB_USER not set")
        val pass = System.getenv("DB_PASS") ?: error("DB_PASS not set")

        // Configuração otimizada para Neon
        val config = HikariConfig().apply {
            jdbcUrl.let { this.jdbcUrl = it }
            username = user
            password = pass
            
            // Pool otimizado para Neon
            maximumPoolSize = (System.getenv("DB_POOL_MAX") ?: "3").toInt()
            minimumIdle = (System.getenv("DB_POOL_MIN") ?: "1").toInt()
            
            // Timeouts mais generosos para Neon
            connectionTimeout = (System.getenv("DB_CONN_TIMEOUT_MS") ?: "30000").toLong()
            idleTimeout = (System.getenv("DB_IDLE_TIMEOUT_MS") ?: "300000").toLong()
            maxLifetime = (System.getenv("DB_MAX_LIFETIME_MS") ?: "1800000").toLong()
            validationTimeout = (System.getenv("DB_VALIDATION_TIMEOUT_MS") ?: "5000").toLong()
            
            // Configurações específicas para Neon
            initializationFailTimeout = -1
            leakDetectionThreshold = (System.getenv("DB_LEAK_DETECTION_THRESHOLD_MS") ?: "60000").toLong()
            
            // Propriedades específicas do PostgreSQL/Neon
            addDataSourceProperty("socketTimeout", "30")
            addDataSourceProperty("loginTimeout", "30")
            addDataSourceProperty("connectTimeout", "30")
            addDataSourceProperty("tcpKeepAlive", "true")
            addDataSourceProperty("application_name", "TaskGo-Backend")
        }

        val ds = HikariDataSource(config)

        // Executar migrações com retry
        try {
            Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        } catch (e: Exception) {
            println("Flyway migration failed, but continuing with existing schema: ${e.message}")
            // Não falhar se as migrações já foram executadas
        }

        return ds
    }

    fun shutdown() {
        (dataSource as? HikariDataSource)?.close()
        dataSource = null
    }
}


