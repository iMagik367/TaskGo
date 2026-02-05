package com.taskgoapp.taskgo.di

import com.taskgoapp.taskgo.core.security.LGPDComplianceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LGPDEntryPoint {
    fun lgpdComplianceManager(): LGPDComplianceManager
}
