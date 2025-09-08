plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    application
}

dependencies {
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    // DB skeleton (Postgres + HikariCP)
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.22")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
kotlin { jvmToolchain(17) }

tasks.test {
    // Temporariamente desabilitado até adicionarmos testes JUnit
    enabled = false
}

// Desabilita task 'check' temporariamente
tasks.named("check") {
    enabled = false
}

application { mainClass.set("com.example.taskgo.server.MainKt") }

kotlin { jvmToolchain(17) }


