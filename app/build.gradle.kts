import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "3.0.2"
}

// Load apiBaseUrl from local.properties for physical devices; fallback to 10.0.2.2
val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropsFile.exists()) {
        try {
            localPropsFile.inputStream().use { load(it) }
        } catch (e: Exception) {
            // Ignore errors reading local.properties
        }
    }
}
// API Base URL - pode ser configurada via local.properties
// Para produção, será substituída pela URL do Railway
val apiBaseUrl = (localProps.getProperty("apiBaseUrl") ?: "http://10.0.2.2:3000/api").trim()
val railwayApiUrl = (localProps.getProperty("railwayApiUrl") ?: "").trim()
val defaultDebugAppCheckTokenName = "TaskGo"
val defaultDebugAppCheckToken = "4D4F1322-E272-454F-9396-ED80E3DBDBD7"

val firebaseDebugAppCheckTokenName =
    (localProps.getProperty("firebaseDebugAppCheckTokenName") ?: defaultDebugAppCheckTokenName).trim()
val firebaseDebugAppCheckToken =
    (localProps.getProperty("firebaseDebugAppCheckToken") ?: defaultDebugAppCheckToken).trim()
val escapedFirebaseDebugAppCheckToken =
    firebaseDebugAppCheckToken.replace("\\", "\\\\").replace("\"", "\\\"")
val escapedFirebaseDebugAppCheckTokenName =
    firebaseDebugAppCheckTokenName.replace("\\", "\\\\").replace("\"", "\\\"")

val enableAppCheckProp = localProps.getProperty("enableAppCheck") ?: "true"
val enableAppCheck = enableAppCheckProp.trim().lowercase() != "false"

val useFirebaseEmulatorProp = localProps.getProperty("useFirebaseEmulator") ?: "false"
val useFirebaseEmulator = useFirebaseEmulatorProp.trim().lowercase() == "true"

// Load keystore.properties for release signing
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    try {
        // Carregar usando FileInputStream e remover BOM se existir
        FileInputStream(keystorePropertiesFile).use { stream ->
            keystoreProperties.load(stream)
        }
        // Remover BOM de todas as chaves (se houver)
        val keysToFix = keystoreProperties.keys.toList()
        keysToFix.forEach { key ->
            val cleanKey = key.toString().replace("\uFEFF", "").trim() // Remove BOM
            if (cleanKey != key.toString()) {
                val value = keystoreProperties.remove(key)
                if (value != null) {
                    keystoreProperties.setProperty(cleanKey, value.toString().trim())
                }
            } else {
                val value = keystoreProperties.getProperty(key.toString())
                if (value != null) {
                    keystoreProperties.setProperty(cleanKey, value.trim())
                }
            }
        }
        println("keystore.properties carregado com sucesso")
        println("Propriedades encontradas: ${keystoreProperties.keys.size}")
        keystoreProperties.keys.forEach { key ->
            println("  - '$key'")
        }
    } catch (e: Exception) {
        println("ERRO ao carregar keystore.properties: ${e.message}")
        e.printStackTrace()
    }
} else {
    println("WARNING: keystore.properties não encontrado em: ${keystorePropertiesFile.absolutePath}")
}

android {
    namespace = "com.taskgoapp.taskgo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.taskgoapp.taskgo"
        minSdk = 24
        targetSdk = 35
        versionCode = 144
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("boolean", "USE_FIREBASE", "true")
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl}\"")
        buildConfigField("boolean", "USE_REMOTE_API", "true")
        buildConfigField("boolean", "USE_EMULATOR", useFirebaseEmulator.toString())
        buildConfigField("String", "FIREBASE_FUNCTIONS_REGION", "\"us-central1\"")
        buildConfigField(
            "String",
            "FIREBASE_DEBUG_APP_CHECK_TOKEN",
            "\"${escapedFirebaseDebugAppCheckToken}\""
        )
        buildConfigField(
            "String",
            "FIREBASE_DEBUG_APP_CHECK_TOKEN_NAME",
            "\"${escapedFirebaseDebugAppCheckTokenName}\""
        )
        buildConfigField(
            "boolean",
            "FIREBASE_APP_CHECK_ENABLED",
            enableAppCheck.toString()
        )
    }
    
    // Signing configs - deve estar antes de buildTypes
    signingConfigs {
        if (keystorePropertiesFile.exists() && keystoreProperties.isNotEmpty()) {
            println("=== CONFIGURANDO SIGNING ===")
            // Usar getProperty() em vez de [] para garantir leitura correta
            val keyAlias = keystoreProperties.getProperty("TASKGO_RELEASE_KEY_ALIAS")?.trim()
            val keyPassword = keystoreProperties.getProperty("TASKGO_RELEASE_KEY_PASSWORD")?.trim()
            val storeFile = keystoreProperties.getProperty("TASKGO_RELEASE_STORE_FILE")?.trim()
            val storePassword = keystoreProperties.getProperty("TASKGO_RELEASE_STORE_PASSWORD")?.trim()
            
            println("keyAlias: ${keyAlias ?: "NULL"} (${keyAlias != null})")
            println("keyPassword: ${if (keyPassword != null) "***" else "NULL"} (${keyPassword != null})")
            println("storeFile: $storeFile (${storeFile != null})")
            println("storePassword: ${if (storePassword != null) "***" else "NULL"} (${storePassword != null})")
            
            if (keyAlias != null && keyPassword != null && storeFile != null && storePassword != null) {
                // Usar caminho absoluto diretamente - Gradle aceita tanto / quanto \
                val keystoreFile = file(storeFile)
                println("Caminho do keystore: ${keystoreFile.absolutePath}")
                println("Keystore existe: ${keystoreFile.exists()}")
                
                // Verificar se o arquivo existe antes de criar o signingConfig
                if (keystoreFile.exists()) {
                create("release") {
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                        this.storeFile = keystoreFile
                    this.storePassword = storePassword
                }
                    println("✅ SigningConfig 'release' criado com sucesso!")
                    println("=== SIGNING CONFIGURADO ===")
                } else {
                    println("❌ ERRO: Keystore file não encontrado em: ${keystoreFile.absolutePath}")
                }
            } else {
                println("❌ ERRO: Propriedades faltando:")
                if (keyAlias == null) println("  - TASKGO_RELEASE_KEY_ALIAS")
                if (keyPassword == null) println("  - TASKGO_RELEASE_KEY_PASSWORD")
                if (storeFile == null) println("  - TASKGO_RELEASE_STORE_FILE")
                if (storePassword == null) println("  - TASKGO_RELEASE_STORE_PASSWORD")
            }
        } else {
            println("❌ ERRO: keystore.properties não encontrado ou vazio")
        }
    }
    
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8091/v1/\"")
            buildConfigField("boolean", "USE_EMULATOR", useFirebaseEmulator.toString())
            buildConfigField(
                "String",
                "FIREBASE_DEBUG_APP_CHECK_TOKEN",
                "\"${escapedFirebaseDebugAppCheckToken}\""
            )
            buildConfigField(
                "String",
                "FIREBASE_DEBUG_APP_CHECK_TOKEN_NAME",
                "\"${escapedFirebaseDebugAppCheckTokenName}\""
            )
            buildConfigField(
                "boolean",
                "FIREBASE_APP_CHECK_ENABLED",
                enableAppCheck.toString()
            )
        }
        release {
            // Minificação desabilitada temporariamente devido a crash do R8/daemon
            // AAB gerado com sucesso sem minificação (55.44 MB)
            // TODO: Investigar crash do R8 e reabilitar minificação em versão futura
            isMinifyEnabled = false
            isShrinkResources = false
            // API URL - Railway Backend
            val releaseApiUrl = if (railwayApiUrl.isNotEmpty()) railwayApiUrl else "https://taskgo-production.up.railway.app/api"
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
            buildConfigField("boolean", "USE_EMULATOR", "false")
            buildConfigField(
                "String",
                "FIREBASE_DEBUG_APP_CHECK_TOKEN",
                "\"${escapedFirebaseDebugAppCheckToken}\""
            )
            buildConfigField(
                "String",
                "FIREBASE_DEBUG_APP_CHECK_TOKEN_NAME",
                "\"${escapedFirebaseDebugAppCheckTokenName}\""
            )
            buildConfigField(
                "boolean",
                "FIREBASE_APP_CHECK_ENABLED",
                enableAppCheck.toString()
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            // Configurações específicas do R8 para evitar ClassCastException, EXCEPTION_ACCESS_VIOLATION e ArrayIndexOutOfBoundsException
            // O erro ClassCastException LinkedList to z2 é um bug conhecido do R8 em AGP 8.12.3
            // EXCEPTION_ACCESS_VIOLATION e ArrayIndexOutOfBoundsException podem ocorrer durante minificação com otimizações agressivas
            // Solução: Usar configurações que evitam otimizações problemáticas
            multiDexEnabled = true
            // Aplicar signing config se existir
            signingConfigs.findByName("release")?.let {
                signingConfig = it
                println("SigningConfig 'release' aplicado ao buildType release")
            } ?: run {
                println("WARNING: SigningConfig 'release' não encontrado - AAB não será assinado!")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Temporariamente desabilitado para corrigir erro D8BackportedMethodsGenerator
        // Reabilitar após sincronização bem-sucedida se necessário para suporte a APIs antigas
        isCoreLibraryDesugaringEnabled = false
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        jniLibs {
            // Use o empacotamento moderno (recomendado para compatibilidade futura)
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
    
    // Configuração de Bundle para garantir compatibilidade com Play Store
    // CRÍTICO: enableSplit = false garante que todos os recursos estejam no base APK
    // Isso permite que usuários de versões anteriores possam atualizar
    bundle {
        language {
            // Desabilitar splits de idioma - incluir tudo no base APK
            // Necessário para permitir atualização de usuários existentes
            enableSplit = false
        }
        density {
            // Desabilitar splits de densidade - incluir tudo no base APK
            // Necessário para permitir atualização de usuários existentes
            enableSplit = false
        }
        abi {
            // Habilitar splits de ABI (normal para otimização)
            enableSplit = true
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
    }
    
    lint {
        disable += "RemoveWorkManagerInitializer"
        checkReleaseBuilds = false
        abortOnError = false
    }
    
    // NOTA: Configurações do R8 via System.setProperty foram removidas
    // A correção para ClassCastException do R8 é feita através de ProGuard rules (app/proguard-rules.pro)
    // As regras específicas para evitar o bug LinkedList to z2 já estão configuradas no proguard-rules.pro
}

kotlin {
    jvmToolchain(17)
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
    javacOptions {
        option("-source", "17")
        option("-target", "17")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.transport.api)
    kapt(libs.androidx.room.compiler)
    
    // Hilt dependencies
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Hilt WorkManager
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    
    implementation(libs.gson)
    implementation(libs.serialization.json)
    implementation(libs.androidx.activity.result)
    
    // Network dependencies
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    
    // ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation(libs.okhttp.logging)
    
    // Image loading
    implementation(libs.coil.compose)
    
    // Image editing and cropping
    implementation("com.github.CanHub:Android-Image-Cropper:4.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    
    // CameraX
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")
    
    // Guava for ListenableFuture support
    implementation("com.google.guava:guava:31.1-android")
    
    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.5") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    }
    
    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-appcheck-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // QR Code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Stripe Payment Sheet
    implementation("com.stripe:stripe-android:20.37.1") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    }
    
    // Google Tink (necessário para Nimbus JOSE usado pelo Stripe)
    // Atualizado para versão mais recente para evitar bug no R8
    implementation("com.google.crypto.tink:tink-android:1.14.1")
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.maps.android:maps-compose-utils:4.3.0")
    implementation("com.google.maps.android:maps-compose-widgets:4.3.0")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Google Play Billing (versão 6.1.0 - compatível com Kotlin 1.9)
    implementation("com.android.billingclient:billing:6.1.0")
    implementation("com.android.billingclient:billing-ktx:6.1.0")
    
    // Google Pay
    implementation("com.google.android.gms:play-services-wallet:19.2.0")
    
    // ML Kit Face Detection para validação facial
    implementation("com.google.mlkit:face-detection:16.1.7")
    
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // ExifInterface para leitura de metadados de imagens
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    
    // Temporariamente comentado para corrigir erro D8BackportedMethodsGenerator
    // Reabilitar após sincronização bem-sucedida se necessário
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    
    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Alias para ambientes/IDE que ainda invocam :app:testClasses
tasks.register("testClasses") {
    dependsOn("testDebugUnitTest")
}

// Nota: Símbolos de depuração nativos
// O Android Gradle Plugin extrai automaticamente os símbolos durante o bundleRelease
// Se necessário, execute o script: .\gerar-simbolos.ps1