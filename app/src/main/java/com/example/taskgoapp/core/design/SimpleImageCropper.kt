package com.example.taskgoapp.core.design

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.taskgoapp.core.design.TGIcons
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

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
