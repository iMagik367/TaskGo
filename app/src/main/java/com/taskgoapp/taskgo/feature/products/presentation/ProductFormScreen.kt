package com.taskgoapp.taskgo.feature.products.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.TGIcons
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showImageEditor by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(productId) {
        try {
            Log.d("ProductFormScreen", "Loading product with ID: $productId")
            viewModel.load(productId)
        } catch (e: Exception) {
            Log.e("ProductFormScreen", "Error loading product", e)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingLauncherAction by remember { mutableStateOf(false) }
    
    val hasImagePermission = remember {
        com.taskgoapp.taskgo.core.permissions.PermissionHandler.hasImageReadPermission(context)
    }
    
    // Photo picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                Log.d("ProductFormScreen", "Image selected: $uri")
                selectedImageUri = uri
                showImageEditor = true
            } catch (e: Exception) {
                Log.e("ProductFormScreen", "Error selecting image", e)
            }
        }
    }
    
    val imagePermissionLauncher = com.taskgoapp.taskgo.core.permissions.rememberImageReadPermissionLauncher(
        onPermissionGranted = {
            pendingLauncherAction = true
        },
        onPermissionDenied = {
            // Permissão negada
        }
    )
    
    // Executar ação do launcher quando permissão for concedida
    LaunchedEffect(pendingLauncherAction) {
        if (pendingLauncherAction && com.taskgoapp.taskgo.core.permissions.PermissionHandler.hasImageReadPermission(context)) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pendingLauncherAction = false
        }
    }
    
    fun openImagePicker() {
        if (hasImagePermission) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            imagePermissionLauncher.launch(com.taskgoapp.taskgo.core.permissions.PermissionHandler.getImageReadPermission())
        }
    }

    // Saved effect
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            try {
                Log.d("ProductFormScreen", "Product saved, navigating back")
                onSaved(uiState.id ?: "")
            } catch (e: Exception) {
                Log.e("ProductFormScreen", "Error navigating after save", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == null) stringResource(R.string.action_add) else stringResource(R.string.action_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(
                            painter = painterResource(TGIcons.Back),
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { 
                            try {
                                if (uiState.canSave) viewModel.save() 
                            } catch (e: Exception) {
                                Log.e("ProductFormScreen", "Error saving product", e)
                            }
                        }, 
                        enabled = uiState.canSave && !uiState.isSaving
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo upload section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Adicione fotos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (uiState.imageUris.isEmpty()) {
                        // Empty state - show add button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { openImagePicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar foto",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Show selected images
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.imageUris) { imageUri ->
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "Imagem do produto",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Remove button
                                    IconButton(
                                        onClick = { viewModel.removeImage(imageUri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remover imagem",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Add more button
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { openImagePicker() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Adicionar mais fotos",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Product name input
            Column {
                Text(
                    text = "Nome do produto",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Digite o nome do produto") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Price input
            Column {
                Text(
                    text = "Preço",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { viewModel.onPriceChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("R$ 0,00") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Description input
            Column {
                Text(
                    text = "Descrição",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Descreva o produto") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Seller name input
            Column {
                Text(
                    text = "Vendedor",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.sellerName,
                    onValueChange = { viewModel.onSellerNameChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nome do vendedor") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { 
                    try {
                        if (uiState.canSave) viewModel.save() 
                    } catch (e: Exception) {
                        Log.e("ProductFormScreen", "Error saving product", e)
                    }
                },
                enabled = uiState.canSave && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (uiState.id == null) "Criar Produto" else "Salvar Alterações",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Error display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Image Editor Dialog
    if (showImageEditor && selectedImageUri != null) {
        ImageEditorDialog(
            imageUri = selectedImageUri!!,
            onConfirm = { finalUri ->
                viewModel.addImage(finalUri.toString())
                showImageEditor = false
                selectedImageUri = null
            },
            onDismiss = {
                showImageEditor = false
                selectedImageUri = null
            }
        )
    }
}

@Composable
private fun ImageEditorDialog(
    imageUri: Uri,
    onConfirm: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var cropMode by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(false) }
    var finalImageUri by remember { mutableStateOf(imageUri) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Editar Imagem",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Image preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = finalImageUri,
                        contentDescription = "Imagem selecionada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Edit options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { cropMode = !cropMode },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (cropMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(if (cropMode) "Cortar ✓" else "Cortar")
                    }
                    
                    OutlinedButton(
                        onClick = { resizeMode = !resizeMode },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (resizeMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(if (resizeMode) "Redimensionar ✓" else "Redimensionar")
                    }
                }
                
                if (cropMode || resizeMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            cropMode && resizeMode -> "Modo: Cortar e Redimensionar"
                            cropMode -> "Modo: Cortar - Arraste para selecionar área"
                            resizeMode -> "Modo: Redimensionar - Use os controles abaixo"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Crop controls
                    if (cropMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Implementar crop */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aplicar Crop")
                            }
                        }
                    }
                    
                    // Resize controls
                    if (resizeMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* Implementar resize */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aplicar Resize")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = { onConfirm(finalImageUri) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}


