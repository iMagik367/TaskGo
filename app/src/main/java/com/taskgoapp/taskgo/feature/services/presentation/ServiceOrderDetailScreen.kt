package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.CalendarToday
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
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceOrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onSendProposal: (String) -> Unit,
    viewModel: ServiceOrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Detalhes da Ordem",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        val errorMessage = uiState.error
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TaskGoGreen)
            }
        } else if (errorMessage != null) {
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
                            text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { viewModel.loadOrder(orderId) },
                        colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                    ) {
                        Text("Tentar Novamente")
                    }
                }
            }
        } else {
            val order = uiState.order
            if (order == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ordem não encontrada", color = TaskGoTextGray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card de Informações
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Título/Descrição
                            Text(
                                text = "Descrição",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextDark
                            )
                            Text(
                                text = order.details.takeIf { it.isNotBlank() } ?: "Sem descrição",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TaskGoTextGray
                            )
                            
                            HorizontalDivider()
                            
                            // Localização
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TaskGoGreen
                                )
                                Column {
                                    Text(
                                        text = "Localização",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TaskGoTextGray
                                    )
                                    Text(
                                        text = order.location.takeIf { it.isNotBlank() } ?: "Não informada",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = TaskGoTextDark
                                    )
                                }
                            }
                            
                            HorizontalDivider()
                            
                            // Orçamento
                            Column {
                                Text(
                                    text = "Orçamento",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray
                                )
                                Text(
                                    text = if (order.budget > 0) {
                                        currencyFormat.format(order.budget)
                                    } else {
                                        "A combinar"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoGreen
                                )
                            }
                            
                            // Data de vencimento
                            order.dueDate?.let { dueDate ->
                                HorizontalDivider()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = TaskGoTextGray
                                    )
                                    Column {
                                        Text(
                                            text = "Prazo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TaskGoTextGray
                                        )
                                        Text(
                                            text = dueDate,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = TaskGoTextDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Botão Enviar Orçamento
                    Button(
                        onClick = {
                            onSendProposal(order.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Enviar Orçamento",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

