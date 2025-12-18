package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarPropostasScreen(
    onBackClick: () -> Unit,
    onVerProposta: (String) -> Unit,
    viewModel: GerenciarPropostasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gerenciar Propostas",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error ?: "Erro ao carregar propostas",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Tentar novamente")
                    }
                }
            }
            uiState.proposals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma proposta recebida",
                        color = TaskGoTextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.proposals,
                        key = { it.proposalId }
                    ) { proposal ->
                        ProposalItem(
                            proposal = proposal,
                            onClickVer = { onVerProposta(proposal.proposalId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalItem(
    proposal: ProposalItemUi,
    onClickVer: () -> Unit
) {
    val initials = proposal.providerName
        .split(" ")
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .take(2)

    val priceFormatted = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(proposal.price)

    val statusColor = when (proposal.status.lowercase()) {
        "pendente" -> TaskGoGreen
        "aceita" -> Color(0xFF4CAF50)
        "recusada" -> Color(0xFFF44336)
        else -> TaskGoTextGray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickVer() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = TaskGoBackgroundGray,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = FigmaProductName,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = proposal.serviceTitle,
                style = FigmaProductName,
                color = TaskGoTextBlack,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = proposal.providerName,
                style = FigmaProductDescription,
                color = TaskGoTextGray
            )
            Text(
                text = priceFormatted,
                style = FigmaPrice,
                color = TaskGoPriceGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = proposal.status,
                style = FigmaStatusText,
                color = statusColor
            )
        }

        OutlinedButton(
            onClick = onClickVer,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, TaskGoGreen)
        ) {
            Text("Ver", style = FigmaButtonText, color = TaskGoGreen)
        }
    }
}
