package com.taskgoapp.taskgo.core.payment

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class GooglePayManager(private val context: Context) {

    private val paymentsClient: PaymentsClient = Wallet.getPaymentsClient(
        context,
        Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // Mude para PRODUCTION em produção
            .build()
    )

    /**
     * Verifica se o Google Pay está disponível no dispositivo
     */
    fun isReadyToPay(): Task<Boolean> {
        val request = IsReadyToPayRequest.fromJson(createIsReadyToPayRequest().toString())
        return paymentsClient.isReadyToPay(request)
    }

    /**
     * Cria uma solicitação de pagamento
     */
    fun createPaymentRequest(
        price: String,
        currencyCode: String = "BRL",
        priceStatus: String = "FINAL"
    ): PaymentDataRequest {
        val request = createPaymentDataRequest(price, currencyCode, priceStatus)
        return PaymentDataRequest.fromJson(request.toString())
    }

    /**
     * Inicia o fluxo de pagamento
     */
    fun loadPaymentData(
        activity: Activity,
        price: String,
        currencyCode: String = "BRL",
        onSuccess: (PaymentData) -> Unit,
        onError: (Exception) -> Unit
    ) {
        isReadyToPay().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result == true) {
                val request = createPaymentRequest(price, currencyCode)
                val taskPayment = paymentsClient.loadPaymentData(request)
                taskPayment.addOnCompleteListener { paymentTask ->
                    if (paymentTask.isSuccessful) {
                        onSuccess(paymentTask.result)
                    } else {
                        val exception = paymentTask.exception
                        if (exception is ApiException) {
                            when (exception.statusCode) {
                                WalletConstants.RESULT_ERROR -> {
                                    // Erro desconhecido
                                    onError(exception)
                                }
                                16 -> { // RESULT_CANCELED
                                    // Usuário cancelou
                                    onError(Exception("Pagamento cancelado pelo usuário"))
                                }
                                else -> {
                                    onError(exception)
                                }
                            }
                        } else {
                            onError(exception ?: Exception("Erro desconhecido"))
                        }
                    }
                }
            } else {
                onError(Exception("Google Pay não está disponível neste dispositivo"))
            }
        }
    }

    private fun createIsReadyToPayRequest(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "CARD")
                    put("parameters", JSONObject().apply {
                        put("allowedAuthMethods", JSONArray().apply {
                            put("PAN_ONLY")
                            put("CRYPTOGRAM_3DS")
                        })
                        put("allowedCardNetworks", JSONArray().apply {
                            put("AMEX")
                            put("DISCOVER")
                            put("JCB")
                            put("MASTERCARD")
                            put("VISA")
                        })
                    })
                    put("tokenizationSpecification", JSONObject().apply {
                        put("type", "PAYMENT_GATEWAY")
                        put("parameters", JSONObject().apply {
                            put("gateway", "stripe")
                            // Stripe gateway merchant ID - deve ser configurado via variável de ambiente
                            // Para produção, use o merchant ID do Stripe obtido do dashboard
                            put("gatewayMerchantId", "stripe")
                        })
                    })
                })
            })
        }
    }

    private fun createPaymentDataRequest(
        price: String,
        currencyCode: String,
        priceStatus: String
    ): JSONObject {
        val paymentDataRequest = JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("merchantInfo", JSONObject().apply {
                put("merchantName", "TaskGo")
                // Merchant ID do Google Pay - deve ser configurado no Google Pay Console
                // Para produção, use o merchant ID real obtido do Google Pay Business Console
                put("merchantId", "01234567890123456789")
            })
            put("allowedPaymentMethods", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "CARD")
                    put("parameters", JSONObject().apply {
                        put("allowedAuthMethods", JSONArray().apply {
                            put("PAN_ONLY")
                            put("CRYPTOGRAM_3DS")
                        })
                        put("allowedCardNetworks", JSONArray().apply {
                            put("AMEX")
                            put("DISCOVER")
                            put("JCB")
                            put("MASTERCARD")
                            put("VISA")
                        })
                        put("billingAddressRequired", true)
                        put("billingAddressParameters", JSONObject().apply {
                            put("format", "FULL")
                        })
                    })
                    put("tokenizationSpecification", JSONObject().apply {
                        put("type", "PAYMENT_GATEWAY")
                        put("parameters", JSONObject().apply {
                            put("gateway", "stripe")
                            // Stripe gateway merchant ID - deve ser configurado via variável de ambiente
                            // Para produção, use o merchant ID do Stripe obtido do dashboard
                            put("gatewayMerchantId", "stripe")
                        })
                    })
                })
            })
            put("transactionInfo", JSONObject().apply {
                put("totalPriceStatus", priceStatus)
                put("totalPrice", price)
                put("totalPriceLabel", "Total")
                put("currencyCode", currencyCode)
            })
            put("shippingAddressRequired", false)
            put("emailRequired", true)
        }

        return paymentDataRequest
    }

    /**
     * Extrai informações do pagamento do PaymentData
     */
    fun extractPaymentInfo(paymentData: PaymentData): PaymentInfo {
        val paymentInfoJson = paymentData.toJson()
        return try {
            val paymentDataJson = JSONObject(paymentInfoJson)
            val paymentMethodData = paymentDataJson.getJSONObject("paymentMethodData")
            val tokenizationData = paymentMethodData.getJSONObject("tokenizationData")
            val token = tokenizationData.getString("token")
            
            PaymentInfo(
                token = token,
                email = paymentDataJson.optString("email"),
                billingAddress = paymentDataJson.optJSONObject("billingAddress")?.toString()
            )
        } catch (e: JSONException) {
            throw Exception("Erro ao processar dados do pagamento", e)
        }
    }
}

data class PaymentInfo(
    val token: String,
    val email: String?,
    val billingAddress: String?
)

