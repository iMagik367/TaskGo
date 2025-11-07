package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.work.WorkManagerHelper
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckoutUseCase @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val ordersRepository: OrdersRepository,
    private val trackingRepository: TrackingRepository,
    private val workManagerHelper: WorkManagerHelper
) {
    
    suspend operator fun invoke(
        paymentMethod: String,
        addressId: String
    ): Result<String> {
        return try {
            // Get current cart
            val cart = productsRepository.observeCart().first()
            if (cart.isEmpty()) {
                return Result.failure(Exception("Carrinho vazio"))
            }
            
            // Calculate total
            val total = calculateTotal(cart)
            
            // Create order
            val orderId = ordersRepository.createOrder(cart, total, paymentMethod, addressId)
            
            // Seed tracking timeline
            trackingRepository.seedTimeline(orderId)
            
            // Schedule order tracking notifications
            workManagerHelper.scheduleOrderTracking(orderId)
            
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun calculateTotal(cart: List<CartItem>): Double {
        var total = 0.0
        for (item in cart) {
            val product = productsRepository.getProduct(item.productId)
            if (product != null) {
                total += product.price * item.qty
            }
        }
        return total
    }
    
    suspend fun getOrderSummary(cart: List<CartItem>): OrderSummary {
        val subtotal = calculateTotal(cart)
        val shipping = if (subtotal > 100.0) 0.0 else 15.0 // Free shipping over R$ 100
        val total = subtotal + shipping
        
        return OrderSummary(
            subtotal = subtotal,
            shipping = shipping,
            total = total,
            itemCount = cart.sumOf { it.qty }
        )
    }
}

data class OrderSummary(
    val subtotal: Double,
    val shipping: Double,
    val total: Double,
    val itemCount: Int
)
