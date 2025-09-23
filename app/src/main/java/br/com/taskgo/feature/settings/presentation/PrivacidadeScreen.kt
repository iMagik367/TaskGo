package br.com.taskgo.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadeScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Privacidade",
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
            Text(
                text = "Política de Privacidade",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Última atualização: 15 de dezembro de 2024",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Esta Política de Privacidade descreve como o TaskGo coleta, usa e compartilha suas informações pessoais quando você usa nosso aplicativo.",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = "Informações que coletamos:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "• Nome e informações de contato\n• Informações de pagamento\n• Dados de localização\n• Histórico de serviços e produtos",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Como usamos suas informações:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "• Para fornecer nossos serviços\n• Para processar pagamentos\n• Para melhorar nossa plataforma\n• Para comunicação com você",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Compartilhamento de dados:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Não vendemos suas informações pessoais. Compartilhamos dados apenas com prestadores de serviços selecionados e quando exigido por lei.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
