package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBackClick: () -> Unit
) {
    var locationSharingEnabled by remember { mutableStateOf(true) }
    var profileVisibilityEnabled by remember { mutableStateOf(true) }
    var contactInfoSharingEnabled by remember { mutableStateOf(false) }
    var analyticsEnabled by remember { mutableStateOf(true) }
    var personalizedAdsEnabled by remember { mutableStateOf(false) }
    var dataCollectionEnabled by remember { mutableStateOf(true) }
    var thirdPartySharingEnabled by remember { mutableStateOf(false) }
    
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Overview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundGray
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Sua Privacidade é Importante",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Controle como suas informações são usadas e compartilhadas no TaskGo. Você pode alterar essas configurações a qualquer momento.",
                        style = FigmaProductDescription,
                        color = TaskGoTextBlack
                    )
                }
            }
            
            // Location Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Localização",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Compartilhar Localização",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Permitir que prestadores vejam sua localização para serviços próximos",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = locationSharingEnabled,
                            onCheckedChange = { locationSharingEnabled = it }
                        )
                    }
                    
                    if (locationSharingEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = { /* TODO: Implementar configurações de localização */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurações de Localização")
                        }
                    }
                }
            }
            
            // Profile Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Perfil e Visibilidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Perfil Público",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Permitir que outros usuários vejam seu perfil",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = profileVisibilityEnabled,
                            onCheckedChange = { profileVisibilityEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Compartilhar Contato",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Permitir que prestadores vejam seu telefone e e-mail",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = contactInfoSharingEnabled,
                            onCheckedChange = { contactInfoSharingEnabled = it }
                        )
                    }
                }
            }
            
            // Data Collection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Coleta de Dados",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Analytics e Métricas",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Coletar dados para melhorar o aplicativo",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = analyticsEnabled,
                            onCheckedChange = { analyticsEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Coleta de Dados",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Armazenar informações sobre seu uso do aplicativo",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = dataCollectionEnabled,
                            onCheckedChange = { dataCollectionEnabled = it }
                        )
                    }
                }
            }
            
            // Advertising
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Publicidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Publicidade Personalizada",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Mostrar anúncios baseados em seus interesses",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = personalizedAdsEnabled,
                            onCheckedChange = { personalizedAdsEnabled = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Compartilhamento com Terceiros",
                                    style = FigmaProductName,
                                    color = TaskGoTextBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Permitir que parceiros acessem dados para publicidade",
                                    style = FigmaStatusText,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                        
                        Switch(
                            checked = thirdPartySharingEnabled,
                            onCheckedChange = { thirdPartySharingEnabled = it }
                        )
                    }
                }
            }
            
            // Privacy Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ações de Privacidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { /* TODO: Implementar download de dados */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Baixar Meus Dados")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { /* TODO: Implementar exclusão de dados */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Excluir Meus Dados")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { /* TODO: Implementar política de privacidade */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Política de Privacidade")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { /* TODO: Implementar termos de uso */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Termos de Uso")
                    }
                }
            }
            
            // Privacy Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurfaceGray
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status da Privacidade",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val privacyScore = calculatePrivacyScore(
                        locationSharingEnabled,
                        profileVisibilityEnabled,
                        contactInfoSharingEnabled,
                        analyticsEnabled,
                        personalizedAdsEnabled,
                        dataCollectionEnabled,
                        thirdPartySharingEnabled
                    )
                    
                    Text(
                        text = "Sua privacidade está ${getPrivacyStatus(privacyScore)}",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = getPrivacyColor(privacyScore),
                        progress = { privacyScore / 100f }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "$privacyScore%",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun calculatePrivacyScore(
    locationSharing: Boolean,
    profileVisibility: Boolean,
    contactInfoSharing: Boolean,
    analytics: Boolean,
    personalizedAds: Boolean,
    dataCollection: Boolean,
    thirdPartySharing: Boolean
): Int {
    var score = 100
    
    if (locationSharing) score -= 15
    if (profileVisibility) score -= 10
    if (contactInfoSharing) score -= 20
    if (analytics) score -= 10
    if (personalizedAds) score -= 15
    if (dataCollection) score -= 10
    if (thirdPartySharing) score -= 20
    
    return maxOf(score, 0)
}

private fun getPrivacyStatus(score: Int): String {
    return when {
        score >= 80 -> "muito bem protegida"
        score >= 60 -> "bem protegida"
        score >= 40 -> "moderadamente protegida"
        score >= 20 -> "pouco protegida"
        else -> "muito exposta"
    }
}

private fun getPrivacyColor(score: Int): Color {
    return when {
        score >= 80 -> TaskGoSuccessGreen
        score >= 60 -> Color(0xFF8BC34A)
        score >= 40 -> TaskGoAmber
        score >= 20 -> TaskGoOrange
        else -> Color(0xFFF44336)
    }
}




