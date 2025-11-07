package com.taskgoapp.taskgo.feature.profile.presentation
import com.taskgoapp.taskgo.core.theme.*

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.design.ImageEditor
import com.taskgoapp.taskgo.core.design.SimpleImageCropper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusDadosScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var selectedImageUris by remember { 
        mutableStateOf(state.profileImages.map { Uri.parse(it) }) 
    }

    // Usar SimpleImageCropper para seleção e corte de imagem
    if (showPhotoDialog) {
        SimpleImageCropper(
            onImageCropped = { croppedUri ->
                println("DEBUG: Imagem cortada recebida: $croppedUri")
                viewModel.onAvatarSelected(croppedUri.toString())
                showPhotoDialog = false
            },
            onCancel = {
                println("DEBUG: Crop cancelado")
                showPhotoDialog = false
            }
        )
    }

    // Atualizar URIs quando o estado mudar
    LaunchedEffect(state.profileImages) {
        selectedImageUris = state.profileImages.map { Uri.parse(it) }
    }
    
    // Debug do estado
    LaunchedEffect(state.avatarUri) {
        println("DEBUG: LaunchedEffect - avatarUri mudou para: ${state.avatarUri}")
    }

    Scaffold(
        topBar = {
            // Header verde
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TaskGoGreenDark) // Verde escuro
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(TGIcons.Back),
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = "Conta",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF424242)) // Fundo cinza escuro
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                items(listOf("profile", "photos", "fields", "save")) { item ->
                    when (item) {
                        "profile" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Foto de perfil circular
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { showPhotoDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    println("DEBUG: Estado atual - avatarUri: ${state.avatarUri}")
                                    if (!state.avatarUri.isNullOrBlank()) {
                                        println("DEBUG: Exibindo AsyncImage com URI: ${state.avatarUri}")
                                        AsyncImage(
                                            model = state.avatarUri,
                                            contentDescription = "Foto de perfil",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        println("DEBUG: Exibindo ícone padrão - avatarUri está vazio")
                                        Icon(
                                            painter = painterResource(TGIcons.Profile),
                                            contentDescription = "Foto de perfil",
                                            modifier = Modifier.size(60.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Nome do usuário
                                Text(
                                    text = state.name.ifBlank { "Carlos Amaral" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                // Profissão
                                Text(
                                    text = state.profession.ifBlank { "Montador de Móveis" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        "photos" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Fotos de Perfil",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                ImageEditor(
                                    selectedImageUris = selectedImageUris,
                                    onImagesChanged = { uris ->
                                        selectedImageUris = uris
                                        viewModel.onProfileImagesChanged(uris.map { it.toString() })
                                    },
                                    maxImages = 5,
                                    placeholderText = "Adicione fotos do seu trabalho"
                                )
                            }
                        }
                        "fields" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Campo Nome Completo
                                Column {
                                    Text(
                                        text = "Nome Completo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = state.name,
                                        onValueChange = { viewModel.onNameChange(it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Gray,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color.White
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                                    )
                                }
                                
                                // Campo Email
                                Column {
                                    Text(
                                        text = "Email",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = state.email,
                                        onValueChange = { viewModel.onEmailChange(it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Gray,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Color.White
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                                    )
                                }
                                
                            }
                        }
                        "save" -> {
                            Button(
                                onClick = { 
                                    viewModel.save()
                                    showSuccessMessage = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F) // Vermelho escuro
                                )
                            ) {
                                Text(
                                    text = "Salvar Alterações",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    // Mensagem de sucesso
    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            title = { Text("Sucesso") },
            text = { Text("Dados salvos com sucesso!") },
            confirmButton = {
                TextButton(onClick = { showSuccessMessage = false }) {
                    Text("OK")
                }
            }
        )
    }
}
