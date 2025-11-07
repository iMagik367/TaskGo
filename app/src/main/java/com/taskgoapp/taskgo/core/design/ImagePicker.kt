package com.taskgoapp.taskgo.core.design

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberCameraPermissionLauncher
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher
import java.io.File

@Composable
fun ImagePicker(
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Adicione fotos"
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionRationaleMessage by remember { mutableStateOf("") }
    
    // URI temporária para foto da câmera
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraAction by remember { mutableStateOf(false) }
    
    // Verificar permissões
    val hasCameraPermission = remember {
        PermissionHandler.hasCameraPermission(context)
    }
    val hasImagePermission = remember {
        PermissionHandler.hasImageReadPermission(context)
    }
    
    // Launcher para galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    // Launcher para câmera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { onImageSelected(it) }
        }
    }
    
    // Launcher para permissão de câmera
    val cameraPermissionLauncher = rememberCameraPermissionLauncher(
        onPermissionGranted = {
            pendingCameraAction = true
        },
        onPermissionDenied = {
            showPermissionRationale = true
            permissionRationaleMessage = "A permissão da câmera é necessária para tirar fotos"
        }
    )
    
    // Launcher para permissão de galeria
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            galleryLauncher.launch("image/*")
        },
        onPermissionDenied = {
            showPermissionRationale = true
            permissionRationaleMessage = "A permissão de acesso à galeria é necessária para selecionar imagens"
        }
    )
    
    // Executar ação da câmera quando permissão for concedida
    LaunchedEffect(pendingCameraAction) {
        if (pendingCameraAction && PermissionHandler.hasCameraPermission(context)) {
            val imageFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            cameraImageUri = Uri.fromFile(imageFile)
            cameraLauncher.launch(cameraImageUri!!)
            pendingCameraAction = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = placeholderText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                // Mostrar imagem selecionada
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedImageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Imagem do produto",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Mostrar placeholder
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Diálogo para escolher fonte da imagem
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Selecionar imagem") },
            text = { Text("Escolha de onde deseja obter a imagem") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        if (hasImagePermission) {
                            galleryLauncher.launch("image/*")
                        } else {
                            imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                        }
                    }
                ) {
                    Text("Galeria")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        if (hasCameraPermission) {
                            val imageFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                            cameraImageUri = Uri.fromFile(imageFile)
                            cameraLauncher.launch(cameraImageUri!!)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Text("Câmera")
                }
            }
        )
    }
    
    // Diálogo de explicação de permissão
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permissão Necessária") },
            text = { Text(permissionRationaleMessage) },
            confirmButton = {
                TextButton(
                    onClick = { showPermissionRationale = false }
                ) {
                    Text("Entendi")
                }
            }
        )
    }
}
