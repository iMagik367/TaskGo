package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.BorderStroke
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
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoBorder
import com.taskgoapp.taskgo.core.data.*
import com.taskgoapp.taskgo.core.data.models.Address as ModelAddress
import com.taskgoapp.taskgo.core.data.models.PaymentMethod as ModelPaymentMethod
import com.taskgoapp.taskgo.core.data.models.PaymentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBackClick: () -> Unit,
    onAddressSelection: () -> Unit,
    onPaymentMethodSelection: () -> Unit,
    onOrderSummary: () -> Unit
) {
    val selectedAddress = ModelAddress(
        id = 1L,
        name = "Carlos Silva",
        phone = "(11) 99999-9999",
        cep = "01234-567",
        street = "Rua das Flores",
        neighborhood = "Centro",
        city = "São Paulo",
        state = "SP",
        isDefault = true
    )
    
    val selectedPaymentMethod = ModelPaymentMethod(
        id = 1L,
        type = PaymentType.PIX,
        lastFourDigits = "PIX",
        cardholderName = "Carlos Silva",
        expiryDate = "",
        isDefault = true
    )
    
    val subtotal = 1349.97
    val shipping = 29.99
    val total = subtotal + shipping
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Checkout",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "R$ %.2f".format(total),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        PrimaryButton(
                            text = "Finalizar Pedido",
                            onClick = onOrderSummary,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delivery Address
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onAddressSelection) {
                            Text("Alterar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${selectedAddress.name} - ${selectedAddress.phone}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${selectedAddress.street}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${selectedAddress.neighborhood}, ${selectedAddress.city} - ${selectedAddress.state}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "CEP: ${selectedAddress.cep}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Payment Method
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onPaymentMethodSelection) {
                            Text("Alterar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (selectedPaymentMethod.type) {
                                PaymentType.PIX -> Icons.Default.QrCode
                                PaymentType.CREDIT_CARD -> Icons.Default.CreditCard
                                PaymentType.DEBIT_CARD -> Icons.Default.CreditCard
                                else -> Icons.Default.Payment
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (selectedPaymentMethod.type) {
                                    PaymentType.PIX -> "PIX"
                                    PaymentType.CREDIT_CARD -> "Cartão de Crédito"
                                    PaymentType.DEBIT_CARD -> "Cartão de Débito"
                                    else -> "Pagamento"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = selectedPaymentMethod.cardholderName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Order Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Resumo do Pedido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Order Items
                    val orderItems = listOf(
                        "1x Furadeira sem fio - R$ 299,99",
                        "1x Guarda Roupa 6 Portas - R$ 899,99",
                        "2x Kit de Ferramentas Básicas - R$ 299,98"
                    )
                    
                    orderItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Totals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "R$ %.2f".format(subtotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Frete",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "R$ %.2f".format(shipping),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "R$ %.2f".format(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


