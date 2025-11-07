package com.taskgoapp.taskgo.feature.splash.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
        LaunchedEffect(Unit) {
            delay(2000) // 2 segundos de delay
            // Navegar para login de pessoa física
            onNavigateToLogin()
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoGreen), // Verde #00BD48 do Figma
        contentAlignment = Alignment.Center
    ) {
        // Logo vertical do TaskGo (sem texto adicional)
        Image(
            painter = painterResource(id = TGIcons.TaskGoLogoVertical),
            contentDescription = "TaskGo Logo",
            modifier = Modifier.size(120.dp)
        )
    }
}
