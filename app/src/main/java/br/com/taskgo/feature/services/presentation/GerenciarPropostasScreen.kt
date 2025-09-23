package br.com.taskgo.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarPropostasScreen(
    onBackClick: () -> Unit,
    onVerProposta: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gerenciar Propostas",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProposalItem(initials = "RS", titulo = "Montagem de Móveis", pessoa = "Rodrigo Silva", preco = "R$ 150,00", status = "Pendente", onClickVer = { onVerProposta("1") })
            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ProposalItem(initials = "MS", titulo = "Limpeza Residencial", pessoa = "Maria Santos", preco = "R$ 80,00", status = "Aceita", onClickVer = { onVerProposta("2") })
        }
    }
}

@Composable
private fun ProposalItem(
    initials: String,
    titulo: String,
    pessoa: String,
    preco: String,
    status: String,
    onClickVer: () -> Unit
) {
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
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(text = pessoa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = preco, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(text = status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        OutlinedButton(onClick = onClickVer) { Text("Ver") }
    }
}
