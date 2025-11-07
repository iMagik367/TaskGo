package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.data.models.PaymentMethod
import com.taskgoapp.taskgo.core.data.models.CartItem
import com.taskgoapp.taskgo.core.data.models.Product
import com.taskgoapp.taskgo.core.data.models.PaymentType
import com.taskgoapp.taskgo.core.data.models.User
import com.taskgoapp.taskgo.core.data.models.AccountType
import com.taskgoapp.taskgo.core.data.models.Address
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    onNavigateBack: () -> Unit,
    onConfirmOrder: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var isConfirmed by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Dados vêm do Firestore - iniciar vazios
    val orderItems = remember { emptyList<CartItem>() }
    val selectedAddress = remember { null as Address? }
    val selectedPaymentMethod = remember { null as PaymentMethod? }
    
    val subtotal = orderItems.sumOf { it.product.price * it.quantity }
    val shipping = 0.0 // Free shipping
    val total = subtotal + shipping
    
    fun processOrder() {
        isProcessing = true
        // Processar pagamento via Cloud Function
        // TODO: Integrar com backend para processar pagamento
        MainScope().launch {
            delay(2000) // Simulate API call
            isProcessing = false
            isConfirmed = true
            showSuccessDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Resumo do Pedido",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Order Summary Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundGray
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = TaskGoGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Confirme seu pedido",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack
                    )
                    
                    Text(
                        text = "Revise as informações antes de confirmar",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
            }
            
            // Order Items
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Itens do Pedido",
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (orderItems.isEmpty()) {
                        Text(
                            text = "Carrinho vazio",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        orderItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = FigmaProductDescription,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Qtd: ${item.quantity}",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                            Text(
                                text = "R$ %.2f".format(item.product.price * item.quantity),
                                style = FigmaPrice,
                                color = TaskGoPriceGreen
                            )
                        }
                        
                            if (item != orderItems.last()) {
                                HorizontalDivider(color = TaskGoDivider, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
            
            // Delivery Address
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Endereço de Entrega",
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TaskGoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedAddress?.name ?: "Selecione um endereço",
                                style = FigmaProductName,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = selectedAddress?.street ?: "Nenhum endereço selecionado",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                            Text(
                                text = selectedAddress?.let { "${it.neighborhood}, ${it.city} - ${it.state}" } ?: "",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                            selectedAddress?.cep?.let {
                                Text(
                                    text = it,
                                    style = FigmaProductDescription,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                    }
                }
            }
            
            // Payment Method
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Forma de Pagamento",
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = TaskGoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedPaymentMethod?.let { "${it.cardholderName} •••• ${it.lastFourDigits}" } ?: "Selecione um método de pagamento",
                                style = FigmaProductName,
                                color = TaskGoTextBlack
                            )
                            Text(
                                text = selectedPaymentMethod?.let { when(it.type) {
                                    PaymentType.CREDIT_CARD -> "Cartão de Crédito"
                                    PaymentType.DEBIT_CARD -> "Cartão de Débito"
                                    PaymentType.PIX -> "Pix"
                                }} ?: "Nenhum método selecionado",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
            
            // Price Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Resumo Financeiro",
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal:",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = "R$ %.2f".format(subtotal),
                            style = FigmaProductDescription,
                            color = TaskGoTextBlack
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Frete:",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = if (shipping == 0.0) "Grátis" else "R$ %.2f".format(shipping),
                            style = FigmaProductDescription,
                            color = if (shipping == 0.0) TaskGoPriceGreen else TaskGoTextBlack
                        )
                    }
                    
                    HorizontalDivider(color = TaskGoDivider, modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            style = FigmaPrice,
                            color = TaskGoTextBlack
                        )
                        Text(
                            text = "R$ %.2f".format(total),
                            style = FigmaPrice,
                            color = TaskGoPriceGreen
                        )
                    }
                }
            }
            
            // Confirm Order Button
            PrimaryButton(
                text = if (isProcessing) "Processando..." else "Confirmar Pedido",
                onClick = { processOrder() },
                enabled = !isProcessing && !isConfirmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
    
    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TaskGoGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pedido Confirmado!")
                }
            },
            text = { 
                Text("Seu pedido foi processado com sucesso! Você receberá um e-mail de confirmação em breve.")
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showSuccessDialog = false
                        onConfirmOrder()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
