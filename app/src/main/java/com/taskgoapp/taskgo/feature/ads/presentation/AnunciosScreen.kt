package com.taskgoapp.taskgo.feature.ads.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosScreen(
    onBackClick: () -> Unit,
    onComprarBanner: () -> Unit,
    onVerDetalhe: () -> Unit,
    variant: String? = null
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
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TaskGoGreen
            )

            // Headline
            Text(
                text = "Divulgue seu trabalho!",
                style = FigmaTitleLarge,
                color = TaskGoTextBlack
            )

            // Description
            Text(
                text = "Destaque seus serviços nos banners da página inicial de TaskGo",
                style = FigmaProductDescription,
                color = TaskGoTextGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Pricing cards
            if (variant == "empty") {
                Text(
                    text = "Nenhum plano disponível no momento",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else Row(
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
                            style = FigmaProductName,
                            color = TaskGoTextBlack
                        )
                        Text(
                            text = "R$ 50/dia",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
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
                            style = FigmaProductName,
                            color = TaskGoTextBlack
                        )
                        Text(
                            text = "R$ 90/dia",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buy button
            Button(
                onClick = { onComprarBanner() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                )
            ) {
                Text(
                    text = "Comprar Banner",
                    style = FigmaButtonText,
                    color = Color.White
                )
            }

            if (variant == "cta_secondary") {
                OutlinedButton(
                    onClick = onVerDetalhe,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver detalhes")
                }
            }
        }
    }
}
