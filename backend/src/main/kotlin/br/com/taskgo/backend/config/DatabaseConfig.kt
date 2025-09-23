package br.com.taskgo.backend.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object DatabaseConfig {
    private val dbUrl = System.getenv("DATABASE_URL") ?: 
        "postgresql://neondb_owner:npg_68ytczWboCwF@ep-bold-brook-ad1ppgew-pooler.c-2.us-east-1.aws.neon.tech/TaskGo"
    private val dbUser = System.getenv("DATABASE_USER") ?: "neondb_owner"
    private val dbPassword = System.getenv("DATABASE_PASSWORD") ?: "npg_68ytczWboCwF"

    fun createDataSource(): DataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            
            // Configurações específicas para Neon
            addDataSourceProperty("socketTimeout", "30")
            addDataSourceProperty("tcpKeepAlive", "true")
            addDataSourceProperty("ssl", "true")
            addDataSourceProperty("sslmode", "require")
            addDataSourceProperty("channel_binding", "require")
        }

        return HikariDataSource(config)
    }

    fun migrateDatabase(dataSource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .load()

        flyway.migrate()
    }
}