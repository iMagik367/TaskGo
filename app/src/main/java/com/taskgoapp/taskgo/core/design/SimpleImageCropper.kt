package com.taskgoapp.taskgo.core.design

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher

@Composable
fun SimpleImageCropper(
    onImageCropped: (Uri) -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingImagePickerAction by remember { mutableStateOf(false) }
    
    val hasImagePermission = remember {
        PermissionHandler.hasImageReadPermission(context)
    }
    
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                onImageCropped(croppedUri)
            }
        } else {
            onCancel()
        }
    }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            // Abrir diretamente o cropper com a imagem selecionada
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = it,
                    cropImageOptions = CropImageConfig.createDefaultOptions()
                )
            )
        } ?: run {
            onCancel()
        }
    }
    
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            pendingImagePickerAction = true
        },
        onPermissionDenied = {
            onCancel()
        }
    )
    
    // Executar ação do imagePicker quando permissão for concedida
    LaunchedEffect(pendingImagePickerAction) {
        if (pendingImagePickerAction && PermissionHandler.hasImageReadPermission(context)) {
            imagePicker.launch("image/*")
            pendingImagePickerAction = false
        }
    }

    // Abrir galeria automaticamente quando o composable for criado
    LaunchedEffect(Unit) {
        if (hasImagePermission) {
            imagePicker.launch("image/*")
        } else {
            imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
        }
    }
}
