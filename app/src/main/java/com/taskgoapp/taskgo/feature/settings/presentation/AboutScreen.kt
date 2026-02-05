package com.taskgoapp.taskgo.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTermsOfService: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Sobre",
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
            // App Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Icon
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = MaterialTheme.shapes.large,
                        color = TaskGoTextBlack
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TaskGoGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "TaskGo",
                        style = FigmaTitleLarge,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Versão 1.0.0",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Conectando você aos melhores prestadores de serviços",
                        style = FigmaProductDescription,
                        color = TaskGoTextBlack,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // App Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Sobre o TaskGo",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "O TaskGo é uma plataforma inovadora que conecta clientes a prestadores de serviços qualificados e confiáveis. Nossa missão é simplificar a contratação de serviços, oferecendo uma experiência segura, transparente e eficiente.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Com o TaskGo, você pode encontrar profissionais para diversos tipos de serviços, desde reformas e manutenções até serviços especializados, tudo com avaliações verificadas e garantia de qualidade.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
            }
            
            // Features
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Principais Funcionalidades",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val features = listOf(
                        "Busca inteligente de prestadores",
                        "Sistema de avaliações e comentários",
                        "Chat integrado para comunicação",
                        "Pagamentos seguros (Pix, cartões)",
                        "Rastreamento de serviços em tempo real",
                        "Histórico completo de transações",
                        "Suporte 24/7 via IA e humanos"
                    )
                    
                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TaskGoGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feature,
                                style = FigmaProductDescription,
                                color = TaskGoTextBlack
                            )
                        }
                    }
                }
            }
            
            // Company Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informações da Empresa",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Company Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Empresa",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "TaskGo Tecnologia Ltda.",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // CNPJ
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CNPJ",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "12.345.678/0001-90",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Address
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
                                text = "Endereço",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Rua das Tecnologias, 123\nBairro da Inovação\nSão Paulo - SP, 01234-567",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Website
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Website",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "www.taskgo.com.br",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
            
            // Contact Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Contato",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "E-mail",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "contato@taskgo.com.br",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Phone
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Telefone",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "(11) 3000-0000",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Support
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Support,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Suporte",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "suporte@taskgo.com.br",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
            
            // Legal Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informações Legais",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToPrivacyPolicy,
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
                        onClick = onNavigateToTermsOfService,
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onNavigateToPrivacyPolicy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LGPD - Lei Geral de Proteção de Dados")
                    }
                }
            }
            
            // Development Team
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Equipe de Desenvolvimento",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "O TaskGo foi desenvolvido por uma equipe dedicada de profissionais apaixonados por tecnologia e inovação. Nossa equipe trabalha constantemente para melhorar a experiência do usuário e adicionar novas funcionalidades.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { /* TODO: Implementar página da equipe */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Conheça Nossa Equipe")
                    }
                }
            }
            
            // App Statistics
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
                        text = "Estatísticas do App",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "10K+",
                                style = FigmaPrice,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoPriceGreen
                            )
                            Text(
                                text = "Usuários Ativos",
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "5K+",
                                style = FigmaPrice,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoPriceGreen
                            )
                            Text(
                                text = "Prestadores",
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "50K+",
                                style = FigmaPrice,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoPriceGreen
                            )
                            Text(
                                text = "Serviços",
                                style = FigmaStatusText,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
            
            // Copyright
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
                        text = "© 2024 TaskGo Tecnologia Ltda.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                    
                    Text(
                        text = "Todos os direitos reservados.",
                        style = FigmaStatusText,
                        color = TaskGoTextGray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Made with ❤️ in Brazil",
                        style = FigmaStatusText,
                        color = TaskGoTextGray
                    )
                }
            }
        }
    }
}




