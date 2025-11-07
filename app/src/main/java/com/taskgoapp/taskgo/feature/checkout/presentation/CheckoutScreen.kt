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
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.data.models.PaymentMethod
import com.taskgoapp.taskgo.core.data.models.CartItem
import com.taskgoapp.taskgo.core.data.models.Product
import com.taskgoapp.taskgo.core.data.models.PaymentType
import com.taskgoapp.taskgo.core.data.models.User
import com.taskgoapp.taskgo.core.data.models.AccountType
import com.taskgoapp.taskgo.core.data.models.Address
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBackClick: () -> Unit,
    onAddressSelection: () -> Unit,
    onPaymentMethodSelection: () -> Unit,
    onOrderSummary: () -> Unit
) {
    // Dados vêm do Firestore - iniciar vazios
    val selectedAddress = remember { null as Address? }
    val selectedPaymentMethod = remember { null as PaymentMethod? }
    val cartItems = remember { emptyList<CartItem>() }
    
    val subtotal = cartItems.sumOf { it.product.price * it.quantity }
    val shipping = 0.0 // Free shipping
    val total = subtotal + shipping
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Finalizar Compra",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Address Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Endereço de Entrega",
                            style = FigmaProductName,
                            color = TaskGoTextBlack
                        )
                        
                        TextButton(onClick = onAddressSelection) {
                            Text("Alterar")
                        }
                    }
                    
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
                                text = selectedAddress?.let { "${it.neighborhood}, ${it.city} - ${it.state}" } ?: "Nenhum endereço selecionado",
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
            
            // Payment Method Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Forma de Pagamento",
                            style = FigmaProductName,
                            color = TaskGoTextBlack
                        )
                        
                        TextButton(onClick = onPaymentMethodSelection) {
                            Text("Alterar")
                        }
                    }
                    
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
            
            // Order Summary Section
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
                        text = "Resumo do Pedido",
                        style = FigmaProductName,
                        color = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cart Items
                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = FigmaProductDescription,
                                color = TaskGoTextGray,
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
                                style = FigmaProductDescription,
                                color = TaskGoTextGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (item != cartItems.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Price Breakdown
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
            
            // Continue Button
            PrimaryButton(
                text = "Continuar para Pagamento",
                onClick = onOrderSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

