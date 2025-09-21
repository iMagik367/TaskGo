package com.example.taskgoapp.domain.repository

import com.example.taskgoapp.core.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
}




