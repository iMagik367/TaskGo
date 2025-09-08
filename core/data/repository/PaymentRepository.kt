package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PaymentRepository {
    fun getPaymentMethods(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun addPaymentMethod(paymentMethod: Any) {
        // Em uma implementação real, isso seria salvo no banco de dados
        // Por enquanto, apenas simulamos a adição
    }
    
    fun removePaymentMethod(paymentMethodId: String) {
        // Em uma implementação real, isso seria removido do banco de dados
        // Por enquanto, apenas simulamos a remoção
    }
}

