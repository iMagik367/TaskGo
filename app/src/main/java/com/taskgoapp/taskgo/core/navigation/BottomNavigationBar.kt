package com.taskgoapp.taskgo.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextGrayPlaceholder
import com.taskgoapp.taskgo.core.design.TGIcons

/**
 * Barra de navegação inferior baseada no protótipo Figma
 * Cores extraídas: ativa #00BD48, inativa #BBBBBB
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavItems = listOf(
        BottomNavItem(
            route = "home",
            label = "Início",
            iconRes = TGIcons.Home
        ),
        BottomNavItem(
            route = "services",
            label = "Serviços", 
            iconRes = TGIcons.Services
        ),
        BottomNavItem(
            route = "products",
            label = "Produtos",
            iconRes = TGIcons.Products
        ),
        BottomNavItem(
            route = "messages",
            label = "Mensagem",
            iconRes = TGIcons.Messages
        ),
        BottomNavItem(
            route = "profile",
            label = "Perfil",
            iconRes = TGIcons.Profile
        )
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                BottomNavItem(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) TaskGoGreen.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (isSelected) TaskGoGreen else TaskGoTextGrayPlaceholder
    val iconColor = if (isSelected) TaskGoGreen else TaskGoTextGrayPlaceholder

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconRes: Int
)
