package com.taskgoapp.taskgo.core.design

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher

/**
 * Componente para seleção múltipla de imagens da galeria
 * Remove funcionalidade de corte - apenas seleção múltipla
 */
@Composable
fun ImageEditor(
    selectedImageUris: List<Uri>,
    onImagesChanged: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    maxImages: Int = 5,
    placeholderText: String = "Adicione fotos"
) {
    val context = LocalContext.current
    var pendingGalleryAction by remember { mutableStateOf(false) }
    
    // Verificar permissão de imagem
    val hasImagePermission = remember {
        PermissionHandler.hasImageReadPermission(context)
    }

    // Launcher para galeria múltipla
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris: List<Uri> ->
        val currentImages = selectedImageUris.toMutableList()
        val availableSlots = maxImages - currentImages.size
        
        // Adicionar novas imagens respeitando o limite
        val newImages = currentImages + uris.take(availableSlots)
        onImagesChanged(newImages.take(maxImages))
    }
    
    // Launcher para permissão de imagem
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            pendingGalleryAction = true
        },
        onPermissionDenied = {
            // Permissão negada, não fazer nada
        }
    )
    
    // Executar ação da galeria quando permissão for concedida
    LaunchedEffect(pendingGalleryAction) {
        if (pendingGalleryAction && PermissionHandler.hasImageReadPermission(context)) {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pendingGalleryAction = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grid de imagens selecionadas
        if (selectedImageUris.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(selectedImageUris) { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagem selecionada $index",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Botão remover
                        IconButton(
                            onClick = {
                                val newImages = selectedImageUris.toMutableList()
                                newImages.removeAt(index)
                                onImagesChanged(newImages)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Remover imagem",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Botão adicionar fotos (se ainda há espaço)
        if (selectedImageUris.size < maxImages) {
            OutlinedButton(
                onClick = {
                    if (hasImagePermission) {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedImageUris.isEmpty()) {
                        placeholderText
                    } else {
                        "Adicionar mais fotos (${selectedImageUris.size}/$maxImages)"
                    }
                )
            }
        } else {
            // Mostrar mensagem quando limite atingido
            Text(
                text = "Limite de $maxImages imagens atingido",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
