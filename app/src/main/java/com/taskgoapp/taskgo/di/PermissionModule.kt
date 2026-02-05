package com.taskgoapp.taskgo.di

import android.content.Context
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.permissions.PermissionManager
import com.taskgoapp.taskgo.core.photo.PhotoPickerManager
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
    
    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: Context,
        preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
    ): LocationManager {
        return LocationManager(context, preferencesManager)
    }
}
