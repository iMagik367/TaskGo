package com.taskgoapp.taskgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.taskgoapp.taskgo.core.navigation.BottomNavigationBar
import com.taskgoapp.taskgo.core.theme.TaskGoTheme
import com.taskgoapp.taskgo.core.design.review.DesignReviewState
import com.taskgoapp.taskgo.navigation.TaskGoNavGraph
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskGoTheme {
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    val viewModel: MainActivityViewModel = hiltViewModel()
    val authRepository = viewModel.authRepository
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "home"
        
        // Observar mudanças no estado de autenticação do Firebase
        val authState by authRepository.observeAuthState().collectAsState(initial = authRepository.getCurrentUser())
        val isAuthenticated = authState != null
        
        // Atualizar estado de autenticação quando a rota mudar (backup)
        LaunchedEffect(currentRoute) {
            // Força atualização quando a rota mudar
        }
        
        // Rotas onde a barra inferior não deve aparecer (rotas de autenticação)
        val authRoutes = listOf(
            "splash",
            "login_person",
            "login_store",
            "forgot_password",
            "signup",
            "signup_success"
        )
        
        val hideBottomBar = !isAuthenticated || 
                           authRoutes.contains(currentRoute) ||
                           currentRoute.startsWith("signup") ||
                           currentRoute.startsWith("login")
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Conteúdo principal
                TaskGoNavGraph(
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )

                // Barra de navegação inferior - só aparece quando não estiver em splash/login
                if (!hideBottomBar) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
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

            // Overlay global do Design Review
            DesignReviewDebugOverlay()
        }
    }
}

@Composable
private fun DesignReviewDebugOverlay(gridStep: Dp = 8.dp) {
    if (!DesignReviewState.overlayEnabled) return

    val alpha by rememberUpdatedState(DesignReviewState.overlayAlpha)
    val screenshotRes = DesignReviewState.overlayScreenshotRes
    val gridEnabled = DesignReviewState.gridEnabled

    if (screenshotRes != null) {
        Image(
            painter = painterResource(screenshotRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            contentScale = ContentScale.FillBounds
        )
    }

    if (gridEnabled) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepPx = gridStep.toPx()
            val width = size.width
            val height = size.height
            val gridColor = androidx.compose.ui.graphics.Color(0x33888888)
            var x = 0f
            while (x <= width) {
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, height), strokeWidth = 1f)
                x += stepPx
            }
            var y = 0f
            while (y <= height) {
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 1f)
                y += stepPx
            }
        }
    }
}
