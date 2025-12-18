package com.taskgoapp.taskgo.di

import android.content.Context
import com.taskgoapp.taskgo.core.ai.GoogleCloudAIService
import com.taskgoapp.taskgo.core.ai.GoogleSpeechToTextService
import com.taskgoapp.taskgo.core.ai.GoogleTranslationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    // API Key do Google Cloud
    private const val GOOGLE_CLOUD_API_KEY = "AIzaSyCG9r2ruOBuTPfBQcaBwKaR3ODWMunaYR4"
    
    @Provides
    @Singleton
    @Named("GoogleCloudAPIKey")
    fun provideGoogleCloudAPIKey(): String {
        return GOOGLE_CLOUD_API_KEY
    }
    
    @Provides
    @Singleton
    fun provideGoogleCloudAIService(
        @Named("GoogleCloudAPIKey") apiKey: String
    ): GoogleCloudAIService {
        return GoogleCloudAIService(apiKey)
    }
    
    @Provides
    @Singleton
    fun provideGoogleTranslationService(
        @Named("GoogleCloudAPIKey") apiKey: String
    ): GoogleTranslationService {
        return GoogleTranslationService(apiKey)
    }
    
    @Provides
    @Singleton
    fun provideGoogleSpeechToTextService(
        @Named("GoogleCloudAPIKey") apiKey: String,
        @ApplicationContext context: Context
    ): GoogleSpeechToTextService {
        return GoogleSpeechToTextService(apiKey, context)
    }
}


