package com.taskgoapp.taskgo.feature.checkout.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.payment.StripePaymentManager
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StripePaymentHelperViewModel @Inject constructor(
    private val functionsService: FirebaseFunctionsService,
    private val stripePaymentManager: StripePaymentManager
) : ViewModel() {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    fun initializeStripe(activity: Activity) {
        viewModelScope.launch {
            try {
                val result = functionsService.getStripePublishableKey()
                result.onSuccess { data ->
                    val publishableKey = data["publishableKey"] as? String
                    if (publishableKey != null) {
                        stripePaymentManager.initialize(activity, publishableKey)
                        _isInitialized.value = true
                    }
                }.onFailure {
                    // Handle error
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    suspend fun presentPaymentSheet(
        activity: Activity,
        clientSecret: String
    ): Result<com.stripe.android.paymentsheet.PaymentSheetResult> {
        return stripePaymentManager.presentPaymentSheet(activity, clientSecret)
    }
}

