package com.taskgoapp.taskgo.core.design

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher

/**
 * Componente para seleção e edição de foto de perfil com corte circular
 * Permite:
 * - Seleção de imagem da galeria
 * - Corte circular usando ImageCropper (com proporção 1:1)
 * - O ImageCropper nativo já suporta movimento/pan e zoom/pinch
 */
@Composable
fun CircularImageCropper(
    currentImageUri: String?,
    onImageCropped: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingGalleryAction by remember { mutableStateOf(false) }
    
    val hasImagePermission = remember {
        PermissionHandler.hasImageReadPermission(context)
    }
    
    // Launcher para ImageCropper com formato circular (proporção 1:1)
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                onImageCropped(croppedUri)
            }
        }
    }
    
    // Launcher para seleção de imagem da galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }
    
    // Launcher para permissão de imagem
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            pendingGalleryAction = true
        },
        onPermissionDenied = {
            // Permissão negada
        }
    )
    
    // Executar ação da galeria quando permissão for concedida
    LaunchedEffect(pendingGalleryAction) {
        if (pendingGalleryAction && PermissionHandler.hasImageReadPermission(context)) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pendingGalleryAction = false
        }
    }
    
    // Iniciar cropper quando imagem for selecionada
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            val cropOptions = CropImageContractOptions(
                uri = uri,
                cropImageOptions = CropImageConfig.createCircularOptions()
            )
            cropImageLauncher.launch(cropOptions)
            selectedImageUri = null // Limpar após iniciar
        }
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Exibir imagem atual ou placeholder
        if (!currentImageUri.isNullOrBlank()) {
            AsyncImage(
                model = currentImageUri,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    painter = painterResource(TGIcons.Profile),
                    contentDescription = "Adicionar foto",
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Overlay para indicar que é clicável
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (hasImagePermission) {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                    }
                },
            color = androidx.compose.ui.graphics.Color.Transparent
        ) {}
    }
}

