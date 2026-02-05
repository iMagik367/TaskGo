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
fun SupportScreen(
    onBackClick: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showContactDialog by remember { mutableStateOf(false) }
    
    val supportCategories = remember {
        listOf(
            SupportCategory(
                id = "account",
                title = "Conta e Login",
                description = "Problemas com login, cadastro ou configurações da conta",
                icon = Icons.Default.Person
            ),
            SupportCategory(
                id = "services",
                title = "Serviços",
                description = "Dúvidas sobre contratação, propostas ou prestadores",
                icon = Icons.Default.Build
            ),
            SupportCategory(
                id = "products",
                title = "Produtos e Compras",
                description = "Problemas com produtos, carrinho ou pagamentos",
                icon = Icons.Default.ShoppingCart
            ),
            SupportCategory(
                id = "payments",
                title = "Pagamentos",
                description = "Problemas com cartões, Pix ou reembolsos",
                icon = Icons.Default.Payment
            ),
            SupportCategory(
                id = "technical",
                title = "Problemas Técnicos",
                description = "Erros, travamentos ou problemas de funcionamento",
                icon = Icons.Default.BugReport
            ),
            SupportCategory(
                id = "other",
                title = "Outros",
                description = "Outras dúvidas ou solicitações",
                icon = Icons.Default.Help
            )
        )
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Suporte",
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
            // Support Header
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
                    Icon(
                        imageVector = Icons.Default.Support,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TaskGoTextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Central de Suporte",
                        style = FigmaTitleLarge,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Estamos aqui para ajudar! Escolha uma categoria ou entre em contato conosco.",
                        style = FigmaProductDescription,
                        color = TaskGoTextBlack,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Quick Actions
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
                        text = "Ações Rápidas",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: Implementar FAQ */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FAQ")
                        }
                        
                        OutlinedButton(
                            onClick = { /* TODO: Implementar tutoriais */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tutoriais")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: Implementar status do sistema */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Status")
                        }
                        
                        OutlinedButton(
                            onClick = { showContactDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactSupport,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contato")
                        }
                    }
                }
            }
            
            // Support Categories
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
                        text = "Categorias de Suporte",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    supportCategories.forEach { category ->
                        SupportCategoryItem(
                            category = category,
                            isSelected = selectedCategory == category.id,
                            onCategorySelected = { selectedCategory = category.id }
                        )
                        
                        if (category != supportCategories.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            // Contact Form
            if (selectedCategory.isNotEmpty()) {
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
                            text = "Enviar Mensagem",
                            style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Descreva seu problema ou dúvida") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 6
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    selectedCategory = ""
                                    message = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            
                            PrimaryButton(
                                text = "Enviar",
                                onClick = { /* TODO: Implementar envio da mensagem */ },
                                modifier = Modifier.weight(1f),
                                enabled = message.isNotBlank()
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
                        text = "Informações de Contato",
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
                                text = "suporte@taskgo.com.br",
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
                                text = "0800 123 4567",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // WhatsApp
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = TaskGoGreen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WhatsApp",
                                style = FigmaProductName,
                                color = TaskGoTextBlack,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "(11) 99999-9999",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Horário de Atendimento: Segunda a Sexta, das 8h às 18h",
                        style = FigmaStatusText,
                        color = TaskGoTextGray
                    )
                }
            }
            
            // Social Media
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoBackgroundWhite
                ),
                border = BorderStroke(1.dp, TaskGoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Siga-nos nas Redes Sociais",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: Implementar link para Facebook */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Facebook")
                        }
                        
                        OutlinedButton(
                            onClick = { /* TODO: Implementar link para Instagram */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instagram")
                        }
                        
                        OutlinedButton(
                            onClick = { /* TODO: Implementar link para LinkedIn */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LinkedIn")
                        }
                    }
                }
            }
        }
    }
    
    // Contact Dialog
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Entre em Contato") },
            text = { 
                Text("Escolha a melhor forma de entrar em contato conosco:")
            },
            confirmButton = {
                TextButton(
                    onClick = { showContactDialog = false }
                ) {
                    Text("Fechar")
                }
            },
            dismissButton = {
                Column {
                    TextButton(
                        onClick = { 
                            /* TODO: Implementar e-mail */
                            showContactDialog = false 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("E-mail")
                    }
                    
                    TextButton(
                        onClick = { 
                            /* TODO: Implementar WhatsApp */
                            showContactDialog = false 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp")
                    }
                    
                    TextButton(
                        onClick = { 
                            /* TODO: Implementar telefone */
                            showContactDialog = false 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Telefone")
                    }
                }
            }
        )
    }
}

@Composable
private fun SupportCategoryItem(
    category: SupportCategory,
    isSelected: Boolean,
    onCategorySelected: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) 
                    TaskGoTextBlack 
                else 
                    TaskGoGreen
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.title,
                    style = FigmaProductName,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) 
                        TaskGoTextBlack 
                    else 
                        TaskGoTextGray
                )
                
                Text(
                    text = category.description,
                    style = FigmaStatusText,
                    color = if (isSelected) 
                        TaskGoTextBlack 
                    else 
                        TaskGoTextGray
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = TaskGoTextBlack,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Clickable area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(
                onClick = onCategorySelected,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = if (isSelected) "Selecionado" else "Selecionar",
                    style = FigmaStatusText,
                    color = TaskGoTextGray
                )
            }
        }
    }
}

data class SupportCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)




