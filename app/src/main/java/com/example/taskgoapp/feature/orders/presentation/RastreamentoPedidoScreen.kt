package com.example.taskgoapp.feature.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar
import com.example.taskgoapp.core.model.TrackingEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RastreamentoPedidoScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onVerDetalhes: (String) -> Unit
) {
    // Dados de exemplo do rastreamento
    val trackingEvents = remember(orderId) {
        listOf(
            TrackingEvent(
                label = "Postado",
                date = System.currentTimeMillis() - 172800000, // 2 dias atrás
                done = true
            ),
            TrackingEvent(
                label = "Em trânsito",
                date = System.currentTimeMillis() - 86400000, // 1 dia atrás
                done = true
            ),
            TrackingEvent(
                label = "Saiu para entrega",
                date = System.currentTimeMillis() - 3600000, // 1 hora atrás
                done = true
            ),
            TrackingEvent(
                label = "Entregue",
                date = System.currentTimeMillis(),
                done = true
            )
        )
    }
    
    val productName = "Furadeira sem Fio"
    val trackingCode = "LP123456789BR"
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Rastreamento do pedido",
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
            // Product Info
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
                        Text(
                            text = productName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = trackingCode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Current Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Entregue",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Entregue",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            // Tracking Timeline
            item {
                Text(
                    text = "Histórico de Rastreamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(trackingEvents) { event ->
                TrackingEventItem(
                    event = event,
                    isLast = event == trackingEvents.last()
                )
            }
            
            // Action Button
            item {
                Button(
                    onClick = { onVerDetalhes(orderId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Ver detalhes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingEventItem(
    event: TrackingEvent,
    isLast: Boolean
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault())
    val eventDate = dateFormat.format(Date(event.date))
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circle indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (event.done) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (event.done) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Concluído",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            
            // Vertical line (except for last item)
            if (!isLast) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Event details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = event.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (event.done) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = eventDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
