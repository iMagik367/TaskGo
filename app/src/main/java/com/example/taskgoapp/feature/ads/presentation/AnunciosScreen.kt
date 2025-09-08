package com.example.taskgoapp.feature.ads.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.core.design.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosScreen(
    onBackClick: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf("Pequeno") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Anúncio",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Megaphone icon
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Headline
            Text(
                text = "Divulgue seu trabalho!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Description
            Text(
                text = "Destaque seus serviços nos banners da página inicial de TaskGo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Pricing cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pequeno card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == "Pequeno")
                            MaterialTheme.colorScheme.primaryContainer
                        else Color.White
                    ),
                    border = if (selectedPlan == "Pequeno") null else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pequeno",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "R$ 50/dia",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grande card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == "Grande")
                            MaterialTheme.colorScheme.primaryContainer
                        else Color.White
                    ),
                    border = if (selectedPlan == "Grande") null else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Grande",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "R$ 90/dia",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buy button
            Button(
                onClick = { /* TODO: Implementar compra */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Comprar Banner",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
