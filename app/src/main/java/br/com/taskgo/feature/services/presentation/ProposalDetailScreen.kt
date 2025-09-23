package br.com.taskgo.taskgo.feature.services.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalDetailScreen(
    proposalId: Long,
    onBackClick: () -> Unit,
    onProposalAccepted: (Long) -> Unit
) {
    val proposal = remember(proposalId) {
        ProposalUi(
            id = proposalId,
            title = if (proposalId == 2L) "Limpeza Residencial" else "Montagem de Móveis",
            description = if (proposalId == 2L) "Limpeza completa da casa" else "Montagem de guarda-roupa e cama",
            price = if (proposalId == 2L) 80.0 else 150.0,
            providerName = if (proposalId == 2L) "Maria Santos" else "Rodrigo Silva",
            providerProfession = if (proposalId == 2L) "Serviços de Limpeza" else "Montador de Móveis"
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Detalhes da Proposta",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = proposal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = proposal.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "R$ %.2f".format(proposal.price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Informações do Prestador", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = proposal.providerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(text = proposal.providerProfession, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Recusar", onClick = onBackClick, modifier = Modifier.weight(1f))
                PrimaryButton(text = "Aceitar", onClick = { onProposalAccepted(proposal.id) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class ProposalUi(
    val id: Long,
    val title: String,
    val description: String,
    val price: Double,
    val providerName: String,
    val providerProfession: String
)


