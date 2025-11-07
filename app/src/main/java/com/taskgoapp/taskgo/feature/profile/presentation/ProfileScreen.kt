package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextDark
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToMyServices: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.error != null) {
            item {
                Text(
                    text = uiState.error ?: "Erro ao carregar perfil",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            item {
                // Header do perfil com dados reais do Firestore
                ProfileHeader(
                    name = uiState.name.ifBlank { "Usuário" },
                    email = uiState.email.ifBlank { "Email não disponível" },
                    rating = uiState.rating?.toFloat() ?: 0f,
                    servicesCount = 0 // TODO: Adicionar contagem de serviços quando necessário
                )
            }
            
            item {
                // Menu de opções
                ProfileMenu(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToMyOrders = onNavigateToMyOrders,
                    onNavigateToMyServices = onNavigateToMyServices,
                    onNavigateToMyProducts = onNavigateToMyProducts
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    rating: Float,
    servicesCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TaskGoGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(40.dp),
                    tint = TaskGoGreen
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            
            Text(
                text = email,
                fontSize = 14.sp,
                color = TaskGoTextGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⭐ $rating",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TaskGoTextDark
                    )
                    Text(
                        text = "Avaliação",
                        fontSize = 12.sp,
                        color = TaskGoTextGray
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$servicesCount+",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TaskGoTextDark
                    )
                    Text(
                        text = "Serviços",
                        fontSize = 12.sp,
                        color = TaskGoTextGray
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
    modifier: Modifier = Modifier
) {
    val menuItems = listOf(
        ProfileMenuItem(
            title = "Meus Pedidos",
            icon = Icons.Default.ShoppingBag,
            onClick = onNavigateToMyOrders
        ),
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
            title = "Configurações",
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings
        )
    )
    
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
                imageVector = Icons.Default.ChevronRight,
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