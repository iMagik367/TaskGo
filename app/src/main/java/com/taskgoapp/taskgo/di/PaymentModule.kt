package com.taskgoapp.taskgo.di

import android.content.Context
import com.taskgoapp.taskgo.core.payment.GooglePayManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

    @Provides
    @Singleton
    fun provideGooglePayManager(@ApplicationContext context: Context): GooglePayManager {
        return GooglePayManager(context)
    }
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}

