package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
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
fun MeusServicosScreen(
    onBackClick: () -> Unit,
    onCriarServico: () -> Unit,
    onEditarServico: (String) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Serviços",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCriarServico,
                containerColor = TaskGoGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Criar Serviço",
                    tint = TaskGoBackgroundWhite
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service list
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Service icon placeholder
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = TaskGoBackgroundGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔧",
                            style = FigmaSectionTitle,
                            color = TaskGoTextBlack
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Service details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Montagem de Móveis",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "R$ 150,00",
                            style = FigmaPrice,
                            color = TaskGoPriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ativo",
                            style = FigmaStatusText,
                            color = TaskGoSuccess
                        )
                    }

                    // Action buttons
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        IconButton(
                            onClick = { onEditarServico("1") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = TaskGoGreen
                            )
                        }
                        IconButton(
                            onClick = { /* TODO: Deletar serviço */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Deletar",
                                tint = TaskGoError
                            )
                        }
                    }
                }
            }

            // Another service
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = TaskGoBackgroundGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🧹",
                            style = FigmaSectionTitle,
                            color = TaskGoTextBlack
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Limpeza Residencial",
                            style = FigmaProductName,
                            color = TaskGoTextBlack,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "R$ 80,00",
                            style = FigmaPrice,
                            color = TaskGoPriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ativo",
                            style = FigmaStatusText,
                            color = TaskGoSuccess
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        IconButton(
                            onClick = { onEditarServico("2") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = TaskGoGreen
                            )
                        }
                        IconButton(
                            onClick = { /* TODO: Deletar serviço */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Deletar",
                                tint = TaskGoError
                            )
                        }
                    }
                }
            }
        }
    }
}
