package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoBorder

@Composable
fun MyServicesScreen(
    onNavigateBack: () -> Unit
) {
    val services = listOf(
        ServiceItem(
            id = 1,
            title = "Montagem de Móveis",
            provider = "João Silva",
            date = "15/12/2024",
            status = "Em andamento",
            price = 150.00
        ),
        ServiceItem(
            id = 2,
            title = "Limpeza Residencial",
            provider = "Maria Santos",
            date = "10/12/2024",
            status = "Concluído",
            price = 80.00
        ),
        ServiceItem(
            id = 3,
            title = "Instalação Elétrica",
            provider = "Pedro Costa",
            date = "05/12/2024",
            status = "Concluído",
            price = 200.00
        )
    )
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Serviços",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Histórico de Serviços",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            items(services) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = service.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prestador: ${service.provider}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(service.status) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Data: ${service.date}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Valor: R$ %.2f".format(service.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row {
                                IconButton(onClick = { /* Ver detalhes */ }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver detalhes")
                                }
                                IconButton(onClick = { /* Avaliar */ }) {
                                    Icon(Icons.Default.Star, contentDescription = "Avaliar")
                                }
                            }
                        }
                    }
                }
            }
            
            if (services.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nenhum serviço encontrado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Contrate seu primeiro serviço",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ServiceItem(
    val id: Int,
    val title: String,
    val provider: String,
    val date: String,
    val status: String,
    val price: Double
)
