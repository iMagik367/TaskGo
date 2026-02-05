package com.taskgoapp.taskgo.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: UniversalSearchViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Busca Universal",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barra de Busca
            SearchBar(
                query = filterState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                placeholder = "Buscar produtos, serviços, categorias...",
                modifier = Modifier.fillMaxWidth()
            )
            
            // Barra de Filtros Rápidos
            FilterBar(
                categories = uiState.categories,
                selectedCategories = filterState.selectedCategories,
                onCategorySelected = { category ->
                    if (category == "Todos") {
                        viewModel.updateFilterState(filterState.copy(selectedCategories = emptySet()))
                    } else {
                        viewModel.toggleCategory(category)
                    }
                },
                onFilterClick = { showFilterSheet = true },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Resultados
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.products.isEmpty() && uiState.services.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhum resultado encontrado",
                                style = FigmaProductName,
                                color = TaskGoTextGray
                            )
                            Text(
                                text = "Tente ajustar os filtros ou buscar por outros termos",
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Seção de Produtos
                        if (uiState.products.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Produtos (${uiState.products.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextBlack
                                )
                            }
                            items(uiState.products) { product ->
                                ProductSearchResultCard(
                                    product = product,
                                    onClick = { onNavigateToProduct(product.id) }
                                )
                            }
                        }
                        
                        // Seção de Serviços
                        if (uiState.services.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Serviços (${uiState.services.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextBlack,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(uiState.services) { service ->
                                ServiceSearchResultCard(
                                    service = service,
                                    onClick = { onNavigateToService(service.id) }
                                )
                            }
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
}

@Composable
private fun ProductSearchResultCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagem do produto (placeholder)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(TaskGoSurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IMG",
                    color = TaskGoTextGray,
                    style = FigmaProductDescription
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.title,
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                product.description?.let {
                    Text(
                        text = it,
                        style = FigmaProductDescription,
                        color = TaskGoTextGray,
                        maxLines = 2
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R$ ${String.format("%.2f", product.price)}",
                        style = FigmaProductName,
                        color = TaskGoGreen,
                        fontWeight = FontWeight.Bold
                    )
                    product.rating?.let { rating ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = FigmaProductDescription,
                                color = TaskGoTextGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceSearchResultCard(
    service: ServiceOrder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
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
                Text(
                    text = service.category,
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                service.rating?.let { rating ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format("%.1f", rating),
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
            Text(
                text = service.description,
                style = FigmaProductDescription,
                color = TaskGoTextGray,
                maxLines = 2
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TaskGoTextGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${service.city}, ${service.state}",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
            }
        }
    }
}

