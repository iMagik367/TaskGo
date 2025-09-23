package br.com.taskgo.taskgo.feature.splash.presentation

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
import com.example.taskgoapp.core.design.TGIcons
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 segundos de delay
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00BD48)), // Verde #00BD48
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
