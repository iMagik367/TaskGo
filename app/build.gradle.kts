import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
    id("com.google.gms.google-services")
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
val apiBaseUrl = (localProps.getProperty("apiBaseUrl") ?: "http://10.0.2.2:8091/v1/").trim()

// Load keystore.properties for release signing
// Descomente estas linhas após criar o keystore e o arquivo keystore.properties
/*
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
*/

android {
    namespace = "com.taskgoapp.taskgo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.taskgoapp.taskgo"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("boolean", "USE_FIREBASE", "true")
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl}\"")
        buildConfigField("boolean", "USE_REMOTE_API", "true")
        buildConfigField("boolean", "USE_EMULATOR", "false") // Set to true to use Firebase Emulator
        buildConfigField("String", "FIREBASE_FUNCTIONS_REGION", "\"us-central1\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8091/v1/\"")
            buildConfigField("boolean", "USE_EMULATOR", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "API_BASE_URL", "\"https://api.taskgo.com/v1/\"")
            buildConfigField("boolean", "USE_EMULATOR", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Descomente a linha abaixo após configurar o signingConfigs
            // signingConfig = signingConfigs.getByName("release")
        }
    }
    
    // Signing configs - será configurado após criar keystore
    // Descomente o bloco abaixo após criar o keystore e o arquivo keystore.properties
    signingConfigs {
        /*
        create("release") {
            keyAlias = keystoreProperties["TASKGO_RELEASE_KEY_ALIAS"] as String
            keyPassword = keystoreProperties["TASKGO_RELEASE_KEY_PASSWORD"] as String
            storeFile = file(keystoreProperties["TASKGO_RELEASE_STORE_FILE"] as String)
            storePassword = keystoreProperties["TASKGO_RELEASE_STORE_PASSWORD"] as String
        }
        */
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

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
    }
}

kotlin {
    jvmToolchain(17)
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
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
    implementation(libs.okhttp.logging)
    
    // Image loading
    implementation(libs.coil.compose)
    
    // Image editing and cropping
    implementation("com.github.CanHub:Android-Image-Cropper:4.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    
    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-appcheck-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Google Play Billing
    implementation("com.android.billingclient:billing:6.1.0")
    implementation("com.android.billingclient:billing-ktx:6.1.0")
    
    // Google Pay
    implementation("com.google.android.gms:play-services-wallet:19.2.0")
    
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
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