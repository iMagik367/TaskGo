package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.database.FirebaseDatabase
import com.taskgoapp.taskgo.core.firebase.FirestoreHelper
import com.taskgoapp.taskgo.core.location.LocationStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return if (BuildConfig.USE_EMULATOR) {
            // Emulator configuration
            FirebaseAuth.getInstance().apply {
                useEmulator("10.0.2.2", 9099)
            }
        } else {
            FirebaseAuth.getInstance()
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = if (BuildConfig.USE_EMULATOR) {
            // Emulator configuration
            FirebaseFirestore.getInstance().apply {
                useEmulator("10.0.2.2", 8080)
            }
        } else {
            // Production Firestore - usar database padrao '(default)'
            FirestoreHelper.getInstance()
        }
        
        // Configurar para melhor performance - cache offline e leitura rápida
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Habilitar cache offline
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Cache ilimitado
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            // Se já foi configurado, ignora o erro
            android.util.Log.w("FirebaseModule", "Firestore settings já configurados: ${e.message}")
        }
        
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return if (BuildConfig.USE_EMULATOR) {
            // Emulator configuration
            FirebaseFunctions.getInstance().apply {
                useEmulator("10.0.2.2", 5001)
            }
        } else {
            FirebaseFunctions.getInstance(
                FirebaseApp.getInstance(),
                BuildConfig.FIREBASE_FUNCTIONS_REGION
            )
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return if (BuildConfig.USE_EMULATOR) {
            // Emulator configuration
            FirebaseStorage.getInstance().apply {
                useEmulator("10.0.2.2", 9199)
            }
        } else {
            FirebaseStorage.getInstance()
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        val database = if (BuildConfig.USE_EMULATOR) {
            // Emulator configuration
            FirebaseDatabase.getInstance().apply {
                useEmulator("10.0.2.2", 9000)
            }
        } else {
            // Production - URL do Realtime Database
            FirebaseDatabase.getInstance("https://task-go-ee85f-default-rtdb.firebaseio.com/")
        }
        
        // Habilitar persistência offline
        database.setPersistenceEnabled(true)
        
        return database
    }
    
    @Provides
    @Singleton
    fun provideLocationStateManager(
        userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository
    ): LocationStateManager {
        return LocationStateManager(userRepository)
    }
}





