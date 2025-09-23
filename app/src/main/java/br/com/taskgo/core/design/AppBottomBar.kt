package br.com.taskgo.taskgo.core.design

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.taskgoapp.R

data class BottomBarItem(
    val route: String,
    val titleResId: Int,
    val iconResId: Int
)

val bottomBarItems = listOf(
    BottomBarItem(
        route = "home",
        titleResId = R.string.nav_home,
        iconResId = R.drawable.ic_home // Usando novo ícone PNG
    ),
    BottomBarItem(
        route = "services",
        titleResId = R.string.nav_services,
        iconResId = R.drawable.ic_servicos // Corrigido para ic_servicos
    ),
    BottomBarItem(
        route = "products",
        titleResId = R.string.nav_products,
        iconResId = R.drawable.ic_produtos // Usando novo ícone PNG
    ),
    BottomBarItem(
        route = "messages",
        titleResId = R.string.nav_messages,
        iconResId = R.drawable.ic_mensagens // Usando novo ícone PNG
    ),
    BottomBarItem(
        route = "profile",
        titleResId = R.string.nav_profile,
        iconResId = R.drawable.ic_perfil // Usando novo ícone PNG
    )
)

@Composable
fun AppBottomBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    ) {
        bottomBarItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.route) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = stringResource(id = item.titleResId)
                    )
                },
                label = {
                    Text(text = stringResource(id = item.titleResId))
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                    indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
