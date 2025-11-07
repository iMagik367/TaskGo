package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
}




