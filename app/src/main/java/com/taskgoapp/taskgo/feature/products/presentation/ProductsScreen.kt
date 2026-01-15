package com.taskgoapp.taskgo.feature.products.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
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
import com.taskgoapp.taskgo.core.model.AccountType
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSellerProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: ProductsViewModel = hiltViewModel()
    val products by viewModel.products.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.productCategories.collectAsState()
    val accountType by viewModel.accountType.collectAsState()
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showImagePreview by remember { mutableStateOf<String?>(null) }
    
    val isPartner = accountType == com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO || 
                    accountType == com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR || 
                    accountType == com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR
    
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
                title = "Loja",
                subtitle = "Encontre produtos para suas necessidades",
                onBackClick = null, // Sem botão de voltar
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
                subtitleColor = Color.White,
                backIconColor = Color.White,
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier.size(36.dp)
                        ) {
                            TGIcon(
                                iconRes = TGIcons.Search,
                                contentDescription = "Buscar",
                                size = TGIcons.Sizes.Medium,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier.size(36.dp)
                        ) {
                            TGIcon(
                                iconRes = TGIcons.Bell,
                                contentDescription = "Notificações",
                                size = TGIcons.Sizes.Medium,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onNavigateToCart,
                            modifier = Modifier.size(36.dp)
                        ) {
                            TGIcon(
                                iconRes = TGIcons.Cart,
                                contentDescription = "Carrinho",
                                size = TGIcons.Sizes.Medium,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onNavigateToMessages,
                            modifier = Modifier.size(36.dp)
                        ) {
                            TGIcon(
                                iconRes = TGIcons.Messages,
                                contentDescription = "Mensagens",
                                size = TGIcons.Sizes.Medium,
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB de adicionar produto (apenas para PARCEIRO/VENDEDOR) no canto inferior direito
            if (isPartner) {
                FloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    containerColor = TaskGoGreen
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Produto",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                placeholder = "Buscar produtos...",
                modifier = Modifier.padding(top = 4.dp)
            )
            
                // Barra de Filtros e botões de ordenação (tudo no mesmo scroll)
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
                    showSortButtons = true,
                    sortBy = filterState.sortBy,
                    onSortByRating = { viewModel.updateFilterState(filterState.copy(sortBy = com.taskgoapp.taskgo.core.design.SortOption.RATING)) },
                    onSortByNewest = { viewModel.updateFilterState(filterState.copy(sortBy = com.taskgoapp.taskgo.core.design.SortOption.NEWEST)) },
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            
            // Grid de produtos
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum produto encontrado",
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
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = { onNavigateToProductDetail(product.id) },
                            onImageClick = { imageUrl ->
                                showImagePreview = imageUrl
                            },
                            onSellerClick = onNavigateToSellerProfile
                        )
                    }
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
private fun ProductCard(
    product: Product,
    onProductClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onSellerClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
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

                if (product.featured == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = TaskGoOrange,
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