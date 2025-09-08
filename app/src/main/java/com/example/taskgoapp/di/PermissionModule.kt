package com.example.taskgoapp.di

import android.content.Context
import com.example.taskgoapp.core.permissions.PermissionManager
import com.example.taskgoapp.core.photo.PhotoPickerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun providePhotoPickerManager(@ApplicationContext context: Context): PhotoPickerManager {
        return PhotoPickerManager(context)
    }
}
