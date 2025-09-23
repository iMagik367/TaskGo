package br.com.taskgo.taskgo.core.design

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions

@Composable
fun ImageEditor(
    selectedImageUris: List<Uri>,
    onImagesChanged: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
    maxImages: Int = 5,
    placeholderText: String = "Adicione fotos"
) {
    val context = LocalContext.current
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var showSimpleCropper by remember { mutableStateOf(false) }

    // Launcher para galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages - selectedImageUris.size)
    ) { uris: List<Uri> ->
        val newImages = selectedImageUris + uris
        onImagesChanged(newImages.take(maxImages))
    }


    // Launcher para edição de imagem
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                editingImageIndex?.let { index ->
                    val newImages = selectedImageUris.toMutableList()
                    newImages[index] = uri
                    onImagesChanged(newImages)
                }
                editingImageIndex = null
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = placeholderText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        // Quadro grande para adicionar fotos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
                .clickable { 
                    if (selectedImageUris.size < maxImages) {
                        showSimpleCropper = true 
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUris.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar foto",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Toque para adicionar fotos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Mostrar primeira imagem em destaque
                AsyncImage(
                    model = selectedImageUris.first(),
                    contentDescription = "Imagem principal",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Exibição horizontal das imagens selecionadas
        if (selectedImageUris.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(selectedImageUris.size) { index ->
                    ImageThumbnail(
                        imageUri = selectedImageUris[index],
                        onEdit = { editingImageIndex = index },
                        onRemove = {
                            val newImages = selectedImageUris.toMutableList()
                            newImages.removeAt(index)
                            onImagesChanged(newImages)
                        }
                    )
                }
                
                // Botão para adicionar mais fotos
                if (selectedImageUris.size < maxImages) {
                    item {
                        AddMoreButton(
                            onClick = { showImageSourceDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialog para escolher fonte da imagem
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Selecionar imagem") },
            text = { Text("Escolha de onde deseja obter a imagem") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text("Galeria")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        // Para câmera, precisaríamos criar um arquivo temporário
                        // Por simplicidade, vamos apenas abrir a galeria por enquanto
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text("Câmera")
                }
            }
        )
    }

    // Iniciar edição de imagem quando necessário
    LaunchedEffect(editingImageIndex) {
        editingImageIndex?.let { index ->
            val imageUri = selectedImageUris[index]
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = imageUri,
                    cropImageOptions = CropImageConfig.createDefaultOptions()
                )
            )
        }
    }

    // SimpleImageCropper para seleção inicial
    if (showSimpleCropper) {
        SimpleImageCropper(
            onImageCropped = { croppedUri ->
                val newImages = selectedImageUris.toMutableList()
                newImages.add(croppedUri)
                onImagesChanged(newImages)
                showSimpleCropper = false
            },
            onCancel = {
                showSimpleCropper = false
            }
        )
    }
}

@Composable
private fun ImageThumbnail(
    imageUri: Uri,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Overlay com botões de ação
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMoreButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Adicionar mais",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
