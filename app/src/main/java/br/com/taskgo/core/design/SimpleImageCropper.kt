package br.com.taskgo.taskgo.core.design

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions

@Composable
fun SimpleImageCropper(
    onImageCropped: (Uri) -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
        }
    }

    // Abrir galeria automaticamente quando o composable for criado
    LaunchedEffect(Unit) {
        println("DEBUG: SimpleImageCropper - Abrindo galeria")
        imagePicker.launch("image/*")
    }
}
