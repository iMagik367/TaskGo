package com.taskgoapp.taskgo.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Handler para solicitar permissões em runtime usando Compose
 */
object PermissionHandler {
    
    /**
     * Verifica se uma permissão foi concedida
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Verifica se a permissão de câmera foi concedida
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }
    
    /**
     * Verifica se a permissão de leitura de imagens foi concedida
     * (compatível com Android 13+ e versões anteriores)
     */
    fun hasImageReadPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Verifica se a permissão de notificações foi concedida
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Não necessário em versões anteriores
        }
    }
    
    /**
     * Verifica se a permissão de localização foi concedida
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocation || coarseLocation
    }
    
    /**
     * Retorna a permissão de leitura de imagens apropriada para a versão do Android
     */
    fun getImageReadPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}

/**
 * Composable para criar um launcher de permissão de câmera
 */
@Composable
fun rememberCameraPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): androidx.activity.result.ActivityResultLauncher<String> {
    val context = LocalContext.current
    
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
}

/**
 * Composable para criar um launcher de permissão de leitura de imagens
 */
@Composable
fun rememberImageReadPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): androidx.activity.result.ActivityResultLauncher<String> {
    val context = LocalContext.current
    
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
}

/**
 * Composable para criar um launcher de permissão de notificações
 */
@Composable
fun rememberNotificationPermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): androidx.activity.result.ActivityResultLauncher<String>? {
    val context = LocalContext.current
    
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    } else {
        // Permissão não necessária em versões anteriores
        null
    }
}

/**
 * Composable para criar um launcher de múltiplas permissões
 */
@Composable
fun rememberMultiplePermissionsLauncher(
    onAllPermissionsGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): androidx.activity.result.ActivityResultLauncher<Array<String>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onAllPermissionsGranted()
        } else {
            onPermissionDenied()
        }
    }
}

