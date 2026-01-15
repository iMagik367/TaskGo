package com.taskgoapp.taskgo.feature.services.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.permissions.PermissionHandler
import com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun CompleteServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (description: String, time: String, mediaUris: List<Uri>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val maxMediaCount = 10
    
    // Verificar permissão reativamente
    val hasImagePermission by remember {
        mutableStateOf(PermissionHandler.hasImageReadPermission(context))
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Concluir Serviço",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descrição do trabalho realizado *") },
                    placeholder = { Text("Descreva o que foi feito") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoDivider
                    ),
                    isError = showError && description.isBlank()
                )
                
                if (showError && description.isBlank()) {
                    Text(
                        text = "A descrição é obrigatória",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tempo levado") },
                    placeholder = { Text("Ex: 2 horas, 30 minutos") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoDivider
                    )
                )
                
                // Seção de mídias
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Fotos/Vídeos (opcional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TaskGoTextDark,
                        fontWeight = FontWeight.Medium
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
                                            selectedMediaUris = selectedMediaUris.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TaskGoGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar Foto/Vídeo")
                        }
                    }
                    
                    if (selectedMediaUris.size >= maxMediaCount) {
                        Text(
                            text = "Máximo de $maxMediaCount mídias atingido",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isBlank()) {
                        showError = true
                        return@Button
                    }
                    onConfirm(description, time, selectedMediaUris)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Concluir Serviço", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancelar", color = TaskGoTextGray)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = TaskGoBackgroundWhite
    )
}

