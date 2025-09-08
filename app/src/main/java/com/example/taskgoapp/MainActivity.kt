package com.example.taskgoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.taskgoapp.core.design.AppBottomBar
import com.example.taskgoapp.core.design.review.DesignReviewScreen
import com.example.taskgoapp.core.navigation.TaskGoNavGraph
import com.example.taskgoapp.core.navigation.TaskGoDestinations
import com.example.taskgoapp.core.theme.TaskGoTheme
import com.example.taskgoapp.feature.splash.presentation.SplashScreen
import com.example.taskgoapp.feature.home.presentation.HomeScreen
import com.example.taskgoapp.feature.messages.presentation.MessagesScreen
import com.example.taskgoapp.feature.products.presentation.ProductsScreen
import com.example.taskgoapp.feature.products.presentation.ProductDetailScreen
import com.example.taskgoapp.feature.products.presentation.CartScreen
import com.example.taskgoapp.feature.profile.presentation.ProfileScreen
import com.example.taskgoapp.feature.settings.presentation.SettingsScreen
import com.example.taskgoapp.feature.settings.presentation.AccountTypeScreen
import com.example.taskgoapp.feature.notifications.presentation.NotificationsScreen
import com.example.taskgoapp.feature.orders.presentation.MyOrdersScreen
import com.example.taskgoapp.feature.services.presentation.CreateWorkOrderScreen
import com.example.taskgoapp.feature.services.presentation.ProposalDetailScreen
import com.example.taskgoapp.feature.services.presentation.ReviewsScreen
import com.example.taskgoapp.feature.products.presentation.CheckoutScreen
import com.example.taskgoapp.feature.checkout.presentation.PaymentMethodScreen
import com.example.taskgoapp.feature.checkout.presentation.OrderSummaryScreen
import com.example.taskgoapp.feature.checkout.presentation.AddressBookScreen
import com.example.taskgoapp.feature.products.presentation.OrderTrackingScreen
import com.example.taskgoapp.feature.messages.presentation.ChatScreen
import com.example.taskgoapp.feature.profile.presentation.MyDataScreen
import com.example.taskgoapp.feature.profile.presentation.MyServicesScreen
import com.example.taskgoapp.feature.profile.presentation.MyProductsScreen
import com.example.taskgoapp.feature.profile.presentation.MyReviewsScreen
import com.example.taskgoapp.feature.profile.presentation.ManageProposalsScreen
import com.example.taskgoapp.feature.settings.presentation.AccountScreen
import com.example.taskgoapp.feature.settings.presentation.PreferencesScreen
import com.example.taskgoapp.feature.settings.presentation.NotificationsSettingsScreen
import com.example.taskgoapp.feature.settings.presentation.LanguageScreen
import com.example.taskgoapp.feature.settings.presentation.PrivacyScreen
import com.example.taskgoapp.feature.settings.presentation.SupportScreen
import com.example.taskgoapp.feature.settings.presentation.AboutScreen
import com.example.taskgoapp.feature.chatai.presentation.AiSupportScreen
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskGoTheme {
                TaskGoAppContent()
            }
        }
    }
}

@Composable
fun TaskGoAppContent() {
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen(
            onSplashComplete = { showSplash = false }
        )
    } else {
        MainAppContent()
    }
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: TaskGoDestinations.HOME_ROUTE

    val isMainTab = currentRoute in listOf(
        TaskGoDestinations.HOME_ROUTE,
        TaskGoDestinations.SERVICES_ROUTE,
        TaskGoDestinations.PRODUCTS_ROUTE,
        TaskGoDestinations.MESSAGES_ROUTE,
        TaskGoDestinations.PROFILE_ROUTE
    )
    
    Scaffold(
        bottomBar = {
            if (isMainTab) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Delegate the whole navigation graph to the centralized TaskGoNavGraph
        Box(modifier = Modifier.padding(paddingValues)) {
            TaskGoNavGraph(navController = navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TaskGoTheme {
        TaskGoAppContent()
    }
}