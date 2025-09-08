package com.example.taskgoapp.feature.orders.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.*
import com.example.taskgoapp.core.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    onBackClick: () -> Unit,
    onOrderClick: (Long) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Em andamento", "Concluído", "Cancelado")
    
    val orders = listOf(
        Order(
            id = 1,
            items = listOf(
                CartItem(
                    Product(
                        id = 1,
                        title = "Furadeira sem fio",
                        price = 299.99,
                        description = "Furadeira de impacto 20V",
                        seller = mockProviders[0],
                        category = "Ferramentas",
                        inStock = true
                    ),
                    1
                )
            ),
            total = 299.99,
            status = OrderStatus.IN_TRANSIT
        ),
        Order(
            id = 2,
            items = listOf(
                CartItem(
                    Product(
                        id = 2,
                        title = "Guarda Roupa 6 Portas",
                        price = 899.99,
                        description = "Guarda roupa com espelho",
                        seller = mockProviders[1],
                        category = "Móveis",
                        inStock = true
                    ),
                    1
                )
            ),
            total = 899.99,
            status = OrderStatus.DELIVERED
        ),
        Order(
            id = 3,
            items = listOf(
                CartItem(
                    Product(
                        id = 3,
                        title = "Smartphone Galaxy A54",
                        price = 1899.99,
                        description = "Smartphone Samsung Galaxy A54 5G",
                        seller = mockProviders[2],
                        category = "Eletrônicos",
                        inStock = true
                    ),
                    1
                )
            ),
            total = 1899.99,
            status = OrderStatus.CANCELLED
        )
    )
    
    val filteredOrders = when (selectedTabIndex) {
        0 -> orders.filter { it.status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.IN_TRANSIT, OrderStatus.OUT_FOR_DELIVERY) }
        1 -> orders.filter { it.status == OrderStatus.DELIVERED }
        2 -> orders.filter { it.status == OrderStatus.CANCELLED }
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
                        EmptyState(
                            icon = Icons.Default.ShoppingBag,
                            title = "Nenhum pedido encontrado",
                            message = "Você não tem pedidos com este status."
                        )
                    }
                } else {
                    items(filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = when (order.status) {
        OrderStatus.PENDING -> "Pendente"
        OrderStatus.CONFIRMED -> "Confirmado"
        OrderStatus.IN_TRANSIT -> "Em trânsito"
        OrderStatus.OUT_FOR_DELIVERY -> "Saiu para entrega"
        OrderStatus.DELIVERED -> "Entregue"
        OrderStatus.CANCELLED -> "Cancelado"
    }
    
    val statusColor = when (order.status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
        OrderStatus.IN_TRANSIT -> MaterialTheme.colorScheme.secondary
        OrderStatus.OUT_FOR_DELIVERY -> MaterialTheme.colorScheme.secondary
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = "Pedido #${order.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = { },
                    label = { Text(statusText) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Order Items
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.quantity}x ${item.product.title}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "R$ %.2f".format(item.product.price * item.quantity),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "R$ %.2f".format(order.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (order.status == OrderStatus.IN_TRANSIT || order.status == OrderStatus.OUT_FOR_DELIVERY) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Implementar cancelamento */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancelar Item")
                    }
                    PrimaryButton(
                        text = "Ver Rastreio",
                        onClick = { /* TODO: Implementar rastreamento */ }
                    )
                }
            }
        }
    }
}
