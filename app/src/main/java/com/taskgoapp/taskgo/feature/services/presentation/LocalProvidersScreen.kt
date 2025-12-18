package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.design.FilterBar
import com.taskgoapp.taskgo.core.design.FilterBottomSheet
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalProvidersScreen(
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateToCreateWorkOrder: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("LocalProvidersScreen", "=== Iniciando LocalProvidersScreen ===")
    
    val viewModel: LocalProvidersViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val filteredProviders by viewModel.filteredProviders.collectAsState()
    val categories by viewModel.serviceCategories.collectAsState()
    
    android.util.Log.d("LocalProvidersScreen", "Estado carregado - isLoading: ${uiState.isLoading}, error: ${uiState.error}, filteredProviders: ${filteredProviders.size}")
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categoriesFull by viewModel.serviceCategoriesFull.collectAsState()
    
    // Atualizar busca no ViewModel quando searchQuery mudar
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }
    
    // Adicionar "Todos" no início das categorias
    val categoriesWithAll = remember(categories) {
        listOf("Todos") + categories.filter { it != "Todos" }
    }
    
    // Separar prestadores em destaque (top 3) e outros (resto)
    val providersWithLargeBanner = remember(filteredProviders) {
        filteredProviders.take(3).map { providerWithScore ->
            ProviderBanner(
                id = providerWithScore.provider.uid,
                name = providerWithScore.provider.displayName ?: "Prestador",
                category = providerWithScore.provider.preferredCategories?.firstOrNull() ?: "Serviços",
                description = "Prestador com excelente avaliação",
                rating = providerWithScore.provider.rating?.toFloat() ?: 0f,
                bannerType = BannerType.LARGE,
                imageUrl = providerWithScore.provider.photoURL
            )
        }
    }
    
    val providersWithSmallBanner = remember(filteredProviders) {
        filteredProviders.drop(3).map { providerWithScore ->
            ProviderBanner(
                id = providerWithScore.provider.uid,
                name = providerWithScore.provider.displayName ?: "Prestador",
                category = providerWithScore.provider.preferredCategories?.firstOrNull() ?: "Serviços",
                description = "Prestador com excelente avaliação",
                rating = providerWithScore.provider.rating?.toFloat() ?: 0f,
                bannerType = BannerType.SMALL,
                imageUrl = providerWithScore.provider.photoURL
            )
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Prestadores Locais",
                subtitle = "Encontre prestadores de serviços na sua região",
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
                placeholder = "Buscar prestadores...",
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
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            // Conteúdo principal com scroll
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Erro ao carregar prestadores",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (selectedCategory == null) {
                // Mostrar cards de categoria
                if (categoriesFull.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma categoria disponível",
                            color = TaskGoTextGray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoriesFull) { category ->
                            LocalProvidersCategoryCard(
                                category = category,
                                onCategoryClick = { 
                                    selectedCategory = category.name
                                    viewModel.updateSelectedCategory(category.name)
                                }
                            )
                        }
                    }
                }
            } else if (filteredProviders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nenhum prestador encontrado",
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextDark
                            )
                            TextButton(onClick = { 
                                selectedCategory = null
                                viewModel.updateSelectedCategory(null)
                            }) {
                                Text("Voltar")
                            }
                        }
                    }
                    // Seção de Banners Grandes (Retrato) - Scroll Horizontal
                    if (providersWithLargeBanner.isNotEmpty()) {
                        android.util.Log.d("LocalProvidersScreen", "Renderizando ${providersWithLargeBanner.size} banners grandes")
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Prestadores em Destaque",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextDark
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(providersWithLargeBanner.size) { index ->
                                        if (index < providersWithLargeBanner.size) {
                                            val provider = providersWithLargeBanner[index]
                                            android.util.Log.d("LocalProvidersScreen", "Renderizando banner grande $index: ${provider.name}")
                                            LargeBannerCard(
                                                provider = provider,
                                                onProviderClick = { 
                                                    android.util.Log.d("LocalProvidersScreen", "Banner grande clicado: ${provider.id}")
                                                    onNavigateToServiceDetail(provider.id) 
                                                }
                                            )
                                        } else {
                                            android.util.Log.e("LocalProvidersScreen", "Índice fora dos limites: $index")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        android.util.Log.d("LocalProvidersScreen", "Nenhum banner grande para exibir")
                    }
                    
                    // Seção de Banners Pequenos (Quadrado) - Grid Vertical
                    if (providersWithSmallBanner.isNotEmpty()) {
                        android.util.Log.d("LocalProvidersScreen", "Renderizando ${providersWithSmallBanner.size} banners pequenos")
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Outros Prestadores",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextDark
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(providersWithSmallBanner.size) { index ->
                                        if (index < providersWithSmallBanner.size) {
                                            val provider = providersWithSmallBanner[index]
                                            android.util.Log.d("LocalProvidersScreen", "Renderizando banner pequeno $index: ${provider.name}")
                                            SmallBannerCard(
                                                provider = provider,
                                                onProviderClick = { 
                                                    android.util.Log.d("LocalProvidersScreen", "Banner pequeno clicado: ${provider.id}")
                                                    onNavigateToServiceDetail(provider.id)
                                                }
                                            )
                                        } else {
                                            android.util.Log.e("LocalProvidersScreen", "Índice fora dos limites: $index")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        android.util.Log.d("LocalProvidersScreen", "Nenhum banner pequeno para exibir")
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
private fun LocalProvidersCategoryCard(
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
                Text(
                    text = when (category.icon) {
                        "build" -> "🔧"
                        "home" -> "🏠"
                        "eco" -> "🌱"
                        "flash_on" -> "⚡"
                        "plumbing" -> "🔧"
                        "format_paint" -> "🎨"
                        "cleaning_services" -> "🧹"
                        else -> "📋"
                    },
                    fontSize = 28.sp
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

// Modelo de dados para prestadores com banners
data class ProviderBanner(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val rating: Float,
    val bannerType: BannerType,
    val imageUrl: String?
)

enum class BannerType {
    LARGE, SMALL
}

// Card de Banner Grande (Retrato) - Scroll Horizontal
@Composable
private fun LargeBannerCard(
    provider: ProviderBanner,
    onProviderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(280.dp)
            .height(400.dp)
            .clickable { onProviderClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagem de fundo ou placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TaskGoGreen.copy(alpha = 0.1f))
            )
            
            // Conteúdo do card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                    Text(
                        text = provider.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = provider.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextDark.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⭐ ${String.format("%.1f", provider.rating)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Card de Banner Pequeno (Quadrado) - Grid Vertical
@Composable
private fun SmallBannerCard(
    provider: ProviderBanner,
    onProviderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onProviderClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = provider.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoGreen,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextDark.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "⭐ ${String.format("%.1f", provider.rating)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

