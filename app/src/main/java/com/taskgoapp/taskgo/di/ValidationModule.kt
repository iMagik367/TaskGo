package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.core.validation.CepService
import com.taskgoapp.taskgo.core.validation.DocumentValidator
import com.taskgoapp.taskgo.core.validation.GovernmentDocumentValidator
import com.taskgoapp.taskgo.core.location.GeocodingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ValidationModule {

    @Provides
    @Singleton
    fun provideCepService(): CepService {
        return CepService()
    }

    @Provides
    @Singleton
    fun provideDocumentValidator(): DocumentValidator {
        return DocumentValidator()
    }
    
    @Provides
    @Singleton
    fun provideGovernmentDocumentValidator(): GovernmentDocumentValidator {
        return GovernmentDocumentValidator()
    }
    
    // GeocodingService usa @Inject constructor, não precisa de provider
}

