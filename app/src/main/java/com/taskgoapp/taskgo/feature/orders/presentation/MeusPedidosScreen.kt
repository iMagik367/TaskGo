package com.taskgoapp.taskgo.feature.orders.presentation
import com.taskgoapp.taskgo.core.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.model.OrderStatus
import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.OrderItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusPedidosScreen(
    onBackClick: () -> Unit,
    onOrderClick: (String) -> Unit,
    onNavigateToCreateReview: ((String, String, String?) -> Unit)? = null, // targetId, type, orderId
    viewModel: MyOrdersViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Em andamento", "Concluído", "Cancelado")
    
    // Carregar pedidos reais do Firestore
    val allOrders by viewModel.orders.collectAsState()
    
    // Converter Order para PurchaseOrder para exibição
    val purchaseOrders = allOrders.map { order ->
        PurchaseOrder(
            id = order.id.toString(),
            orderNumber = order.id.toString(),
            createdAt = order.createdAt.toEpochMilli(),
            total = order.total,
            subtotal = order.total,
            deliveryFee = 0.0,
            status = when (order.status) {
                "EM_ANDAMENTO" -> OrderStatus.EM_ANDAMENTO
                "CONCLUIDO" -> OrderStatus.CONCLUIDO
                "CANCELADO" -> OrderStatus.CANCELADO
                else -> OrderStatus.EM_ANDAMENTO
            },
            items = order.items.map { item ->
                OrderItem(
                    productId = item.productId,
                    price = item.price,
                    quantity = item.quantity
                )
            },
            paymentMethod = "",
            trackingCode = null,
            deliveryAddress = null
        )
    }
    
    val filteredOrders = when (selectedTabIndex) {
        0 -> purchaseOrders.filter { it.status == OrderStatus.EM_ANDAMENTO }
        1 -> purchaseOrders.filter { it.status == OrderStatus.CONCLUIDO }
        2 -> purchaseOrders.filter { it.status == OrderStatus.CANCELADO }
        else -> emptyList()
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Pedidos",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Orders List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredOrders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum pedido encontrado",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) },
                            onNavigateToCreateReview = onNavigateToCreateReview
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: PurchaseOrder,
    onClick: () -> Unit,
    onNavigateToCreateReview: ((String, String, String?) -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val orderDate = dateFormat.format(Date(order.createdAt))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                if (order.items.isNotEmpty()) {
                    // Placeholder para imagem do produto
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "Produto",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
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
            
            // Order Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Pedido #${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (order.items.isNotEmpty()) {
                    Text(
                        text = "Produto ID: ${order.items.first().productId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "R$ ${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TaskGoSuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Data da compra $orderDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Status
                val statusText = when (order.status) {
                    OrderStatus.EM_ANDAMENTO -> "Em andamento"
                    OrderStatus.CONCLUIDO -> "Entregue"
                    OrderStatus.CANCELADO -> "Cancelado"
                }
                
                val statusColor = when (order.status) {
                    OrderStatus.EM_ANDAMENTO -> TaskGoSuccessGreen
                    OrderStatus.CONCLUIDO -> TaskGoSuccessGreen
                    OrderStatus.CANCELADO -> Color.Red
                }
                
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
                
                // Botão de avaliação para pedidos concluídos
                if (order.status == OrderStatus.CONCLUIDO && onNavigateToCreateReview != null && order.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val productId = order.items.first().productId
                            onNavigateToCreateReview(productId, "PRODUCT", order.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Avaliar Produto", style = FigmaButtonText)
                    }
                }
            }
            
            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver detalhes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}