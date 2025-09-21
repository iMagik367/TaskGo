package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PaymentRepository {
    fun getPaymentMethods(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun addPaymentMethod(paymentMethod: Any) {
        // TODO: Adicionar o método de pagamento ao banco de dados
        // paymentMethod: objeto contendo informações do novo método de pagamento
    }
    
    fun removePaymentMethod(paymentMethodId: String) {
        // TODO: Remover o método de pagamento do banco de dados
        // paymentMethodId: identificador único do método de pagamento a ser removido
    }
}

