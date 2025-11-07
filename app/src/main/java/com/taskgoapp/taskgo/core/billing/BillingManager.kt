package com.taskgoapp.taskgo.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener, BillingClientStateListener {

    private var billingClient: BillingClient? = null
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _purchaseState.value = PurchaseState.Connected
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        _purchaseState.value = PurchaseState.Disconnected
        // Tentar reconectar
        billingClient?.startConnection(this)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
            }
        }
    }

    fun queryProducts(productIds: List<String>, @BillingClient.SkuType type: String) {
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(productIds)
            .setType(type)
            .build()

        billingClient?.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                _purchaseState.value = PurchaseState.ProductsLoaded(skuDetailsList)
            } else {
                _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, skuDetails: SkuDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        val responseCode = billingClient?.launchBillingFlow(activity, flowParams)?.responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Error("Erro ao iniciar compra: $responseCode")
        }
    }

    fun queryPurchases(@BillingClient.SkuType type: String) {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(type)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchaseState.value = PurchaseState.PurchasesLoaded(purchases)
            } else {
                _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
            }
        }
    }

    fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient?.acknowledgePurchase(acknowledgeParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _purchaseState.value = PurchaseState.PurchaseAcknowledged(purchase)
                } else {
                    _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
            _purchaseState.value = PurchaseState.PurchaseSuccess(purchase)
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _purchaseState.value = PurchaseState.PurchasePending(purchase)
        }
    }

    fun release() {
        billingClient?.endConnection()
    }
}

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Connected : PurchaseState()
    object Disconnected : PurchaseState()
    data class ProductsLoaded(val skuDetailsList: List<SkuDetails>) : PurchaseState()
    data class PurchasesLoaded(val purchases: List<Purchase>) : PurchaseState()
    data class PurchaseSuccess(val purchase: Purchase) : PurchaseState()
    data class PurchasePending(val purchase: Purchase) : PurchaseState()
    data class PurchaseAcknowledged(val purchase: Purchase) : PurchaseState()
    data class PurchaseRestored(val purchases: List<Purchase>) : PurchaseState()
    object Cancelled : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}


