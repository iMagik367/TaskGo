package com.taskgoapp.taskgo.feature.splash.presentation

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberMultiplePermissionsLauncher
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToBiometricAuth: () -> Unit = {}, // Mantido para compatibilidade, mas não será usado
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionsRequested by viewModel.permissionsRequested.collectAsState(initial = false)
    
    // Preparar lista de permissões necessárias
    val requiredPermissions = remember {
        mutableListOf<String>().apply {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    
    // Launcher para solicitar múltiplas permissões
    val permissionsLauncher = rememberMultiplePermissionsLauncher(
        onAllPermissionsGranted = {
            Log.d("SplashScreen", "Todas as permissões foram concedidas")
            viewModel.setPermissionsRequested(true)
            // Continuar com a navegação após um pequeno delay
            scope.launch {
                delay(500)
                viewModel.checkAuthState(
                    onNavigateToBiometricAuth = {},
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        },
        onPermissionDenied = {
            Log.d("SplashScreen", "Algumas permissões foram negadas, mas continuando...")
            viewModel.setPermissionsRequested(true)
            // Continuar mesmo se algumas permissões foram negadas
            scope.launch {
                delay(500)
                viewModel.checkAuthState(
                    onNavigateToBiometricAuth = {},
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }
    )
    
    LaunchedEffect(Unit) {
        Log.d("SplashScreen", "=== Iniciando SplashScreen ===")
        delay(2000) // 2 segundos de delay
        
        // Verificar quais permissões ainda não foram concedidas
        val missingPermissions = requiredPermissions.filter { permission ->
            !PermissionHandler.hasPermission(context, permission)
        }
        
        Log.d("SplashScreen", "Permissões necessárias: ${requiredPermissions.joinToString()}")
        Log.d("SplashScreen", "Permissões faltando: ${missingPermissions.joinToString()}")
        Log.d("SplashScreen", "Permissões já solicitadas antes: $permissionsRequested")
        
        // SEMPRE solicitar permissões no primeiro acesso, mesmo que já tenham sido solicitadas antes
        // Isso garante que todas as permissões sejam solicitadas
        if (!permissionsRequested || missingPermissions.isNotEmpty()) {
            // Solicitar TODAS as permissões (o Android vai mostrar apenas as que ainda não foram concedidas)
            Log.d("SplashScreen", "Solicitando TODAS as permissões: ${requiredPermissions.joinToString()}")
            permissionsLauncher.launch(requiredPermissions)
        } else {
            // Todas as permissões já foram concedidas, apenas navegar
            Log.d("SplashScreen", "Todas as permissões já foram concedidas, navegando...")
            viewModel.checkAuthState(
                onNavigateToBiometricAuth = {},
                onNavigateToHome = onNavigateToHome,
                onNavigateToLogin = onNavigateToLogin
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaskGoGreen), // Verde #00BD48 do Figma
        contentAlignment = Alignment.Center
    ) {
        // Logo vertical do TaskGo (aumentado)
        Image(
            painter = painterResource(id = TGIcons.TaskGoLogoVertical),
            contentDescription = "TaskGo Logo",
            modifier = Modifier.size(200.dp)
        )
    }
}
