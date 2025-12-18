package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

/**
 * Pagamento de produto com split de comissão
 */
data class ProductPayment(
    val id: String = "",
    val orderId: String = "", // ID do pedido de compra
    val clientId: String = "", // ID do cliente que comprou
    val sellerId: String = "", // ID do vendedor
    val storeId: String? = null, // ID da loja (se aplicável)
    val totalAmount: Double = 0.0, // Valor total pago pelo cliente
    val sellerAmount: Double = 0.0, // Valor que o vendedor receberá (98%)
    val platformFee: Double = 0.0, // Comissão da plataforma (2%)
    val currency: String = "BRL",
    val paymentMethod: String = "", // "PIX", "CREDIT_CARD", "DEBIT_CARD"
    val stripePaymentIntentId: String? = null, // ID do PaymentIntent do Stripe
    val stripeTransferId: String? = null, // ID da transferência para o vendedor
    val status: String = "PENDING", // PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED
    val paidAt: Date? = null,
    val transferredAt: Date? = null, // Quando o valor foi transferido para o vendedor
    val failedAt: Date? = null,
    val failureReason: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

