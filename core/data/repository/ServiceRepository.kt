package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ServiceRepository {
    fun getServiceCategories(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun getServiceProviders(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun getServiceProvidersByCategory(category: String): Flow<List<Any>> {
        return flowOf(emptyList())
    }
}

