package com.taskgoapp.taskgo.feature.services.presentation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.ImageEditor
import com.taskgoapp.taskgo.core.design.EnhancedOutlinedTextField
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import com.taskgoapp.taskgo.core.model.AccountType
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceFormScreen(
    serviceId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ServiceFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Carregar serviço se estiver editando ou carregar categorias preferidas se for novo
    LaunchedEffect(serviceId) {
        if (serviceId != null) {
            viewModel.loadService(serviceId)
        } else {
            viewModel.loadUserPreferredCategories()
        }
    }

    // Navegar quando salvo
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    // Categorias padrão
    val categories = remember {
        listOf(
            ServiceCategory(1, "Montagem", "build", "Serviços de montagem"),
            ServiceCategory(2, "Reforma", "home", "Reformas e construções"),
            ServiceCategory(3, "Jardinagem", "eco", "Jardinagem"),
            ServiceCategory(4, "Elétrica", "flash_on", "Serviços elétricos"),
            ServiceCategory(5, "Encanamento", "plumbing", "Encanamento"),
            ServiceCategory(6, "Pintura", "format_paint", "Pintura"),
            ServiceCategory(7, "Limpeza", "cleaning_services", "Limpeza"),
            ServiceCategory(8, "Outros", "more_horiz", "Outros")
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (serviceId == null) "Novo Serviço" else "Editar Serviço",
                onBackClick = onBack,
                backgroundColor = TaskGoGreen,
                titleColor = TaskGoBackgroundWhite,
                backIconColor = TaskGoBackgroundWhite
            )
        }
    ) { paddingValues ->
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            EnhancedOutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Título do Serviço *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Descrição
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Descrição *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = TaskGoTextGray
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            )

            // Categoria principal (para compatibilidade) - APENAS se NÃO for prestador
            if (uiState.accountType != AccountType.PRESTADOR) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = uiState.category.ifEmpty { "Selecione uma categoria principal" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria Principal *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaskGoGreen,
                            unfocusedBorderColor = TaskGoTextGray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.updateCategory(category.name)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Categorias que o prestador oferece (checkboxes)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Categorias que você oferece",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecione todas as categorias de serviços que você pode realizar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleCategory(category.name) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Checkbox(
                                checked = uiState.selectedCategories.contains(category.name),
                                onCheckedChange = { viewModel.toggleCategory(category.name) }
                            )
                        }
                    }
                }
            }

            // Preço (não exibir para prestadores)
            if (uiState.accountType != com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR) {
                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { newValue ->
                        val formatted = com.taskgoapp.taskgo.core.utils.TextFormatters.formatPrice(newValue)
                        viewModel.updatePrice(formatted)
                    },
                    label = { Text("Preço (R$) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("R$ ") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoTextGray
                    )
                )
            }

            // Imagens
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Imagens",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ImageEditor(
                        selectedImageUris = uiState.imageUris,
                        onImagesChanged = { viewModel.updateImageUris(it) },
                        maxImages = 10
                    )
                }
            }

            // Vídeos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vídeos (MP4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    VideoPicker(
                        selectedVideoUris = uiState.videoUris,
                        onVideosChanged = { viewModel.updateVideoUris(it) },
                        maxVideos = 3
                    )
                }
            }

            // Status Ativo/Inativo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Serviço Ativo",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = uiState.isActive,
                    onCheckedChange = { viewModel.toggleActive() }
                )
            }

            // Progresso de upload
            if (uiState.isSaving && uiState.uploadProgress > 0f) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enviando mídia... ${(uiState.uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    LinearProgressIndicator(
                        progress = { uiState.uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = TaskGoGreen
                    )
                }
            }

            // Erro
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TaskGoError.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = TaskGoError
                    )
                }
            }

            // Botão Salvar
            Button(
                onClick = { viewModel.saveService(onSaved) },
                enabled = viewModel.canSave() && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (serviceId == null) "Criar Serviço" else "Salvar Alterações",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VideoPicker(
    selectedVideoUris: List<Uri>,
    onVideosChanged: (List<Uri>) -> Unit,
    maxVideos: Int = 3
) {
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf(false) }
    
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val newVideos = selectedVideoUris + it
            onVideosChanged(newVideos.take(maxVideos))
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(selectedVideoUris) { videoUri ->
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TaskGoBackgroundGray)
                    .border(2.dp, TaskGoTextGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Vídeo",
                        modifier = Modifier.size(48.dp),
                        tint = TaskGoTextGray
                    )
                    Text(
                        text = "Vídeo MP4",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
                
                IconButton(
                    onClick = { onVideosChanged(selectedVideoUris - videoUri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover",
                        tint = TaskGoError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (selectedVideoUris.size < maxVideos) {
            item {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TaskGoSurface)
                        .border(2.dp, TaskGoGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { videoLauncher.launch("video/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar vídeo",
                            modifier = Modifier.size(32.dp),
                            tint = TaskGoGreen
                        )
                        Text(
                            text = "Adicionar\nVídeo",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoGreen
                        )
                    }
                }
            }
        }
    }
}

