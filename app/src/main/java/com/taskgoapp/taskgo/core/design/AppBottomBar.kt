package com.taskgoapp.taskgo.core.design

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.TGIcon
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.*

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
        route = "feed",
        titleResId = R.string.nav_feed,
        iconResId = R.drawable.ic_feed
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
        containerColor = TaskGoBackgroundWhite,
    ) {
        bottomBarItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.route) },
                icon = {
                    TGIcon(
                        iconRes = item.iconResId,
                        contentDescription = stringResource(id = item.titleResId),
                        size = TGIcons.Sizes.Navigation,
                        tint = if (selected) TaskGoNavActive else TaskGoNavInactive
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = item.titleResId),
                        style = FigmaNavLabel,
                        color = if (selected) TaskGoNavActive else TaskGoNavInactive
                    )
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = TaskGoNavActive,
                    selectedTextColor = TaskGoNavActive,
                    indicatorColor = TaskGoNavActive.copy(alpha = 0.12f),
                    unselectedIconColor = TaskGoNavInactive,
                    unselectedTextColor = TaskGoNavInactive
                )
            )
        }
    }
}
