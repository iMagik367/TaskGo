plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    id("org.flywaydb.flyway") version "9.22.3"
}

// Repositórios herdados do settings.gradle.kts (dependencyResolutionManagement)

dependencies {
    val ktorVersion = "2.3.12"

    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    
    // Content Negotiation & Serialization
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    
    // CORS & Compression
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    
    // Logging & Auth
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    
    // Database
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.flywaydb:flyway-core:9.22.3")
    // Email
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

}

application {
    mainClass.set("com.example.taskgo.backend.MainKt")
}

kotlin {
    jvmToolchain(17)
}


// Desabilita tarefas de teste temporariamente (sem testes definidos)
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    enabled = false
}

val flywayProperties = mapOf(
    "flyway.url" to (System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://ep-bold-brook-ad1ppgew-pooler.c-2.us-east-1.aws.neon.tech/TaskGo"),
    "flyway.user" to (System.getenv("DB_USER") ?: "admin"),
    "flyway.password" to (System.getenv("DB_PASSWORD") ?: "kOgq9FYh61Bh"),
    "flyway.cleanDisabled" to "false"
)

// Task para recriar o banco
tasks.register("recreateDatabase") {
    group = "database"
    description = "Drops all tables and recreates database schema"
    
    doLast {
        tasks.getByName("flywayClean").setProperty("flywayProperties", flywayProperties)
        tasks.getByName("flywayMigrate").setProperty("flywayProperties", flywayProperties)
        tasks.getByName("flywayClean").actions.forEach { it.execute(this) }
        tasks.getByName("flywayMigrate").actions.forEach { it.execute(this) }
    }
}


