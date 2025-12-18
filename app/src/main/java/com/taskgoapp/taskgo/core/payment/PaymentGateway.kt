package com.taskgoapp.taskgo.core.payment

import com.taskgoapp.taskgo.core.model.PaymentType
import com.taskgoapp.taskgo.core.tracking.TrackingCodeGenerator
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.taskgoapp.taskgo.domain.usecase.CheckoutUseCase
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentGateway @Inject constructor(
    private val checkoutUseCase: CheckoutUseCase,
    private val functionsService: FirebaseFunctionsService,
    private val trackingRepository: TrackingRepository
) {

    suspend fun process(request: PaymentGatewayRequest): Result<PaymentGatewayResult> {
        return try {
            // Criar pedido primeiro
            // Mapear PaymentType para string legível
            val paymentMethodString = when (request.paymentType) {
                PaymentType.CREDIT_CARD -> "Crédito"
                PaymentType.DEBIT_CARD -> "Débito"
                PaymentType.PIX -> "Pix"
            }
            val orderResult = checkoutUseCase.invoke(
                paymentMethod = paymentMethodString,
                addressId = request.addressId
            )
            val orderId = orderResult.getOrThrow()

            // Se for PIX, criar pagamento PIX
            if (request.paymentType == PaymentType.PIX) {
                val pixData = functionsService.createPixPayment(orderId).getOrElse { throw it }
                val pixKey = pixData["pixKey"] as? String ?: ""
                val qrCodeData = pixData["qrCodeData"] as? String ?: ""
                
                // Para PIX, o pagamento fica pendente até confirmação
                // Não confirmamos automaticamente como nos cartões
                
                return Result.success(
                    PaymentGatewayResult(
                        orderId = orderId,
                        paymentIntentId = pixKey, // Usar chave PIX como ID
                        clientSecret = qrCodeData, // Usar QR code data como secret
                        trackingCode = TrackingCodeGenerator.generate(orderId),
                        paymentType = request.paymentType
                    )
                )
            }

            // Para produtos, usar createProductPaymentIntent (com split payment e comissão)
            // Para serviços, usar createPaymentIntent (pagamento de serviços)
            val intentData = if (isProductOrder(orderId)) {
                // Usar novo sistema de pagamento de produtos com Stripe Connect
                functionsService.createProductPaymentIntent(orderId).getOrElse { throw it }
            } else {
                // Usar sistema de pagamento de serviços
                functionsService.createPaymentIntent(orderId).getOrElse { throw it }
            }
            
            val paymentIntentId = intentData["paymentIntentId"] as? String
                ?: intentData["id"] as? String
                ?: throw IllegalStateException("paymentIntentId não retornado pelo backend")
            val clientSecret = intentData["clientSecret"] as? String
                ?: throw IllegalStateException("clientSecret não retornado pelo backend")

            // NÃO confirmar pagamento aqui - o PaymentSheet vai processar o pagamento
            // O webhook do Stripe vai confirmar automaticamente quando o pagamento for bem-sucedido
            // Retornar o clientSecret para que o app possa apresentar o PaymentSheet
            
            Result.success(
                PaymentGatewayResult(
                    orderId = orderId,
                    paymentIntentId = paymentIntentId,
                    clientSecret = clientSecret,
                    trackingCode = TrackingCodeGenerator.generate(orderId),
                    paymentType = request.paymentType
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
    
    // Verifica se é um pedido de produto (purchase_order) ou serviço (order)
    private suspend fun isProductOrder(orderId: String): Boolean {
        // Por padrão, assumimos que pedidos criados via checkout são de produtos
        // Isso pode ser melhorado verificando a coleção no Firestore
        return true // Por enquanto, sempre produtos
    }
}

data class PaymentGatewayRequest(
    val paymentType: PaymentType,
    val addressId: String
)

data class PaymentGatewayResult(
    val orderId: String,
    val paymentIntentId: String,
    val clientSecret: String?,
    val trackingCode: String,
    val paymentType: PaymentType
)

