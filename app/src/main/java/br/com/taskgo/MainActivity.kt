package br.com.taskgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import br.com.taskgo.navigation.TaskGoNavGraph
import br.com.taskgo.core.theme.TaskGoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskGoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TaskGoNavGraph(navController = navController)
                }
            }
        }
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