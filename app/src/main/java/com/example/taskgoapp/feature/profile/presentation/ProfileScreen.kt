package com.example.taskgoapp.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.example.taskgoapp.core.design.TGIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.AppTopBar
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskgoapp.feature.profile.presentation.ProfileViewModel
import com.example.taskgoapp.core.model.AccountType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToMyData: () -> Unit,
    onNavigateToMyServices: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToMyReviews: () -> Unit,
    onNavigateToManageProposals: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToMessages: () -> Unit
) {
    val vm: ProfileViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
                onBackClick = null, // Removido botão de voltar
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Bell),
                            contentDescription = stringResource(R.string.notifications_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Cart),
                            contentDescription = stringResource(R.string.cart_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToMessages,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Messages),
                            contentDescription = stringResource(R.string.messages_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            item(key = "profile_header") {
                ProfileHeader(
                    userName = state.name,
                    accountType = state.accountType,
                    avatarUri = state.avatarUri,
                    onEditProfile = onNavigateToMyData
                )
            }
            
            // Quick Stats
            item(key = "profile_stats") {
                ProfileStats(state = state)
            }
            

            
            // Main Menu
            item(key = "profile_menu") {
                ProfileMenuSection(
                    onNavigateToMyData = onNavigateToMyData,
                    onNavigateToMyServices = onNavigateToMyServices,
                    onNavigateToMyProducts = onNavigateToMyProducts,
                    onNavigateToMyOrders = onNavigateToMyOrders,
                    onNavigateToMyReviews = onNavigateToMyReviews,
                    onNavigateToManageProposals = onNavigateToManageProposals,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    accountType: AccountType,
    avatarUri: String?,
    onEditProfile: () -> Unit
) {
    val circleShape = remember { CircleShape }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(circleShape)
                .background(surfaceVariantColor),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUri.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(circleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(TGIcons.Profile),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = onSurfaceVariantColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (userName.isNotBlank()) userName else stringResource(R.string.profile_loading_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = when (accountType) {
                AccountType.PRESTADOR -> "Prestador de Serviços"
                AccountType.VENDEDOR -> "Vendedor"
                AccountType.CLIENTE -> "Cliente"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEditProfile,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                painter = painterResource(TGIcons.Edit),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_edit_profile))
        }
    }
}

@Composable
private fun ProfileStats(state: ProfileState) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            StatItem(
                icon = TGIcons.Star,
                value = state.rating?.toString() ?: "0.0",
                label = stringResource(R.string.profile_rating),
                modifier = Modifier.weight(1f)
            )
            
            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )
            
            StatItem(
                icon = TGIcons.Star,
                value = "0",
                label = stringResource(R.string.profile_reviews),
                modifier = Modifier.weight(1f)
            )
            
            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )
            
            StatItem(
                icon = TGIcons.Time,
                value = "",
                label = stringResource(R.string.profile_time),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
                Text(
            text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



@Composable
private fun ProfileMenuSection(
    onNavigateToMyData: () -> Unit,
    onNavigateToMyServices: () -> Unit,
    onNavigateToMyProducts: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToMyReviews: () -> Unit,
    onNavigateToManageProposals: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_menu_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val menuItems = listOf(
                MenuItem(
                    icon = TGIcons.Profile,
                    title = stringResource(R.string.profile_my_data),
                    onClick = onNavigateToMyData
                ),
                MenuItem(
                    icon = TGIcons.Services,
                    title = stringResource(R.string.profile_my_services),
                    onClick = onNavigateToMyServices
                ),
                MenuItem(
                    icon = TGIcons.Cart,
                    title = stringResource(R.string.profile_my_products),
                    onClick = onNavigateToMyProducts
                ),
                MenuItem(
                    icon = TGIcons.MyOrders,
                    title = stringResource(R.string.profile_my_orders),
                    onClick = onNavigateToMyOrders
                ),
                MenuItem(
                    icon = TGIcons.Star,
                    title = stringResource(R.string.profile_my_reviews),
                    onClick = onNavigateToMyReviews
                ),
                MenuItem(
                    icon = TGIcons.ManageProposals,
                    title = stringResource(R.string.profile_manage_proposals),
                    onClick = onNavigateToManageProposals
                ),
                MenuItem(
                    icon = TGIcons.Settings,
                    title = stringResource(R.string.settings_title),
                    onClick = onNavigateToSettings
                )
            )
            
            menuItems.forEach { item ->
                MenuItemRow(
                    item = item,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (item != menuItems.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    item: MenuItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = item.onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
            
            Icon(
                painter = painterResource(TGIcons.Arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

private data class MenuItem(
    val icon: Int,
    val title: String,
    val onClick: () -> Unit
)
