package com.taskgoapp.taskgo.feature.products.presentation
import com.taskgoapp.taskgo.core.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeusProdutosScreen(
    onBackClick: () -> Unit,
    onCriarProduto: () -> Unit,
    onEditarProduto: (String) -> Unit,
    viewModel: MeusProdutosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedProducts by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Gerencie seus Produtos",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barra de pesquisa
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar produtos...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Pesquisar"
                    )
                },
                singleLine = true
            )
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
                        text = uiState.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (uiState.products.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum produto encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Clique no botão + para criar seu primeiro produto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Lista de produtos com select boxes
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val filteredProducts = uiState.products.filter { 
                        it.title.contains(searchQuery, ignoreCase = true) 
                    }
                    items(filteredProducts.size) { index ->
                        val product = filteredProducts[index]
                        ProductCardWithCheckbox(
                            product = product,
                            isSelected = selectedProducts.contains(product.id),
                            onSelectionChange = { isSelected ->
                                selectedProducts = if (isSelected) {
                                    selectedProducts + product.id
                                } else {
                                    selectedProducts - product.id
                                }
                            },
                            onEdit = { onEditarProduto(product.id) },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
                
                // Botões na parte inferior
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCriarProduto,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoSuccessGreen
                        )
                    ) {
                        Text(
                            text = "Criar Produto",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                }
            }
        }
    }
}

@Composable
private fun ProductCardWithCheckbox(
    product: com.taskgoapp.taskgo.core.model.Product,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TaskGoSurfaceGrayBg else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = TaskGoSuccessGreen
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Product image - melhorado para exibir corretamente
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUris.isNotEmpty() && product.imageUris.first().isNotBlank()) {
                    AsyncImage(
                        model = product.imageUris.first(),
                        contentDescription = "Imagem do produto ${product.title}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(TGIcons.Package),
                        contentDescription = "Produto sem imagem",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "R$ ${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TaskGoSuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                product.description?.let { description ->
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
                // Badge de produto em destaque
                if (product.featured == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Em destaque",
                            modifier = Modifier.size(14.dp),
                            tint = TaskGoSuccessGreen
                        )
                        Text(
                            text = "Em destaque",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoSuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action buttons
            Column(
                horizontalAlignment = Alignment.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = TaskGoSuccessGreen
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: com.taskgoapp.taskgo.core.model.Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            if (product.imageUris.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUris.first(),
                    contentDescription = "Imagem do produto ${product.title}",
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Package),
                        contentDescription = "Produto sem imagem",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "R$ ${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                product.description?.let { description ->
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }

            // Action buttons
            Column(
                horizontalAlignment = Alignment.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
