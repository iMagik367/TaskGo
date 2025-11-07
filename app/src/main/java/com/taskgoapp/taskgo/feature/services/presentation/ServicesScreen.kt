package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun ServicesScreen(
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateToCreateWorkOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Serviços",
            style = FigmaTitleLarge,
            color = TaskGoTextBlack
        )
        
        Text(
            text = "Encontre prestadores de serviços para suas necessidades",
            style = FigmaProductDescription,
            color = TaskGoTextGray
        )
        
        // Lista de serviços disponíveis - dados vêm do backend
        val viewModel: ServicesViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val serviceOrders by viewModel.serviceOrders.collectAsState()
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Text(
                text = uiState.error ?: "Erro ao carregar serviços",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(serviceOrders) { serviceOrder ->
                    ServiceCard(
                        title = serviceOrder.category,
                        description = serviceOrder.description,
                        price = "Ver proposta", // ServiceOrder não tem preço, apenas Proposals têm
                        rating = 0f, // ServiceOrder não tem rating
                        onServiceClick = { onNavigateToServiceDetail(serviceOrder.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    title: String,
    description: String,
    price: String,
    rating: Float,
    onServiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onServiceClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaskGoTextDark
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = TaskGoTextDark.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoGreen
                )
                
                Text(
                    text = "⭐ $rating",
                    fontSize = 14.sp,
                    color = TaskGoTextDark.copy(alpha = 0.7f)
                )
            }
        }
    }
}