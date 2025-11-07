package com.taskgoapp.taskgo.feature.services.presentation
import com.taskgoapp.taskgo.core.theme.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesServicoScreen(
    serviceId: String,
    onBackClick: () -> Unit
) {
    // Dados do serviço (em uma implementação real, viria de um ViewModel)
    val serviceData = when (serviceId) {
        "1" -> ServiceDetails(
            title = "Montagem de Móveis",
            providerName = "Rodrigo Silva",
            providerPhone = "(11) 99999-9999",
            providerEmail = "rodrigo@email.com",
            price = "R$ 150,00",
            completionDate = "15/12/2024",
            description = "Montagem profissional de móveis com garantia de 30 dias. Inclui desembalagem, montagem, ajustes e limpeza do local.",
            location = "São Paulo, SP",
            rating = 4.8,
            reviewCount = 156
        )
        "2" -> ServiceDetails(
            title = "Limpeza Residencial",
            providerName = "Maria Santos",
            providerPhone = "(11) 88888-8888",
            providerEmail = "maria@email.com",
            price = "R$ 80,00",
            completionDate = "10/12/2024",
            description = "Limpeza completa da residência incluindo todos os cômodos, banheiros, cozinha e áreas comuns.",
            location = "São Paulo, SP",
            rating = 4.6,
            reviewCount = 89
        )
        else -> ServiceDetails(
            title = "Serviço",
            providerName = "Prestador",
            providerPhone = "(11) 00000-0000",
            providerEmail = "prestador@email.com",
            price = "R$ 0,00",
            completionDate = "01/01/2024",
            description = "Descrição do serviço",
            location = "Local",
            rating = 5.0,
            reviewCount = 0
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Detalhes do Serviço",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Card principal do serviço
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título e preço
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serviceData.title,
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = serviceData.price,
                            style = FigmaPrice,
                            color = TaskGoPriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Status de conclusão
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = TaskGoSuccessGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "✓ Concluído em ${serviceData.completionDate}",
                            modifier = Modifier.padding(12.dp),
                            style = FigmaStatusText,
                            color = TaskGoSuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Descrição
                    Text(
                        text = "Descrição:",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = serviceData.description,
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
            }

            // Card do prestador
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Prestador do Serviço",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )

                    // Nome e avaliação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serviceData.providerName,
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = TaskGoStarYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${serviceData.rating} (${serviceData.reviewCount} avaliações)",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }

                    // Informações de contato
                    ContactInfoItem(
                        icon = Icons.Default.Call,
                        text = serviceData.providerPhone
                    )
                    ContactInfoItem(
                        icon = Icons.Default.Email,
                        text = serviceData.providerEmail
                    )
                    ContactInfoItem(
                        icon = Icons.Default.LocationOn,
                        text = serviceData.location
                    )
                }
            }

            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Implementar contato */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, TaskGoGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TaskGoGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contatar", style=FigmaButtonText, color=TaskGoGreen)
                }

                Button(
                    onClick = { /* Implementar nova solicitação */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    )
                ) {
                    Text("Solicitar Novamente", style=FigmaButtonText, color=TaskGoBackgroundWhite)
                }
            }
        }
    }
}

@Composable
private fun ContactInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TaskGoGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = FigmaProductDescription,
            color = TaskGoTextGray
        )
    }
}

data class ServiceDetails(
    val title: String,
    val providerName: String,
    val providerPhone: String,
    val providerEmail: String,
    val price: String,
    val completionDate: String,
    val description: String,
    val location: String,
    val rating: Double,
    val reviewCount: Int
)
