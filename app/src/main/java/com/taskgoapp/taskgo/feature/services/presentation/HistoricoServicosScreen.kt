package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoServicosScreen(
    onBackClick: () -> Unit,
    onVerDetalhes: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Histórico de Serviços",
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
            // Service history item
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Montagem de Móveis",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rodrigo Silva",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                        
                        Text(
                            text = "R$ 150,00",
                            style = FigmaPrice,
                            color = TaskGoPriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Concluído em 15/12/2024",
                            style = FigmaStatusText,
                            color = TaskGoTextGray
                        )
                        
                        Button(
                            onClick = { onVerDetalhes("1") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TaskGoGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TaskGoBackgroundWhite
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ver Detalhes",
                                style = FigmaButtonText,
                                color = TaskGoBackgroundWhite
                            )
                        }
                    }
                }
            }

            // Another service item
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Limpeza Residencial",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Maria Santos",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                        
                        Text(
                            text = "R$ 80,00",
                            style = FigmaPrice,
                            color = TaskGoPriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Concluído em 10/12/2024",
                            style = FigmaStatusText,
                            color = TaskGoTextGray
                        )
                        
                        Button(
                            onClick = { onVerDetalhes("2") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TaskGoGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TaskGoBackgroundWhite
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ver Detalhes",
                                style = FigmaButtonText,
                                color = TaskGoBackgroundWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
