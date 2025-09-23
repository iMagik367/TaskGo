package br.com.taskgo.taskgo.feature.checkout.presentation

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
import com.example.taskgoapp.core.design.*
import com.example.taskgoapp.core.data.models.PaymentMethod
import com.example.taskgoapp.core.data.models.CartItem
import com.example.taskgoapp.core.data.models.Product
import com.example.taskgoapp.core.data.models.PaymentType
import com.example.taskgoapp.core.data.models.User
import com.example.taskgoapp.core.data.models.AccountType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    onNavigateBack: () -> Unit,
    onConfirmOrder: () -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var isConfirmed by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Mock data
    val orderItems = remember {
        listOf(
            CartItem(
                id = 1,
                product = Product(
                    id = 1,
                    name = "Furadeira sem fio DeWalt 20V",
                    description = "Furadeira de impacto profissional",
                    price = 299.99,
                    seller = User(
                        id = 1,
                        name = "Ferramentas Pro",
                        email = "contato@ferramentaspro.com",
                        phone = "(11) 88888-8888",
                        accountType = AccountType.SELLER,
                        timeOnTaskGo = "5 anos",
                        rating = 4.8,
                        reviewCount = 156,
                        city = "São Paulo"
                    ),
                    category = "Ferramentas",
                    inStock = true
                ),
                quantity = 1
            ),
            CartItem(
                id = 2,
                product = Product(
                    id = 2,
                    name = "Kit de Ferramentas Básicas",
                    description = "Kit completo com 32 peças",
                    price = 149.99,
                    seller = User(
                        id = 1,
                        name = "Ferramentas Pro",
                        email = "contato@ferramentaspro.com",
                        phone = "(11) 88888-8888",
                        accountType = AccountType.SELLER,
                        timeOnTaskGo = "5 anos",
                        rating = 4.8,
                        reviewCount = 156,
                        city = "São Paulo"
                    ),
                    category = "Ferramentas",
                    inStock = true
                ),
                quantity = 2
            )
        )
    }
    
    val selectedAddress = remember {
        Address(
            id = 1,
            name = "Casa",
            street = "Rua das Flores, 123",
            neighborhood = "Centro",
            city = "São Paulo",
            state = "SP",
            cep = "01234-567",
            phone = "(11) 99999-9999"
        )
    }
    
    val selectedPaymentMethod = remember {
        PaymentMethod(
            id = 1,
            type = PaymentType.CREDIT_CARD,
            lastFourDigits = "1234",
            cardholderName = "João Silva",
            expiryDate = "12/25",
            isDefault = true
        )
    }
    
    val subtotal = orderItems.sumOf { it.product.price * it.quantity }
    val shipping = 0.0 // Free shipping
    val total = subtotal + shipping
    
    fun processOrder() {
        isProcessing = true
        // Simulate payment processing
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Confirme seu pedido",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "Revise as informações antes de confirmar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    orderItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Qtd: ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "R$ %.2f".format(item.product.price * item.quantity),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (item != orderItems.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedAddress.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = selectedAddress.street,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${selectedAddress.neighborhood}, ${selectedAddress.city} - ${selectedAddress.state}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = selectedAddress.cep,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${selectedPaymentMethod.cardholderName} •••• ${selectedPaymentMethod.lastFourDigits}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Cartão de Crédito",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtotal:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "R$ %.2f".format(subtotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Frete:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (shipping == 0.0) "Grátis" else "R$ %.2f".format(shipping),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (shipping == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "R$ %.2f".format(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                        tint = MaterialTheme.colorScheme.primary,
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
