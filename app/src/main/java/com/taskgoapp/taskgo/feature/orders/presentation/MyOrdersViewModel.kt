package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.Order as SimpleOrder
import com.taskgoapp.taskgo.core.model.OrderItem
import com.taskgoapp.taskgo.core.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import java.time.Instant

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : ViewModel() {
    
    val orders: StateFlow<List<SimpleOrder>> = ordersRepository
        .observeOrders()
        .map { purchaseOrders ->
            purchaseOrders.map { it.toSimpleOrder() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    fun observeOrdersByStatus(status: String): StateFlow<List<SimpleOrder>> {
        val orderStatus = when (status) {
            "EM_ANDAMENTO" -> OrderStatus.EM_ANDAMENTO
            "CONCLUIDO" -> OrderStatus.CONCLUIDO
            "CANCELADO" -> OrderStatus.CANCELADO
            else -> null
        }
        
        return if (orderStatus != null) {
            ordersRepository
                .observeOrdersByStatus(orderStatus)
                .map { purchaseOrders ->
                    purchaseOrders.map { it.toSimpleOrder() }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        } else {
            orders
        }
    }
    
    private fun PurchaseOrder.toSimpleOrder(): SimpleOrder {
        return SimpleOrder(
            id = this.id.toLongOrNull() ?: 0L,
            items = this.items.map { OrderItem(it.productId, it.quantity, it.price) },
            total = this.total,
            status = this.status.name,
            createdAt = Instant.ofEpochMilli(this.createdAt)
        )
    }
}




