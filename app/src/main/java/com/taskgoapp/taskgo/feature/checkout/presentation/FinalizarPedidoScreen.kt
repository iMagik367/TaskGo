package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskgoapp.taskgo.core.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinalizarPedidoScreen(
    onBackClick: () -> Unit,
    onFinalizar: () -> Unit,
    variant: String? = null,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    LaunchedEffect(uiState.checkoutProcess) {
        when (val process = uiState.checkoutProcess) {
            is CheckoutProcessState.Success -> {
                onFinalizar()
            }
            is CheckoutProcessState.Error -> {
                // Erro será tratado na UI
            }
            else -> {}
        }
    }
    
    Column(
        Modifier.fillMaxSize().padding(32.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when(variant) {
            "payment_error" -> {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TaskGoError, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Erro no pagamento", style = FigmaTitleLarge, color = TaskGoError)
                Text(
                    uiState.checkoutProcess.let { 
                        if (it is CheckoutProcessState.Error) it.message else "Houve um problema ao processar o pagamento. Revise seus dados ou tente novamente."
                    },
                    style = FigmaProductDescription, 
                    color = TaskGoTextGray, 
                    modifier = Modifier.padding(8.dp)
                )
            }
            "pending" -> {
                Icon(Icons.Default.HourglassEmpty, null, tint = TaskGoWarning, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Pedido em processamento", style = FigmaTitleLarge, color = TaskGoWarning)
                Text("Aguardando confirmação do pagamento.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(8.dp))
            }
            "success" -> {
                Icon(Icons.Default.CheckCircle, null, tint = TaskGoSuccess, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Pedido realizado com sucesso!", style = FigmaTitleLarge, color = TaskGoSuccess)
                Text("Seu pedido está sendo preparado para envio.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(8.dp))
            }
            else -> {
                Text("Finalizar Pedido", style = FigmaSectionTitle, color = TaskGoTextBlack)
                Spacer(Modifier.height(12.dp))
                
                // Endereço selecionado
                val addressText = uiState.selectedAddress?.let { 
                    "${it.street}, ${it.number}${it.complement?.let { " - $it" } ?: ""}\n${it.district.ifEmpty { it.neighborhood }}, ${it.city} - ${it.state}\nCEP: ${it.zipCode.ifEmpty { it.cep }}"
                } ?: "Nenhum endereço selecionado"
                Text("Endereço de entrega:", style = FigmaProductName, color = TaskGoTextBlack)
                Text(addressText, color = TaskGoTextGray, style = FigmaProductDescription)
                
                Spacer(Modifier.height(12.dp))
                
                // Método de pagamento
                val paymentText = uiState.selectedPaymentMethod?.let {
                    when (it.type) {
                        com.taskgoapp.taskgo.core.model.PaymentType.CREDIT_CARD -> "Cartão de Crédito •••• ${it.lastFourDigits}"
                        com.taskgoapp.taskgo.core.model.PaymentType.DEBIT_CARD -> "Cartão de Débito •••• ${it.lastFourDigits}"
                        com.taskgoapp.taskgo.core.model.PaymentType.PIX -> "PIX"
                    }
                } ?: "Nenhum método selecionado"
                Text("Forma de pagamento:", style = FigmaProductName, color = TaskGoTextBlack)
                Text(paymentText, color = TaskGoTextGray, style = FigmaProductDescription)
                
                Spacer(Modifier.height(12.dp))
                
                // Resumo do pedido
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", style = FigmaProductDescription, color = TaskGoTextGray)
                        Text(currencyFormat.format(uiState.subtotal), style = FigmaProductDescription, color = TaskGoTextBlack)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Frete:", style = FigmaProductDescription, color = TaskGoTextGray)
                        Text(
                            if (uiState.shipping == 0.0) "Grátis" else currencyFormat.format(uiState.shipping),
                            style = FigmaProductDescription,
                            color = if (uiState.shipping == 0.0) TaskGoGreen else TaskGoTextBlack
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total:", style = FigmaProductName, color = TaskGoTextBlack)
                        Text(currencyFormat.format(uiState.total), style = FigmaPrice, color = TaskGoPriceGreen)
                    }
                }
                
                Spacer(Modifier.height(18.dp))
                
                val canFinalize = uiState.selectedAddress != null && 
                    uiState.selectedPaymentMethod != null && 
                    uiState.cartItems.isNotEmpty() &&
                    uiState.checkoutProcess !is CheckoutProcessState.Processing
                
                Button(
                    onClick = { 
                        uiState.selectedAddress?.id?.let { addressId ->
                            uiState.selectedPaymentMethod?.type?.let { paymentType ->
                                viewModel.finalizeOrder(addressId, paymentType)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canFinalize
                ) { 
                    if (uiState.checkoutProcess is CheckoutProcessState.Processing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
                    } else {
                        Text("Finalizar Pedido")
                    }
                }
                OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) { 
                    Text("Voltar") 
                }
            }
        }
    }
}
