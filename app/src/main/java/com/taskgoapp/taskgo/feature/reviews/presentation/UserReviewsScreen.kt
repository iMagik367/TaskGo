package com.taskgoapp.taskgo.feature.reviews.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.reviews.ReviewCard
import com.taskgoapp.taskgo.core.design.reviews.ReviewSummaryCard
import com.taskgoapp.taskgo.core.design.reviews.RatingStarsDisplay
import com.taskgoapp.taskgo.core.model.Review
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReviewsScreen(
    userId: String,
    userName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: UserReviewsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Recebidas", "Enviadas")
    
    // Filtros e busca
    var searchQuery by remember { mutableStateOf("") }
    var selectedRatingFilter by remember { mutableStateOf<Int?>(null) } // null = todos, 1-5 = filtro por estrelas
    var selectedTypeFilter by remember { mutableStateOf<ReviewType?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.RECENT) }
    var showFilters by remember { mutableStateOf(false) }
    
    LaunchedEffect(userId) {
        viewModel.loadUserReviews(userId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Avaliações de $userName",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de busca e filtros
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Campo de busca
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar avaliações...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    // Botões de filtro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botão de filtros
                        FilterChip(
                            selected = showFilters,
                            onClick = { showFilters = !showFilters },
                            label = { Text("Filtros") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        // Filtro de ordenação
                        var expandedSort by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = false,
                                onClick = { expandedSort = true },
                                label = { Text("Ordenar: ${sortOrder.label}") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = expandedSort,
                                onDismissRequest = { expandedSort = false }
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            sortOrder = order
                                            expandedSort = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Painel de filtros expandido
                    if (showFilters) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider()
                            
                            // Filtro por avaliação
                            Text(
                                text = "Avaliação:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = selectedRatingFilter == null,
                                    onClick = { selectedRatingFilter = null },
                                    label = { Text("Todas") }
                                )
                                (5 downTo 1).forEach { rating ->
                                    FilterChip(
                                        selected = selectedRatingFilter == rating,
                                        onClick = { 
                                            selectedRatingFilter = if (selectedRatingFilter == rating) null else rating
                                        },
                                        label = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text("$rating")
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Filtro por tipo
                            Text(
                                text = "Tipo:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTypeFilter == null,
                                    onClick = { selectedTypeFilter = null },
                                    label = { Text("Todos") }
                                )
                                ReviewType.values().forEach { type ->
                                    FilterChip(
                                        selected = selectedTypeFilter == type,
                                        onClick = { 
                                            selectedTypeFilter = if (selectedTypeFilter == type) null else type
                                        },
                                        label = { Text(type.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content
            when (selectedTabIndex) {
                0 -> {
                    // Avaliações sobre o usuário (quando é prestador/vendedor)
                    ReviewsAboutUserContent(
                        uiState = uiState,
                        userId = userId,
                        searchQuery = searchQuery,
                        ratingFilter = selectedRatingFilter,
                        typeFilter = selectedTypeFilter,
                        sortOrder = sortOrder
                    )
                }
                1 -> {
                    // Avaliações que o usuário fez
                    UserReviewsAsReviewerContent(
                        uiState = uiState,
                        userId = userId,
                        searchQuery = searchQuery,
                        ratingFilter = selectedRatingFilter,
                        typeFilter = selectedTypeFilter,
                        sortOrder = sortOrder
                    )
                }
            }
        }
    }
}

enum class SortOrder(val label: String) {
    RECENT("Mais recentes"),
    OLDEST("Mais antigas"),
    HIGHEST_RATING("Maior avaliação"),
    LOWEST_RATING("Menor avaliação")
}

val ReviewType.label: String
    get() = when (this) {
        ReviewType.PRODUCT -> "Produto"
        ReviewType.SERVICE -> "Serviço"
        ReviewType.PROVIDER -> "Prestador"
    }

@Composable
private fun ReviewsAboutUserContent(
    uiState: UserReviewsUiState,
    userId: String,
    searchQuery: String,
    ratingFilter: Int?,
    typeFilter: ReviewType?,
    sortOrder: SortOrder
) {
    // Filtrar e ordenar avaliações
    val filteredReviews = remember(uiState.reviewsAsTarget, searchQuery, ratingFilter, typeFilter, sortOrder) {
        uiState.reviewsAsTarget
            .filter { review ->
                // Filtro de busca
                val matchesSearch = searchQuery.isBlank() || 
                    review.comment?.contains(searchQuery, ignoreCase = true) == true ||
                    review.reviewerName.contains(searchQuery, ignoreCase = true)
                
                // Filtro de avaliação
                val matchesRating = ratingFilter == null || review.rating == ratingFilter
                
                // Filtro de tipo
                val matchesType = typeFilter == null || review.type == typeFilter
                
                matchesSearch && matchesRating && matchesType
            }
            .sortedWith(
                when (sortOrder) {
                    SortOrder.RECENT -> compareByDescending { it.createdAt }
                    SortOrder.OLDEST -> compareBy { it.createdAt }
                    SortOrder.HIGHEST_RATING -> compareByDescending { it.rating }
                    SortOrder.LOWEST_RATING -> compareBy { it.rating }
                }
            )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Resumo de avaliações
        if (uiState.summaryAsTarget.totalReviews > 0) {
            item {
                ReviewSummaryCard(summary = uiState.summaryAsTarget)
            }
        }
        
        // Contador de resultados
        if (filteredReviews.isNotEmpty() && (searchQuery.isNotBlank() || ratingFilter != null || typeFilter != null)) {
            item {
                Text(
                    text = "${filteredReviews.size} avaliação(ões) encontrada(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        // Lista de avaliações
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredReviews.isEmpty()) {
            item {
                EmptyReviewsState(
                    title = if (searchQuery.isNotBlank() || ratingFilter != null || typeFilter != null) {
                        "Nenhuma avaliação encontrada"
                    } else {
                        "Nenhuma avaliação ainda"
                    },
                    message = if (searchQuery.isNotBlank() || ratingFilter != null || typeFilter != null) {
                        "Tente ajustar os filtros de busca"
                    } else {
                        "Você ainda não recebeu avaliações como prestador/vendedor"
                    }
                )
            }
        } else {
            items(filteredReviews) { review ->
                ReviewCard(
                    review = review,
                    onHelpfulClick = null // Não permitir marcar como útil suas próprias avaliações
                )
            }
        }
    }
}

@Composable
private fun UserReviewsAsReviewerContent(
    uiState: UserReviewsUiState,
    userId: String,
    searchQuery: String,
    ratingFilter: Int?,
    typeFilter: ReviewType?,
    sortOrder: SortOrder
) {
    // Filtrar e ordenar avaliações
    val filteredReviews = remember(uiState.reviewsAsReviewer, searchQuery, ratingFilter, typeFilter, sortOrder) {
        uiState.reviewsAsReviewer
            .filter { review ->
                // Filtro de busca
                val matchesSearch = searchQuery.isBlank() || 
                    review.comment?.contains(searchQuery, ignoreCase = true) == true
                
                // Filtro de avaliação
                val matchesRating = ratingFilter == null || review.rating == ratingFilter
                
                // Filtro de tipo
                val matchesType = typeFilter == null || review.type == typeFilter
                
                matchesSearch && matchesRating && matchesType
            }
            .sortedWith(
                when (sortOrder) {
                    SortOrder.RECENT -> compareByDescending { it.createdAt }
                    SortOrder.OLDEST -> compareBy { it.createdAt }
                    SortOrder.HIGHEST_RATING -> compareByDescending { it.rating }
                    SortOrder.LOWEST_RATING -> compareBy { it.rating }
                }
            )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Estatísticas das avaliações feitas
        if (filteredReviews.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Total de Avaliações",
                            style = FigmaProductDescription,
                            color = TaskGoTextGray
                        )
                        Text(
                            text = "${filteredReviews.size}",
                            style = FigmaProductName,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                        
                        val averageRating = filteredReviews.map { it.rating.toDouble() }.average()
                        if (averageRating > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            RatingStarsDisplay(
                                rating = averageRating,
                                starSize = 20.dp,
                                showRating = true
                            )
                        }
                    }
                }
            }
        }
        
        // Contador de resultados
        if (filteredReviews.isNotEmpty() && (searchQuery.isNotBlank() || ratingFilter != null || typeFilter != null)) {
            item {
                Text(
                    text = "${filteredReviews.size} avaliação(ões) encontrada(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        // Lista de avaliações
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredReviews.isEmpty()) {
            item {
                EmptyReviewsState(
                    title = if (searchQuery.isNotBlank() || ratingFilter != null || typeFilter != null) {
                        "Nenhuma avaliação encontrada"
                    } else {
                        "Nenhuma avaliação feita"
                    },
                    message = if (searchQuery.isNotBlank() || ratingFilter != null || typeFilter != null) {
                        "Tente ajustar os filtros de busca"
                    } else {
                        "Você ainda não avaliou nenhum produto ou serviço"
                    }
                )
            }
        } else {
            // Agrupar por tipo (se não houver filtro de tipo)
            val groupedByType = typeFilter == null
            val productReviews = filteredReviews.filter { it.type == ReviewType.PRODUCT }
            val serviceReviews = filteredReviews.filter { it.type == ReviewType.SERVICE }
            val providerReviews = filteredReviews.filter { it.type == ReviewType.PROVIDER }
            
            if (groupedByType) {
                if (productReviews.isNotEmpty()) {
                    item {
                        SectionHeader("Produtos (${productReviews.size})")
                    }
                    items(productReviews) { review ->
                        ReviewCardWithTarget(
                            review = review,
                            targetType = "Produto"
                        )
                    }
                }
                
                if (serviceReviews.isNotEmpty()) {
                    item {
                        SectionHeader("Serviços (${serviceReviews.size})")
                    }
                    items(serviceReviews) { review ->
                        ReviewCardWithTarget(
                            review = review,
                            targetType = "Serviço"
                        )
                    }
                }
                
                if (providerReviews.isNotEmpty()) {
                    item {
                        SectionHeader("Prestadores (${providerReviews.size})")
                    }
                    items(providerReviews) { review ->
                        ReviewCardWithTarget(
                            review = review,
                            targetType = "Prestador"
                        )
                    }
                }
            } else {
                // Se houver filtro de tipo, mostrar todas juntas
                items(filteredReviews) { review ->
                    ReviewCardWithTarget(
                        review = review,
                        targetType = review.type.label
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCardWithTarget(
    review: Review,
    targetType: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge do tipo
            Badge(
                containerColor = TaskGoGreen.copy(alpha = 0.2f),
                contentColor = TaskGoGreen
            ) {
                Text(
                    text = targetType,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Card de avaliação normal
            ReviewCard(
                review = review,
                onHelpfulClick = null
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = FigmaProductName,
        fontWeight = FontWeight.Bold,
        color = TaskGoTextBlack,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EmptyReviewsState(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TaskGoTextGray.copy(alpha = 0.5f)
            )
            Text(
                text = title,
                style = FigmaProductName,
                color = TaskGoTextGray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = FigmaProductDescription,
                color = TaskGoTextGray
            )
        }
    }
}

