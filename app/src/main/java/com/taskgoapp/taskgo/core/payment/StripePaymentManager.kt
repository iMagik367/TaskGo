package com.taskgoapp.taskgo.core.payment

import android.app.Activity
import androidx.activity.ComponentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class StripePaymentManager @Inject constructor() {
    
    private var isStripeInitialized = false
    
    /**
     * Initialize Stripe with publishable key
     */
    fun initialize(activity: Activity, publishableKey: String) {
        PaymentConfiguration.init(activity.application, publishableKey)
        isStripeInitialized = true
    }
    
    /**
     * Present PaymentSheet to user and process payment
     * Returns PaymentSheetResult indicating success or failure
     */
    suspend fun presentPaymentSheet(
        activity: Activity,
        clientSecret: String,
        customerId: String? = null,
        customerEphemeralKeySecret: String? = null
    ): Result<PaymentSheetResult> = suspendCancellableCoroutine { continuation ->
        try {
            if (!isStripeInitialized) {
                throw IllegalStateException("Stripe not initialized. Call initialize() first.")
            }
            
            val componentActivity = activity as? ComponentActivity
                ?: throw IllegalArgumentException("Activity must be a ComponentActivity")
            
            val paymentSheet = PaymentSheet(componentActivity) { result: PaymentSheetResult ->
                when (result) {
                    is PaymentSheetResult.Completed -> {
                        continuation.resume(Result.success(result))
                    }
                    is PaymentSheetResult.Canceled -> {
                        continuation.resume(Result.failure(Exception("Payment canceled by user")))
                    }
                    is PaymentSheetResult.Failed -> {
                        continuation.resume(Result.failure(result.error))
                    }
                }
            }
            
            val paymentSheetConfiguration = PaymentSheet.Configuration.Builder("TaskGo")
                .apply {
                    customerId?.let { customer ->
                        customerEphemeralKeySecret?.let { ephemeralKey ->
                            customer(
                                PaymentSheet.CustomerConfiguration(
                                    id = customer,
                                    ephemeralKeySecret = ephemeralKey
                                )
                            )
                        }
                    }
                }
                .build()
            
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = clientSecret,
                configuration = paymentSheetConfiguration
            )
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Check if Stripe is initialized
     */
    fun isInitialized(): Boolean {
        return isStripeInitialized
    }
}

