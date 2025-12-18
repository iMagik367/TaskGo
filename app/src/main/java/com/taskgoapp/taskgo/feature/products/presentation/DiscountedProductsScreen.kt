package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.design.TGIcon
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.design.FilterBar
import com.taskgoapp.taskgo.core.design.FilterBottomSheet
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Star

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountedProductsScreen(
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProductsViewModel = hiltViewModel()
    val allProducts by viewModel.products.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.productCategories.collectAsState()
    
    // Filtrar apenas produtos com desconto
    val discountedProducts = remember(allProducts) {
        allProducts.filter { product ->
            // Assumindo que produtos com desconto têm um campo discount ou price menor que originalPrice
            // Por enquanto, vamos mostrar todos os produtos, mas você pode adicionar lógica de desconto
            true // TODO: Filtrar por produtos que realmente têm desconto
        }
    }
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showImagePreview by remember { mutableStateOf<String?>(null) }
    
    // Adicionar "Todos" no início das categorias se não estiver presente
    val categoriesWithAll = remember(categories) {
        if (categories.isEmpty() || categories.first() != "Todos") {
            listOf("Todos") + categories.filter { it != "Todos" }
        } else {
            categories
        }
    }
    
    // Atualizar busca quando o usuário digitar
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Produtos com Descontos",
                subtitle = "Aproveite os melhores descontos do dia!",
                onBackClick = onBackClick,
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
                subtitleColor = Color.White,
                backIconColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Barra de Busca
            com.taskgoapp.taskgo.core.design.SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Buscar produtos com desconto...",
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Barra de Filtros
            FilterBar(
                categories = categoriesWithAll,
                selectedCategories = filterState.selectedCategories,
                onCategorySelected = { category ->
                    if (category == "Todos") {
                        viewModel.updateFilterState(filterState.copy(selectedCategories = emptySet()))
                    } else {
                        viewModel.toggleCategory(category)
                    }
                },
                onFilterClick = { showFilterSheet = true },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Grid de produtos
            if (discountedProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum produto com desconto encontrado",
                        color = TaskGoTextGray
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(discountedProducts) { product ->
                        DiscountedProductCard(
                            product = product,
                            onProductClick = { onNavigateToProductDetail(product.id) },
                            onImageClick = { imageUrl ->
                                showImagePreview = imageUrl
                            }
                        )
                    }
                }
            }
        }
        
        // Bottom Sheet de Filtros
        FilterBottomSheet(
            isOpen = showFilterSheet,
            onDismiss = { showFilterSheet = false },
            filterState = filterState,
            onFilterStateChange = { newState ->
                viewModel.updateFilterState(newState)
            }
        )
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
private fun DiscountedProductCard(
    product: Product,
    onProductClick: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
    }
    val imageUri = remember(product.imageUris) {
        product.imageUris.firstOrNull { it.isNotBlank() }
    }
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(TaskGoSurfaceGray)
                    .clickable { 
                        if (imageUri != null) {
                            onImageClick(imageUri)
                        } else {
                            onProductClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagem do produto ${product.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    TGIcon(
                        iconRes = TGIcons.Products,
                        contentDescription = null,
                        size = TGIcons.Sizes.Large,
                        tint = TaskGoTextGray
                    )
                }
                
                // Badge de desconto
                if (product.featured == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = TaskGoError,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Promoção",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { onProductClick() },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.title,
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                product.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Preço",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = currencyFormat.format(product.price),
                            style = FigmaPrice,
                            color = TaskGoPriceGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Avaliação",
                            tint = TaskGoStarYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "%.1f".format(product.rating ?: 0.0),
                            style = FigmaRatingText,
                            color = TaskGoTextDark
                        )
                    }
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
    
    Dialog(onDismissRequest = onDismiss) {
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

