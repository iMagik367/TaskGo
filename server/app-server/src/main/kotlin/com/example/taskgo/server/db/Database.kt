package com.example.taskgo.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object Database {
    @Volatile private var dataSource: HikariDataSource? = null

    fun init(url: String?, user: String?, pass: String?) {
        if (dataSource != null) return
        val cfg = HikariConfig().apply {
            jdbcUrl = url ?: "jdbc:postgresql://localhost:5432/taskgo"
            username = user ?: "taskgo"
            password = pass ?: "taskgo"
            maximumPoolSize = 5
            initializationFailTimeout = -1
        }
        dataSource = HikariDataSource(cfg)
    }

    fun close() {
        dataSource?.close()
        dataSource = null
    }
}


