package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoBorder

data class ServiceHistoryItem(
    val id: String,
    val serviceTitle: String,
    val providerName: String,
    val clientLocation: String,
    val serviceDate: String,
    val status: ServiceStatus,
    val rating: Double? = null
)

enum class ServiceStatus {
    COMPLETED, IN_PROGRESS, CANCELLED, PENDING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    onBackClick: () -> Unit,
    onServiceClick: (String) -> Unit,
    onRateService: (String) -> Unit
) {
    // Lista vazia - dados vêm do Firestore
    val serviceHistory = remember { emptyList<ServiceHistoryItem>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Histórico de Serviços",
                    color = TaskGoTextBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TaskGoTextBlack
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = TaskGoBackgroundWhite
            )
        )

        // Lista de serviços
        if (serviceHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nenhum histórico de serviços",
                        color = TaskGoTextGray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Quando você completar serviços, eles aparecerão aqui",
                        color = TaskGoTextGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(serviceHistory) { service ->
                    ServiceHistoryCard(
                        service = service,
                        onServiceClick = { onServiceClick(service.id) },
                        onRateService = { onRateService(service.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceHistoryCard(
    service: ServiceHistoryItem,
    onServiceClick: () -> Unit,
    onRateService: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onServiceClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header com status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = service.serviceTitle,
                    color = TaskGoTextBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Status badge
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Text(
                        text = when (service.status) {
                            ServiceStatus.COMPLETED -> "Concluído"
                            ServiceStatus.IN_PROGRESS -> "Em Andamento"
                            ServiceStatus.CANCELLED -> "Cancelado"
                            ServiceStatus.PENDING -> "Pendente"
                        },
                        color = when (service.status) {
                            ServiceStatus.COMPLETED -> Color(0xFF155724)
                            ServiceStatus.IN_PROGRESS -> Color(0xFF856404)
                            ServiceStatus.CANCELLED -> Color(0xFF721C24)
                            ServiceStatus.PENDING -> Color(0xFF6C757D)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Prestador
            Text(
                text = "Prestador: ${service.providerName}",
                color = TaskGoTextGray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Localização
            Text(
                text = "Cliente: ${service.clientLocation}",
                color = TaskGoTextGray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Data
            Text(
                text = "Data: ${service.serviceDate}",
                color = TaskGoTextGray,
                fontSize = 12.sp
            )
            
            if (service.status == ServiceStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Avaliação
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Avaliação: ",
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                    
                    if (service.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Estrela",
                                    tint = if (index < service.rating.toInt()) Color(0xFFFFD700) else Color(0xFFE0E0E0),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${service.rating}",
                                color = TaskGoTextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onRateService,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = TaskGoGreen
                            )
                        ) {
                            Text(
                                text = "Avaliar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
