package com.example.taskgoapp.feature.checkout.presentation

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
import com.example.taskgoapp.core.data.models.PaymentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    onNavigateBack: () -> Unit,
    onPaymentMethodSelected: (PaymentMethod) -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    
    val paymentMethods = remember {
        listOf(
            PaymentMethod(
                id = 1,
                type = PaymentType.CREDIT_CARD,
                lastFourDigits = "1234",
                cardholderName = "João Silva",
                expiryDate = "12/25",
                isDefault = true
            ),
            PaymentMethod(
                id = 2,
                type = PaymentType.CREDIT_CARD,
                lastFourDigits = "5678",
                cardholderName = "João Silva",
                expiryDate = "12/26",
                isDefault = false
            ),
            PaymentMethod(
                id = 3,
                type = PaymentType.PIX,
                lastFourDigits = "",
                cardholderName = "",
                expiryDate = "",
                isDefault = false
            )
        )
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Forma de Pagamento",
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
            // Payment Methods List
            paymentMethods.forEach { method ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPaymentMethod?.id == method.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentMethod?.id == method.id,
                            onClick = { selectedPaymentMethod = method }
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Payment Method Icon
                        when (method.type) {
                            PaymentType.CREDIT_CARD -> {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            PaymentType.PIX -> {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            PaymentType.DEBIT_CARD -> {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            when (method.type) {
                                PaymentType.CREDIT_CARD -> {
                                    Text(
                                        text = "•••• ${method.lastFourDigits}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Cartão de Crédito",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PaymentType.PIX -> {
                                    Text(
                                        text = "PIX",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Pagamento instantâneo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PaymentType.DEBIT_CARD -> {
                                    Text(
                                        text = "•••• ${method.lastFourDigits}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Cartão de Débito",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            if (method.isDefault) {
                                Text(
                                    text = "Padrão",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        if (method.type == PaymentType.CREDIT_CARD || method.type == PaymentType.DEBIT_CARD) {
                            IconButton(
                                onClick = { /* TODO: Edit card */ }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar cartão"
                                )
                            }
                        }
                    }
                }
            }
            
            // Add New Payment Method
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Adicionar nova forma de pagamento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { showAddCardDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Adicionar",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Continue Button
            PrimaryButton(
                text = "Continuar",
                onClick = { 
                    selectedPaymentMethod?.let { onPaymentMethodSelected(it) }
                },
                enabled = selectedPaymentMethod != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
    
    // Add Card Dialog
    if (showAddCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Adicionar Cartão") },
            text = { 
                Text("Funcionalidade de adicionar cartão será implementada em uma versão futura.")
            },
            confirmButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
