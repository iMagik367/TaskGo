package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

data class Proposal(
    val id: String,
    val providerName: String,
    val providerRating: Double,
    val serviceTitle: String,
    val serviceDescription: String,
    val budget: Double,
    val location: String,
    val date: String,
    val status: ProposalStatus
)

enum class ProposalStatus {
    PENDING, ACCEPTED, REJECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalsReceivedScreen(
    onBackClick: () -> Unit,
    onProposalClick: (String) -> Unit,
    onAcceptProposal: (String) -> Unit,
    onRejectProposal: (String) -> Unit
) {
    // Lista vazia - dados vêm do Firestore
    val proposals = remember { emptyList<Proposal>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoBackgroundWhite)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Propostas Recebidas",
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

        // Lista de propostas
        if (proposals.isEmpty()) {
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
                        text = "Nenhuma proposta recebida",
                        color = TaskGoTextGray,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Quando você receber propostas, elas aparecerão aqui",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(proposals) { proposal ->
                ProposalCard(
                    proposal = proposal,
                    onProposalClick = { onProposalClick(proposal.id) },
                    onAcceptProposal = { onAcceptProposal(proposal.id) },
                    onRejectProposal = { onRejectProposal(proposal.id) }
                )
                }
            }
        }
    }
}

@Composable
fun ProposalCard(
    proposal: Proposal,
    onProposalClick: () -> Unit,
    onAcceptProposal: () -> Unit,
    onRejectProposal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProposalClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header com nome e avaliação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = proposal.providerName,
                        color = TaskGoTextBlack,
                        fontSize = 16.sp,
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
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Status
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (proposal.status) {
                            ProposalStatus.PENDING -> Color(0xFFFFF3CD)
                            ProposalStatus.ACCEPTED -> Color(0xFFD4EDDA)
                            ProposalStatus.REJECTED -> Color(0xFFF8D7DA)
                        }
                    )
                ) {
                    Text(
                        text = when (proposal.status) {
                            ProposalStatus.PENDING -> "Pendente"
                            ProposalStatus.ACCEPTED -> "Aceita"
                            ProposalStatus.REJECTED -> "Rejeitada"
                        },
                        color = when (proposal.status) {
                            ProposalStatus.PENDING -> Color(0xFF856404)
                            ProposalStatus.ACCEPTED -> Color(0xFF155724)
                            ProposalStatus.REJECTED -> Color(0xFF721C24)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Título do serviço
            Text(
                text = proposal.serviceTitle,
                color = TaskGoTextBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Descrição
            Text(
                text = proposal.serviceDescription,
                color = TaskGoTextGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Informações adicionais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Data solicitada: ${proposal.date}",
                        color = TaskGoTextGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Local: ${proposal.location}",
                        color = TaskGoTextGray,
                        fontSize = 10.sp
                    )
                }
                
                Text(
                    text = "R$ ${String.format("%.2f", proposal.budget)}",
                    color = TaskGoGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (proposal.status == ProposalStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRejectProposal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC3545)
                        )
                    ) {
                        Text(
                            text = "Rejeitar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = onAcceptProposal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Text(
                            text = "Aceitar",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
