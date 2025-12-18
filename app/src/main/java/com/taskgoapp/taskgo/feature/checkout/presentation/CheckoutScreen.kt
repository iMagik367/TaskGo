package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.model.PaymentType
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutProcessState.Error
import com.taskgoapp.taskgo.feature.checkout.presentation.CheckoutProcessState.Processing
import com.taskgoapp.taskgo.feature.checkout.presentation.components.CheckoutBottomBar
import com.taskgoapp.taskgo.feature.checkout.presentation.components.CheckoutCartItemRow
import com.taskgoapp.taskgo.feature.checkout.presentation.components.EmptyCheckoutState
import com.taskgoapp.taskgo.feature.checkout.presentation.components.SelectedFieldCard
import com.taskgoapp.taskgo.feature.checkout.presentation.components.SummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBackClick: () -> Unit,
    onAddressSelection: () -> Unit,
    onPaymentMethodSelection: () -> Unit,
    onOrderSummary: (String, PaymentType) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedAddress = uiState.selectedAddress
    val selectedPaymentMethod = uiState.selectedPaymentMethod
    
    // Garantir que PIX esteja sempre disponível como método padrão
    LaunchedEffect(uiState.availablePaymentMethods, uiState.selectedPaymentMethod) {
        if (uiState.availablePaymentMethods.isNotEmpty() && 
            uiState.selectedPaymentMethod == null) {
            // Selecionar PIX automaticamente se disponível
            val pixMethod = uiState.availablePaymentMethods.firstOrNull { it.type == PaymentType.PIX }
            pixMethod?.let { viewModel.selectPaymentMethod(it) }
        }
    }
    
    // Preservar seleções quando voltar das telas de seleção
    LaunchedEffect(Unit) {
        // Garantir que seleções sejam preservadas
        // O ViewModel já mantém o estado, mas vamos garantir que esteja sincronizado
    }
    val canContinue = uiState.cartItems.isNotEmpty() &&
        selectedAddress != null &&
        selectedPaymentMethod != null &&
        uiState.checkoutProcess !is Processing

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Finalizar compra",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            CheckoutBottomBar(
                total = uiState.total,
                enabled = canContinue,
                isLoading = uiState.checkoutProcess is Processing,
                onCheckout = {
                    if (selectedAddress != null && selectedPaymentMethod != null) {
                        onOrderSummary(selectedAddress.id, selectedPaymentMethod.type)
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.cartItems.isEmpty() -> {
                EmptyCheckoutState(modifier = Modifier.padding(paddingValues))
            }

            else -> {
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
                        value = selectedAddress?.name ?: "Selecione um endereço",
                        subtitle = selectedAddress?.let { "${it.city} - ${it.state}" }
                            ?: if (uiState.availableAddresses.isEmpty()) {
                                "Nenhum endereço cadastrado"
                            } else {
                                "Nenhum endereço selecionado"
                            },
                        icon = Icons.Default.LocationOn,
                        onAction = onAddressSelection
                    )

                    SelectedFieldCard(
                        title = "Forma de pagamento",
                        value = selectedPaymentMethod?.let { 
                            if (it.type == PaymentType.PIX) "PIX" else "•••• ${it.lastFourDigits}"
                        } ?: "Selecione um método",
                        subtitle = selectedPaymentMethod?.let {
                            when (it.type) {
                                PaymentType.CREDIT_CARD -> "Cartão de crédito${if (it.cardholderName.isNotEmpty()) " (${it.cardholderName})" else ""}"
                                PaymentType.DEBIT_CARD -> "Cartão de débito${if (it.cardholderName.isNotEmpty()) " (${it.cardholderName})" else ""}"
                                PaymentType.PIX -> "PIX - Pagamento instantâneo"
                            }
                        } ?: if (uiState.availablePaymentMethods.isEmpty()) {
                            "Nenhum método disponível"
                        } else {
                            "Nenhum método selecionado"
                        },
                        icon = Icons.Default.CreditCard,
                        onAction = onPaymentMethodSelection
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

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (uiState.checkoutProcess is Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetCheckoutProcess() },
            confirmButton = {
                TextButton(onClick = { viewModel.resetCheckoutProcess() }) {
                    Text("Entendi")
                }
            },
            title = { Text("Não foi possível continuar") },
            text = { Text((uiState.checkoutProcess as Error).message) }
        )
    }
}

