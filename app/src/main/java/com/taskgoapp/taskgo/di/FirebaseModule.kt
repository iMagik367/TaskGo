package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
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
        return if (BuildConfig.USE_EMULATOR) {
            // Emulator configuration
            FirebaseFirestore.getInstance().apply {
                useEmulator("10.0.2.2", 8080)
            }
        } else {
            // Production Firestore (persistence is enabled by default)
            FirebaseFirestore.getInstance()
        }
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
}





