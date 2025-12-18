package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import kotlinx.coroutines.flow.Flow

interface CategoriesRepository {
    fun observeProductCategories(): Flow<List<String>>
    fun observeServiceCategories(): Flow<List<ServiceCategory>>
    suspend fun getProductCategories(): List<String>
    suspend fun getServiceCategories(): List<ServiceCategory>
}

