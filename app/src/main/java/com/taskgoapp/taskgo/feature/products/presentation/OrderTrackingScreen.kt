package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.TrackingEvent
import com.taskgoapp.taskgo.core.model.OrderStatus
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.feature.products.presentation.OrderTrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(orderId) {
        viewModel.loadOrderTracking(orderId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Rastreamento de Pedido",
                subtitle = "Acompanhe seu pedido em tempo real",
                onBackClick = onNavigateBack,
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
                subtitleColor = Color.White,
                backIconColor = Color.White
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TaskGoGreen)
            }
        } else {
            val error = uiState.error
            val currentOrder = uiState.order
            
            if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Erro ao carregar rastreamento",
                            style = MaterialTheme.typography.titleMedium,
                            color = TaskGoError
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray
                        )
                        Button(onClick = { viewModel.loadOrderTracking(orderId) }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            } else if (currentOrder != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                // Order Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Pedido #${currentOrder.orderNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Status: ${getStatusText(currentOrder.status)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = getStatusColor(currentOrder.status)
                        )
                        if (uiState.trackingCode.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Código de rastreamento: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray
                                )
                                Text(
                                    text = uiState.trackingCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoGreen
                                )
                                if (uiState.carrier != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${uiState.carrier}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TaskGoTextGray
                                    )
                                }
                            }
                            if (uiState.trackingUrl != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { /* Abrir URL de rastreamento */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Abrir rastreamento completo")
                                }
                            }
                        }
                        if (uiState.isLocalDelivery && uiState.deliveryTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TaskGoGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Entrega local • Chegada: ${uiState.deliveryTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tracking Timeline
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Rastreamento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (uiState.trackingEvents.isNotEmpty()) {
                            uiState.trackingEvents.forEachIndexed { index, event ->
                                TrackingEventItem(
                                    event = event,
                                    isLast = index == uiState.trackingEvents.size - 1
                                )
                            }
                        } else {
                            DefaultTimeline(currentStatus = currentOrder.status)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Detalhes do Pedido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        currentOrder.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x Produto",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "R$ ${String.format("%.2f", item.price * item.quantity)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (item != currentOrder.items.lastOrNull()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
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
                                text = "R$ ${String.format("%.2f", currentOrder.total)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoGreen
                            )
                        }
                    }
                }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pedido não encontrado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TaskGoTextGray
                    )
                }
            }
        }
    }
}

private fun getStatusText(status: com.taskgoapp.taskgo.core.model.OrderStatus): String {
    return when (status) {
        com.taskgoapp.taskgo.core.model.OrderStatus.EM_ANDAMENTO -> "Em Andamento"
        com.taskgoapp.taskgo.core.model.OrderStatus.CONCLUIDO -> "Concluído"
        com.taskgoapp.taskgo.core.model.OrderStatus.CANCELADO -> "Cancelado"
    }
}

private fun getStatusColor(status: com.taskgoapp.taskgo.core.model.OrderStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        com.taskgoapp.taskgo.core.model.OrderStatus.EM_ANDAMENTO -> TaskGoWarning
        com.taskgoapp.taskgo.core.model.OrderStatus.CONCLUIDO -> TaskGoSuccessGreen
        com.taskgoapp.taskgo.core.model.OrderStatus.CANCELADO -> TaskGoError
    }
}

@Composable
private fun OrderHeaderCard(order: PurchaseOrder) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.order_tracking_order_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pedido #${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "ID: ${order.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Cart),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingTimelineCard(
    trackingEvents: List<TrackingEvent>,
    currentStatus: OrderStatus
) {
    Card(
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
                text = stringResource(R.string.order_tracking_timeline_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trackingEvents.isNotEmpty()) {
                trackingEvents.forEachIndexed { index, event ->
                    TrackingEventItem(
                        event = event,
                        isLast = index == trackingEvents.size - 1
                    )
                }
            } else {
                // Default timeline based on current status
                DefaultTimeline(currentStatus = currentStatus)
            }
        }
    }
}

@Composable
private fun TrackingEventItem(
    event: TrackingEvent,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline line and dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Event content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = event.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (event.done) TaskGoTextBlack else TaskGoTextGray
            )
            
            Text(
                text = formatTrackingDate(event.date),
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray
            )
        }
    }
}

@Composable
private fun DefaultTimeline(currentStatus: OrderStatus) {
    val timelineSteps = listOf(
        TrackingStep(
            title = "Pedido Criado",
            description = "Seu pedido foi criado e está aguardando pagamento",
            isCompleted = true,
            icon = TGIcons.Edit
        ),
        TrackingStep(
            title = "Pagamento Confirmado",
            description = "Pagamento confirmado, aguardando preparo",
            isCompleted = currentStatus != OrderStatus.EM_ANDAMENTO,
            icon = TGIcons.Services
        ),
        TrackingStep(
            title = "Pedido Enviado",
            description = "Seu pedido foi enviado e está a caminho",
            isCompleted = currentStatus == OrderStatus.CONCLUIDO,
            icon = TGIcons.Cart
        ),
        TrackingStep(
            title = "Pedido Entregue",
            description = "Seu pedido foi entregue com sucesso",
            isCompleted = currentStatus == OrderStatus.CONCLUIDO,
            icon = TGIcons.Check
        )
    )
    
    timelineSteps.forEachIndexed { index, step ->
        TrackingStepItem(
            step = step,
            isLast = index == timelineSteps.size - 1
        )
    }
}

@Composable
private fun TrackingStepItem(
    step: TrackingStep,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline line and dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (step.isCompleted) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
            )
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(
                            color = if (step.isCompleted) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Step content
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(step.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (step.isCompleted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (step.isCompleted) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsCard(order: PurchaseOrder) {
    Card(
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
                text = stringResource(R.string.order_tracking_details_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Order items
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.quantity}x Produto",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "R$ ${String.format("%.2f", item.price * item.quantity)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (item != order.items.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.order_tracking_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "R$ ${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class TrackingStep(
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val icon: Int
)

private fun formatTrackingDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(timestamp))
}


