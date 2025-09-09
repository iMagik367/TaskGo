package com.example.taskgo.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object Database {
    @Volatile
    private var dataSourceInternal: HikariDataSource? = null
    private val initLock = Any()

    val dataSource: DataSource
        get() = requireNotNull(dataSourceInternal) { "Database not initialized. Call Database.init() first." }

    fun init(): DataSource {
        return dataSourceInternal ?: synchronized(initLock) {
            dataSourceInternal ?: initializeDataSource()
        }
    }

    private fun initializeDataSource(): HikariDataSource {
        val enable = System.getenv("DB_ENABLE")?.equals("true", ignoreCase = true) == true
        if (!enable) {
            return HikariDataSource(HikariConfig()).also { dataSourceInternal = it }
        }

        val jdbcUrl = System.getenv("DB_URL") ?: error("DB_URL not set")
        val user = System.getenv("DB_USER") ?: error("DB_USER not set")
        val pass = System.getenv("DB_PASS") ?: error("DB_PASS not set")

        val config = HikariConfig().apply {
            jdbcUrl.let { this.jdbcUrl = it }
            username = user
            password = pass
            maximumPoolSize = (System.getenv("DB_POOL_MAX") ?: "10").toInt()
            minimumIdle = (System.getenv("DB_POOL_MIN") ?: "2").toInt()
            connectionTimeout = (System.getenv("DB_CONN_TIMEOUT_MS") ?: "10000").toLong()
            idleTimeout = (System.getenv("DB_IDLE_TIMEOUT_MS") ?: "600000").toLong()
            maxLifetime = (System.getenv("DB_MAX_LIFETIME_MS") ?: "1800000").toLong()
            initializationFailTimeout = -1
        }

        val ds = HikariDataSource(config)

        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        dataSourceInternal = ds
        return ds
    }

    fun shutdown() {
        dataSourceInternal?.close()
        dataSourceInternal = null
    }
}


