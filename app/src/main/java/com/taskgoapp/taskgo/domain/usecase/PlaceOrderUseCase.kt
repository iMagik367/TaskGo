package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(
    private val productsRepo: ProductsRepository,
    private val ordersRepo: OrdersRepository,
    private val trackingRepo: TrackingRepository
) {
    suspend operator fun invoke(paymentMethod: String, addressId: String): String {
        val cart = productsRepo.observeCart().first()
        require(cart.isNotEmpty()) { "Carrinho vazio" }
        
        val total = cart.sumOf { item ->
            val product = productsRepo.getProduct(item.productId)
            (product?.price ?: 0.0) * item.qty
        }
        
        val orderId = ordersRepo.createOrder(cart, total, paymentMethod, addressId)
        trackingRepo.seedTimeline(orderId)
        
        return orderId
    }
}
