package br.com.taskgo.taskgo.data.repository

import com.example.taskgoapp.core.data.remote.OrdersApi
import com.example.taskgoapp.domain.repository.OrderRepository
import com.example.taskgoapp.core.model.Order
import com.example.taskgoapp.core.model.OrderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val ordersApi: OrdersApi
) : OrderRepository {

    override fun observeOrders(): Flow<List<Order>> = flow {
        val res = ordersApi.list()
        emit(res.items.map { dto ->
            Order(
                id = dto.id,
                items = dto.items.map { item -> 
                    OrderItem(
                        productId = item.productId.toString(), 
                        quantity = item.quantity, 
                        price = item.price
                    ) 
                },
                total = dto.total,
                status = dto.status,
                createdAt = dto.createdAt
            )
        })
    }
}


