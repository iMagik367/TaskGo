package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.FilterBar
import com.taskgoapp.taskgo.core.design.FilterBottomSheet
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalServiceOrdersScreen(
    onBackClick: () -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: LocalServiceOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoriesFull by viewModel.serviceCategoriesFull.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val categoriesWithAll = remember(categories) {
        if (categories.isEmpty() || categories.first() != "Todos") {
            listOf("Todos") + categories.filter { it != "Todos" }
        } else {
            categories
        }
    }
    
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Ordens de Serviço Locais",
                subtitle = "Encontre oportunidades na sua região",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra de Busca
            com.taskgoapp.taskgo.core.design.SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Buscar ordens de serviço...",
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
            showSortButtons = true,
            sortBy = filterState.sortBy,
            onSortByRating = { viewModel.updateFilterState(filterState.copy(sortBy = com.taskgoapp.taskgo.core.design.SortOption.RATING)) },
            onSortByNewest = { viewModel.updateFilterState(filterState.copy(sortBy = com.taskgoapp.taskgo.core.design.SortOption.NEWEST)) },
            modifier = Modifier.padding(vertical = 2.dp)
            )
            
            val errorMessage = uiState.error
            
            // Mostrar categorias primeiro, depois ordens ao selecionar categoria
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TaskGoGreen)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            } else if (selectedCategory == null) {
                // Mostrar cards de categorias
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categoriesFull) { category ->
                        LocalServiceOrdersCategoryCard(
                            category = category,
                            onCategoryClick = { 
                                selectedCategory = category.name
                                viewModel.updateSelectedCategory(category.name)
                            }
                        )
                    }
                }
            } else {
                // Mostrar ordens filtradas por categoria
                val filteredOrders = orders.filter { 
                    it.category?.equals(selectedCategory, ignoreCase = true) == true
                }
                
                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhuma ordem encontrada",
                                style = MaterialTheme.typography.titleMedium,
                                color = TaskGoTextGray
                            )
                            Text(
                                text = "Não há ordens de serviço disponíveis para esta categoria",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextGray
                            )
                            TextButton(onClick = { 
                                selectedCategory = null
                                viewModel.updateSelectedCategory(null)
                            }) {
                                Text("Voltar")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory ?: "",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { 
                                    selectedCategory = null
                                    viewModel.updateSelectedCategory(null)
                                }) {
                                    Text("Voltar")
                                }
                            }
                        }
                        items(filteredOrders) { order ->
                            ServiceOrderCard(
                                order = order,
                                isOwnOrder = uiState.currentUserId == order.clientId,
                                onClick = { onOrderClick(order.id) }
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
}

@Composable
private fun ServiceOrderCard(
    order: OrderFirestore,
    isOwnOrder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val cardColor = if (isOwnOrder) {
        TaskGoGreen.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagem (placeholder por enquanto)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TaskGoGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📋", fontSize = 32.sp)
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Título
                Text(
                    text = order.details.takeIf { it.isNotBlank() } ?: "Ordem de Serviço",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Localização
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TaskGoTextGray
                    )
                    Text(
                        text = order.location.takeIf { it.isNotBlank() } ?: "Localização não informada",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Valor
                if (order.budget > 0) {
                    Text(
                        text = currencyFormat.format(order.budget),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
                    )
                } else {
                    Text(
                        text = "Orçamento a combinar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray
                    )
                }
                
                // Badge se for própria ordem
                if (isOwnOrder) {
                    Surface(
                        color = TaskGoGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Sua ordem",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TaskGoGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalServiceOrdersCategoryCard(
    category: ServiceCategory,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCategoryClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(TaskGoGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category.icon) {
                        "build" -> Icons.Default.Build
                        "home" -> Icons.Default.Home
                        "eco" -> Icons.Default.Eco
                        "flash_on" -> Icons.Default.FlashOn
                        "plumbing" -> Icons.Default.Plumbing
                        "format_paint" -> Icons.Default.FormatPaint
                        "cleaning_services" -> Icons.Default.CleaningServices
                        else -> Icons.Default.List
                    },
                    contentDescription = category.name,
                    tint = TaskGoGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaskGoTextDark
                )
                Text(
                    text = category.description,
                    fontSize = 14.sp,
                    color = TaskGoTextGray,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ver mais",
                tint = TaskGoTextGray
            )
        }
    }
}

