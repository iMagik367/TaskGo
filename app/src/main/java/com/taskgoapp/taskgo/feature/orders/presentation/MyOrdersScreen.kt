package com.taskgoapp.taskgo.feature.orders.presentation

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
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.model.Order
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    onBackClick: () -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val viewModel: MyOrdersViewModel = hiltViewModel()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Em andamento", "Concluído", "Cancelado")
    
    val orders by viewModel.orders.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val filteredOrders = when (selectedTabIndex) {
        0 -> orders.filter { it.status in listOf("CONFIRMED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "PENDING") }
        1 -> orders.filter { it.status in listOf("DELIVERED", "CONCLUIDO") }
        2 -> orders.filter { it.status in listOf("CANCELLED", "CANCELADO") }
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
        "PENDING" -> "Pendente"
        "CONFIRMED" -> "Confirmado"
        "IN_TRANSIT" -> "Em trânsito"
        "OUT_FOR_DELIVERY" -> "Saiu para entrega"
        "DELIVERED" -> "Entregue"
        "CANCELLED" -> "Cancelado"
        else -> order.status
    }
    
    val statusColor = when (order.status) {
        "PENDING" -> MaterialTheme.colorScheme.tertiary
        "CONFIRMED" -> MaterialTheme.colorScheme.primary
        "IN_TRANSIT" -> MaterialTheme.colorScheme.secondary
        "OUT_FOR_DELIVERY" -> MaterialTheme.colorScheme.secondary
        "DELIVERED" -> MaterialTheme.colorScheme.secondary
        "CANCELLED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
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
                        text = "${item.quantity}x Produto #${item.productId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "R$ %.2f".format(item.price * item.quantity),
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
            
            if (order.status == "IN_TRANSIT" || order.status == "OUT_FOR_DELIVERY") {
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
