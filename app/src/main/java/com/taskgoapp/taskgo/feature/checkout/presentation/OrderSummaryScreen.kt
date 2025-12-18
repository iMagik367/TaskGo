package com.taskgoapp.taskgo.feature.checkout.presentation

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.model.PaymentType
import com.taskgoapp.taskgo.core.payment.StripePaymentManager
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutProcessState.Error
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutProcessState.PaymentSheetReady
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutProcessState.Processing
import com.taskgoapp.taskgo.feature.checkout.presentation.components.CheckoutCartItemRow
import com.taskgoapp.taskgo.feature.checkout.presentation.components.SelectedFieldCard
import com.taskgoapp.taskgo.feature.checkout.presentation.components.SummaryCard
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    onNavigateBack: () -> Unit,
    onOrderFinished: (orderId: String, total: Double, trackingCode: String) -> Unit,
    onNavigateToPix: (orderId: String, total: Double) -> Unit,
    addressId: String,
    paymentTypeName: String,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paymentType = runCatching { PaymentType.valueOf(paymentTypeName) }.getOrDefault(PaymentType.PIX)
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    
    // Stripe Payment Helper ViewModel
    val stripeHelperViewModel = hiltViewModel<StripePaymentHelperViewModel>()
    val stripeInitialized by stripeHelperViewModel.isInitialized.collectAsStateWithLifecycle()

    LaunchedEffect(addressId, paymentTypeName) {
        if (addressId.isNotBlank()) {
            viewModel.applySelection(addressId, paymentType)
        }
    }
    
    // Initialize Stripe when needed (for card payments)
    LaunchedEffect(paymentType) {
        if ((paymentType == PaymentType.CREDIT_CARD || paymentType == PaymentType.DEBIT_CARD) 
            && !stripeInitialized && activity != null) {
            stripeHelperViewModel.initializeStripe(activity)
        }
    }
    
    // Handle PaymentSheet ready state
    LaunchedEffect(uiState.checkoutProcess, stripeInitialized) {
        val process = uiState.checkoutProcess
        if (process is PaymentSheetReady && stripeInitialized && activity != null) {
            scope.launch {
                val result = stripeHelperViewModel.presentPaymentSheet(
                    activity = activity,
                    clientSecret = process.clientSecret
                )
                result.onSuccess {
                    // Payment successful - webhook will confirm automatically
                    viewModel.onPaymentSheetSuccess(process.orderId, "")
                    onOrderFinished(process.orderId, uiState.total, "")
                }.onFailure { error ->
                    viewModel.onPaymentSheetError(error.message ?: "Erro ao processar pagamento")
                }
            }
        }
    }

    LaunchedEffect(uiState.checkoutProcess) {
        val process = uiState.checkoutProcess
        if (process is CheckoutProcessState.Success) {
            // Se for PIX, navegar para tela de PIX em vez de finalizar imediatamente
            if (paymentType == PaymentType.PIX) {
                onNavigateToPix(process.orderId, uiState.total)
                viewModel.resetCheckoutProcess()
            } else {
                onOrderFinished(process.orderId, uiState.total, process.trackingCode)
                viewModel.resetCheckoutProcess()
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Resumo do pedido",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectedFieldCard(
                title = "Endereço de entrega",
                value = uiState.selectedAddress?.name ?: "Selecione um endereço",
                subtitle = uiState.selectedAddress?.let { "${it.city} - ${it.state}" }
                    ?: "Nenhum endereço selecionado",
                icon = Icons.Default.LocationOn,
                onAction = onNavigateBack
            )

            SelectedFieldCard(
                title = "Pagamento",
                value = uiState.selectedPaymentMethod?.let { "•••• ${it.lastFourDigits}" }
                    ?: "Selecione uma forma de pagamento",
                subtitle = uiState.selectedPaymentMethod?.let {
                    when (it.type) {
                        PaymentType.CREDIT_CARD -> "Cartão de crédito (${it.cardholderName})"
                        PaymentType.DEBIT_CARD -> "Cartão de débito (${it.cardholderName})"
                        PaymentType.PIX -> "PIX"
                    }
                } ?: "Você poderá alterar na etapa anterior",
                icon = Icons.Default.CreditCard,
                onAction = onNavigateBack
            )

            SummaryCard(
                subtotal = uiState.subtotal,
                shipping = uiState.shipping,
                total = uiState.total
            )

            HorizontalDivider()

            uiState.cartItems.forEach { item ->
                CheckoutCartItemRow(item = item)
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = if (uiState.checkoutProcess is Processing) "Processando..." else "Confirmar pedido",
                enabled = uiState.checkoutProcess !is Processing,
                onClick = { viewModel.finalizeOrder(addressId, paymentType) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (uiState.checkoutProcess is Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetCheckoutProcess() },
            confirmButton = {
                TextButton(onClick = { viewModel.resetCheckoutProcess() }) {
                    Text("Tentar novamente")
                }
            },
            title = { Text("Não foi possível concluir o pedido") },
            text = { Text((uiState.checkoutProcess as Error).message) }
        )
    }
}

