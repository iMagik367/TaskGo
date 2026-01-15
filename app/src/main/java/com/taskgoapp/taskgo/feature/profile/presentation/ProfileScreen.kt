package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextDark
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.model.AccountType

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToMyServices: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToMyServiceOrders: () -> Unit = {},
    onNavigateToUserReviews: ((String, String) -> Unit)? = null, // userId, userName
    onNavigateToAboutMe: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else if (uiState.error != null) {
            item {
                Text(
                    text = uiState.error ?: "Erro ao carregar perfil",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                // Header do perfil com dados reais do Firestore
                ProfileHeader(
                    name = uiState.name,
                    email = uiState.email,
                    avatarUri = uiState.avatarUri,
                    rating = uiState.rating?.toFloat() ?: 0f,
                    servicesCount = 0, // TODO: Adicionar contagem de serviços quando necessário
                    onHeaderClick = {
                        // Abrir tela "Sobre mim" ao clicar no card
                        onNavigateToAboutMe()
                    }
                )
            }
            
            item {
                // Menu de opções
                ProfileMenu(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToMyOrders = onNavigateToMyOrders,
                    onNavigateToMyServices = onNavigateToMyServices,
                    onNavigateToMyProducts = onNavigateToMyProducts,
                    onNavigateToMyServiceOrders = onNavigateToMyServiceOrders,
                    accountType = uiState.accountType
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    avatarUri: String?,
    rating: Float,
    servicesCount: Int,
    onHeaderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
            .clickable { onHeaderClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar com foto do perfil ou ícone padrão
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(TaskGoGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUri.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Foto do perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(50.dp),
                        tint = TaskGoGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Nome do usuário - centralizado
            Text(
                text = name.ifBlank { "Usuário" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email - centralizado
            Text(
                text = email.ifBlank { "Email não disponível" },
                fontSize = 14.sp,
                color = TaskGoTextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Estatísticas - centralizadas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "⭐ $rating",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TaskGoTextDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Avaliação",
                        fontSize = 12.sp,
                        color = TaskGoTextGray,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Divisor vertical
                VerticalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp),
                    color = TaskGoTextGray.copy(alpha = 0.2f)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "$servicesCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TaskGoTextDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Serviços",
                        fontSize = 12.sp,
                        color = TaskGoTextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenu(
    onNavigateToSettings: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToMyServices: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToMyServiceOrders: () -> Unit,
    accountType: AccountType = AccountType.CLIENTE,
    modifier: Modifier = Modifier
) {
    val menuItems = when (accountType) {
        AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> listOf(
            // Unificar menu items para Parceiro: Serviços + Produtos
            ProfileMenuItem(
                title = "Meus Serviços",
                icon = Icons.Default.Build,
                onClick = onNavigateToMyServices
            ),
            ProfileMenuItem(
                title = "Meus Produtos",
                icon = Icons.Default.Inventory,
                onClick = onNavigateToMyProducts
            ),
            ProfileMenuItem(
                title = "Meus Pedidos",
                icon = Icons.Default.ShoppingBag,
                onClick = onNavigateToMyOrders
            ),
            // Removido "Minhas Ordens de Serviço" - parceiros não geram ordens, apenas recebem
            ProfileMenuItem(
                title = "Configurações",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )
        )
        else -> listOf( // CLIENTE
            ProfileMenuItem(
                title = "Minhas Ordens de Serviço",
                icon = Icons.AutoMirrored.Filled.Assignment,
                onClick = onNavigateToMyServiceOrders
            ),
            ProfileMenuItem(
                title = "Meus Pedidos",
                icon = Icons.Default.ShoppingBag,
                onClick = onNavigateToMyOrders
            ),
            ProfileMenuItem(
                title = "Configurações",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            menuItems.forEachIndexed { index, item ->
                ProfileMenuRow(
                    item = item,
                    showDivider = index < menuItems.size - 1
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    item: ProfileMenuItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { item.onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = TaskGoGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = item.title,
                fontSize = 16.sp,
                color = TaskGoTextDark,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ir",
                tint = TaskGoTextGray,
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (showDivider) {
            HorizontalDivider(
                color = TaskGoTextGray.copy(alpha = 0.2f),
                thickness = 1.dp
            )
        }
    }
}

data class ProfileMenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)