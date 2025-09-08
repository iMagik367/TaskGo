package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UserRepository {
    fun getCurrentUser(): Flow<Any?> {
        return flowOf(null)
    }
    
    fun updateUserProfile(user: Any) {
        // Em uma implementação real, isso seria salvo no banco de dados
        // Por enquanto, apenas simulamos a atualização
    }
}

