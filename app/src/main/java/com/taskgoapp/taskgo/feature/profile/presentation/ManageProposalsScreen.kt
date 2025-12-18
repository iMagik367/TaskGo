package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar

@Composable
fun ManageProposalsScreen(
    onNavigateBack: () -> Unit
) {
    val proposals = listOf(
        ProposalItem(
            id = 1,
            serviceTitle = "Montagem de Móveis",
            providerName = "João Silva",
            price = 150.00,
            status = "Pendente",
            date = "15/12/2024",
            description = "Montagem de guarda-roupa e cama"
        ),
        ProposalItem(
            id = 2,
            serviceTitle = "Limpeza Residencial",
            providerName = "Maria Santos",
            price = 80.00,
            status = "Aceita",
            date = "10/12/2024",
            description = "Limpeza completa da casa"
        ),
        ProposalItem(
            id = 3,
            serviceTitle = "Instalação Elétrica",
            providerName = "Pedro Costa",
            price = 200.00,
            status = "Recusada",
            date = "05/12/2024",
            description = "Instalação de tomadas e interruptores"
        )
    )
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gerenciar Propostas",
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
                    text = "Minhas Propostas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            items(proposals) { proposal ->
                Card(
                    modifier = Modifier.fillMaxWidth()
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
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = proposal.serviceTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prestador: ${proposal.providerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(proposal.status) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = proposal.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Data: ${proposal.date}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Valor: R$ %.2f".format(proposal.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row {
                                IconButton(onClick = { /* Ver detalhes */ }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Ver detalhes")
                                }
                                if (proposal.status == "Pendente") {
                                    IconButton(onClick = { /* Aceitar */ }) {
                                        Icon(Icons.Default.Check, contentDescription = "Aceitar")
                                    }
                                    IconButton(onClick = { /* Recusar */ }) {
                                        Icon(Icons.Default.Close, contentDescription = "Recusar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (proposals.isEmpty()) {
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
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nenhuma proposta encontrada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Solicite seu primeiro serviço",
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

data class ProposalItem(
    val id: Int,
    val serviceTitle: String,
    val providerName: String,
    val price: Double,
    val status: String,
    val date: String,
    val description: String
)
