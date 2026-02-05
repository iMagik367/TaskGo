package com.taskgoapp.taskgo.feature.feed.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher
import com.taskgoapp.taskgo.core.theme.*

/**
 * Campo de criação de post inline (similar ao Facebook)
 */
@Composable
fun InlinePostCreator(
    userAvatarUrl: String?,
    userName: String,
    isLoading: Boolean = false,
    onPostCreated: (String, List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    var postText by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val maxMediaCount = 10
    
    // Verificar permissão reativamente usando derivedStateOf para reagir a mudanças
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
        selectedMediaUris = newMedia
    }
    
    val imagePermissionLauncher = rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Linha superior: Avatar + Campo de texto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar do usuário
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(userAvatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = userName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TaskGoBackgroundGray),
                    contentScale = ContentScale.Crop,
                    loading = {
                        // Placeholder durante carregamento
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TaskGoBackgroundGray),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TaskGoGreen,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        // Placeholder quando erro ao carregar ou URL vazia
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TaskGoBackgroundGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = userName,
                                modifier = Modifier.size(24.dp),
                                tint = TaskGoTextGray
                            )
                        }
                    },
                    success = { state ->
                        SubcomposeAsyncImageContent(
                            painter = state.painter,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                )
                
                // Campo de texto
                OutlinedTextField(
                    value = postText,
                    onValueChange = { if (it.length <= 2000) postText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("No que você está pensando?", color = TaskGoTextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TaskGoBackgroundGray,
                        unfocusedContainerColor = TaskGoBackgroundGray,
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoBackgroundGray
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4
                )
            }
            
            // Mídias selecionadas (se houver)
            if (selectedMediaUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(selectedMediaUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
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
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover",
                                    tint = TaskGoBackgroundWhite,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                                            CircleShape
                                        )
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Divisor
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = TaskGoBackgroundGray
            )
            
            // Linha inferior: Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Adicionar Fotos/Vídeos
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (hasImagePermission) {
                                mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            } else {
                                imagePermissionLauncher.launch(PermissionHandler.getImageReadPermission())
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TaskGoGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fotos/Vídeos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
                
                // Botão Publicar
                Button(
                    onClick = {
                        if ((postText.isNotBlank() || selectedMediaUris.isNotEmpty()) && !isLoading) {
                            onPostCreated(postText, selectedMediaUris)
                            // Limpar apenas após chamar o callback
                            postText = ""
                            selectedMediaUris = emptyList()
                        }
                    },
                    enabled = (postText.isNotBlank() || selectedMediaUris.isNotEmpty()) && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen,
                        disabledContainerColor = TaskGoBackgroundGray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TaskGoBackgroundWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Publicar",
                            color = if (postText.isNotBlank() || selectedMediaUris.isNotEmpty()) {
                                TaskGoBackgroundWhite
                            } else {
                                TaskGoTextGray
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}





