package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalDetailScreen(
    proposalId: String,
    onBackClick: () -> Unit,
    onAcceptProposal: (String) -> Unit,
    onRejectProposal: (String) -> Unit
) {
    // Dados vêm do Firestore - iniciar vazio
    val proposal = remember(proposalId) {
        null as Proposal?
    }
    
    // Se não há proposta, mostrar estado vazio
    if (proposal == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Proposta não encontrada",
                    color = TaskGoTextGray,
                    fontSize = 16.sp
                )
                Text(
                    text = "Esta proposta não existe ou foi removida",
                    color = TaskGoTextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Detalhes da Proposta",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card do prestador
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Avatar e informações do prestador
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = TaskGoGreen.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = proposal.providerName.split(" ").map { it.first() }.joinToString(""),
                                color = TaskGoGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = proposal.providerName,
                                color = TaskGoTextBlack,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Avaliação",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${proposal.providerRating} (100+ serviços)",
                                    color = TaskGoTextGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Card de informações do serviço
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Detalhes do Serviço",
                        color = TaskGoTextBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = proposal.serviceTitle,
                        color = TaskGoTextBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                        Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = proposal.serviceDescription,
                        color = TaskGoTextGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Card de informações da solicitação
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Informações da Solicitação",
                        color = TaskGoTextBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    InfoRow("Data solicitada:", proposal.date)
                    InfoRow("Local:", proposal.location)
                    InfoRow("Orçamento:", "R$ ${String.format("%.2f", proposal.budget)}")
                }
            }
            
            // Card de orçamento
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Orçamento",
                        color = TaskGoTextGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "R$ ${String.format("%.2f", proposal.budget)}",
                        color = TaskGoTextBlack,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (proposal.status == ProposalStatus.PENDING) {
                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRejectProposal(proposal.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC3545)
                        )
                    ) {
                        Text(
                            text = "Rejeitar Proposta",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = { onAcceptProposal(proposal.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Text(
                            text = "Aceitar Proposta",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Status da proposta
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Text(
                        text = when (proposal.status) {
                            ProposalStatus.ACCEPTED -> "Proposta Aceita"
                            ProposalStatus.REJECTED -> "Proposta Rejeitada"
                            else -> "Proposta Pendente"
                        },
                        color = when (proposal.status) {
                            ProposalStatus.ACCEPTED -> Color(0xFF155724)
                            ProposalStatus.REJECTED -> Color(0xFF721C24)
                            else -> Color(0xFF856404)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = TaskGoTextGray,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = TaskGoTextBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}