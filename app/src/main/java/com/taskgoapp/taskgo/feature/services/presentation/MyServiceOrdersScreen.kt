package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.feature.home.presentation.FilterTypeChip
import java.text.SimpleDateFormat
import java.util.*

enum class ServiceOrderTab {
    ACTIVE, CANCELLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyServiceOrdersScreen(
    onBackClick: () -> Unit,
    onEditOrder: (String) -> Unit,
    onCreateOrder: () -> Unit = {},
    viewModel: MyServiceOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ServiceOrderTab.ACTIVE) }
    
    // Filtrar ordens baseado na aba selecionada
    val filteredOrders = when (selectedTab) {
        ServiceOrderTab.ACTIVE -> uiState.orders.filter { 
            it.status.lowercase() != "cancelled" && it.status.lowercase() != "cancelada"
        }
        ServiceOrderTab.CANCELLED -> uiState.orders.filter { 
            it.status.lowercase() == "cancelled" || it.status.lowercase() == "cancelada"
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Minhas Ordens de Serviço",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            if (selectedTab == ServiceOrderTab.ACTIVE) {
                FloatingActionButton(
                    onClick = onCreateOrder,
                    containerColor = TaskGoGreen
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Criar Ordem de Serviço",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Abas Ativas/Canceladas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTypeChip(
                    text = "Ativas",
                    selected = selectedTab == ServiceOrderTab.ACTIVE,
                    onClick = { selectedTab = ServiceOrderTab.ACTIVE },
                    modifier = Modifier.weight(1f)
                )
                FilterTypeChip(
                    text = "Canceladas",
                    selected = selectedTab == ServiceOrderTab.CANCELLED,
                    onClick = { selectedTab = ServiceOrderTab.CANCELLED },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Conteúdo das ordens
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "Erro ao carregar ordens",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (selectedTab) {
                                ServiceOrderTab.ACTIVE -> "Nenhuma ordem de serviço ativa encontrada"
                                ServiceOrderTab.CANCELLED -> "Nenhuma ordem de serviço cancelada encontrada"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TaskGoTextGray
                        )
                        if (selectedTab == ServiceOrderTab.ACTIVE) {
                            Text(
                                text = "Crie sua primeira ordem de serviço",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
                        ServiceOrderCard(
                            order = order,
                            onEditClick = { 
                                if (selectedTab == ServiceOrderTab.ACTIVE) {
                                    onEditOrder(order.id) 
                                }
                            },
                            onDeleteClick = { 
                                if (selectedTab == ServiceOrderTab.ACTIVE) {
                                    viewModel.deleteOrder(order.id) 
                                }
                            },
                            showActions = selectedTab == ServiceOrderTab.ACTIVE
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceOrderCard(
    order: ServiceOrderItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    showActions: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                    if (order.dueDate != null) {
                        Text(
                            text = "Prazo: ${formatDate(order.dueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                    }
                }
                StatusChip(status = order.status)
            }
            
            Text(
                text = order.details,
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextDark,
                maxLines = 3
            )
            
            if (order.location.isNotEmpty()) {
                Text(
                    text = "📍 ${order.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
            
            if (order.budget != null && order.budget > 0) {
                Text(
                    text = "Orçamento: R$ ${String.format("%.2f", order.budget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TaskGoGreen
                )
            }
            
            if (showActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, backgroundColor) = when (status.lowercase()) {
        "pending" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        "accepted" -> TaskGoGreen to TaskGoGreen.copy(alpha = 0.1f)
        "completed" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
        "cancelled" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        else -> TaskGoTextGray to TaskGoTextGray.copy(alpha = 0.1f)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = when (status.lowercase()) {
                "pending" -> "Pendente"
                "accepted" -> "Aceita"
                "completed" -> "Concluída"
                "cancelled" -> "Cancelada"
                else -> status
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDate(date: Date): String {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return format.format(date)
}

data class ServiceOrderItem(
    val id: String,
    val category: String,
    val details: String,
    val location: String,
    val budget: Double?,
    val dueDate: Date?,
    val status: String,
    val createdAt: Date?
)

