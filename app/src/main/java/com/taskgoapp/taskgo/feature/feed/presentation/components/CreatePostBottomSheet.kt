package com.taskgoapp.taskgo.feature.feed.presentation.components

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
import com.taskgoapp.taskgo.core.design.ImageEditor
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    onPostCreated: (String, List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    var postText by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current
    val maxMediaCount = 10
    
    // Verificar permissão reativamente
    val hasImagePermission by remember {
        derivedStateOf {
            PermissionHandler.hasImageReadPermission(context)
        }
    }
    
    // Launcher para seleção múltipla de mídias
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxMediaCount)
    ) { uris: List<Uri> ->
        val currentMedia = selectedMediaUris.toMutableList()
        val availableSlots = maxMediaCount - currentMedia.size
        val newMedia = currentMedia + uris.take(availableSlots)
        selectedMediaUris = newMedia.take(maxMediaCount)
    }
    
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        },
        onPermissionDenied = {
            // Permissão negada
        }
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Criar Post",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TaskGoTextBlack
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = TaskGoTextGray
                    )
                }
            }
            
            // Campo de texto
            OutlinedTextField(
                value = postText,
                onValueChange = { 
                    if (it.length <= 2000) { // Limite de 2000 caracteres
                        postText = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 300.dp),
                placeholder = {
                    Text(
                        text = "O que você está pensando?",
                        color = TaskGoTextGray
                    )
                },
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = TaskGoTextGray
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Text(
                text = "${postText.length}/2000",
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray,
                modifier = Modifier.align(Alignment.End)
            )
            
            // Preview de mídias selecionadas
            if (selectedMediaUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(selectedMediaUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Mídia ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Botão remover
                            IconButton(
                                onClick = {
                                    selectedMediaUris = selectedMediaUris.toMutableList().apply {
                                        removeAt(index)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Botão adicionar mídia
            if (selectedMediaUris.size < maxMediaCount) {
                OutlinedButton(
                    onClick = {
                        if (hasImagePermission) {
                            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        } else {
                            imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Fotos/Vídeos (${selectedMediaUris.size}/$maxMediaCount)")
                }
            }
            
            // Botão publicar
            Button(
                onClick = {
                    if (postText.isNotBlank() || selectedMediaUris.isNotEmpty()) {
                        onPostCreated(postText, selectedMediaUris)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = postText.isNotBlank() || selectedMediaUris.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen,
                    disabledContainerColor = TaskGoTextGray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Publicar",
                    style = MaterialTheme.typography.labelLarge,
                    color = TaskGoBackgroundWhite
                )
            }
        }
    }
}
