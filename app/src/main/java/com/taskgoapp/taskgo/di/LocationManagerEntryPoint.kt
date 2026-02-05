package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.core.location.LocationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationManagerEntryPoint {
    fun locationManager(): LocationManager
}
