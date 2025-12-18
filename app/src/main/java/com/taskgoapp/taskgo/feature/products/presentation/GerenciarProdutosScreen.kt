package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextDark
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoSurfaceGray
import com.taskgoapp.taskgo.core.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarProdutosScreen(
    onBackClick: () -> Unit,
    onCriarProduto: () -> Unit,
    onEditarProduto: (String) -> Unit,
    viewModel: GerenciarProdutosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showImagePreview by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gerenciar Produtos",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCriarProduto,
                containerColor = TaskGoGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Criar Produto",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "Erro ao carregar produtos",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (uiState.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nenhum produto encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = "Crie seu primeiro produto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.products) { product ->
                        ProductCard(
                            product = product,
                            onEditClick = { onEditarProduto(product.id) },
                            onDeleteClick = { viewModel.deleteProduct(product.id) },
                            onImageClick = { imageUrl ->
                                showImagePreview = imageUrl
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Image Preview Modal
    showImagePreview?.let { imageUrl ->
        ImagePreviewModal(
            imageUrl = imageUrl,
            onDismiss = { showImagePreview = null }
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "R$ ${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
                    )
                }
            }
            
            // Product image preview - exibir sempre, mesmo se vazio
            val firstImageUri = product.imageUris.firstOrNull { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TaskGoSurfaceGray)
                    .then(
                        if (firstImageUri != null) {
                            Modifier.clickable { onImageClick(firstImageUri) }
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (firstImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(firstImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagem do produto ${product.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder quando não há imagem
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(com.taskgoapp.taskgo.core.design.TGIcons.Products),
                                contentDescription = "Sem imagem",
                                modifier = Modifier.size(48.dp),
                                tint = TaskGoTextGray
                            )
                            Text(
                                text = "Sem imagem",
                                style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
            
            product.description?.let { description ->
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextDark,
                        maxLines = 3
                    )
                }
            }
            
            if (product.imageUris.size > 1) {
                Text(
                    text = "📷 ${product.imageUris.size} imagens",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir")
                }
                
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewModal(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview da imagem",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}
