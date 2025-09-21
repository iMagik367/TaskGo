package com.example.taskgoapp.feature.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.taskgoapp.core.design.AppTopBar
import com.example.taskgoapp.core.model.OrderStatus
import com.example.taskgoapp.core.model.PurchaseOrder
import com.example.taskgoapp.core.model.OrderItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumoPedidoScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onIrParaPedidos: () -> Unit
) {
    // Dados de exemplo do pedido
    val order = remember(orderId) {
        PurchaseOrder(
            id = orderId,
            orderNumber = "12345",
            createdAt = System.currentTimeMillis() - 86400000,
            total = 750.0,
            subtotal = 750.0,
            deliveryFee = 0.0,
            status = OrderStatus.CONCLUIDO,
            items = listOf(
                OrderItem(
                    productId = "prod1",
                    price = 750.0,
                    quantity = 1
                )
            ),
            paymentMethod = "Crédito",
            trackingCode = null,
            deliveryAddress = "Rua das Flores, 123 - Centro, São Paulo - SP"
        )
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Resumo do Pedido",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order Details Section
            item {
                Text(
                    text = "Detalhes do pedido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Product Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Product Image
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (order.items.isNotEmpty() && order.items.first().productImage != null) {
                                AsyncImage(
                                    model = order.items.first().productImage,
                                    contentDescription = "Imagem do produto",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "Produto",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Product Details
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = order.items.firstOrNull()?.productName ?: "Produto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "R$ ${String.format("%.2f", order.total)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Chega até ${order.items.firstOrNull()?.deliveryDate ?: "Data não informada"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Price Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "R$ ${String.format("%.2f", order.subtotal)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Delivery
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Entrega",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (order.deliveryFee > 0) "R$ ${String.format("%.2f", order.deliveryFee)}" else "Grátis",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Divider
                        Divider()
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "R$ ${String.format("%.2f", order.total)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
            
            // Action Button
            item {
                Button(
                    onClick = onIrParaPedidos,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(
                        text = "Ir para pedidos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
