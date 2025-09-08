package com.example.taskgoapp.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.*
import com.example.taskgoapp.core.design.TGIcons
import com.example.taskgoapp.core.design.PrimaryButton
import com.example.taskgoapp.core.design.SecondaryButton
import com.example.taskgoapp.core.design.SearchBar
import com.example.taskgoapp.core.design.TGChip
import com.example.taskgoapp.core.data.models.*
import com.example.taskgoapp.core.data.repositories.MarketplaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import com.example.taskgoapp.core.data.models.ServiceCategory
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToService: (Long) -> Unit,
    onNavigateToProduct: (Long) -> Unit,
    onNavigateToCreateWorkOrder: () -> Unit,
    onNavigateToProposals: () -> Unit,
    onNavigateToBuyBanner: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToCart: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    val productsRepository = remember { 
        object : com.example.taskgoapp.core.data.repositories.MarketplaceRepository {
            override fun getProducts(): Flow<List<com.example.taskgoapp.core.data.models.Product>> = flow {
                delay(500)
                emit(listOf(
                    com.example.taskgoapp.core.data.models.Product(
                        id = 1L,
                        name = "Guarda Roupa 6 Portas",
                        description = "Guarda roupa 6 portas com espelho, acabamento em MDF, cor branca. Perfeito para quartos modernos.",
                        price = 899.90,
                        seller = com.example.taskgoapp.core.data.models.User(
                            id = 1L,
                            name = "João Silva",
                            email = "joao@email.com",
                            phone = "(11) 99999-9999",
                            accountType = com.example.taskgoapp.core.data.models.AccountType.SELLER,
                            rating = 4.8,
                            reviewCount = 156,
                            city = "São Paulo",
                            timeOnTaskGo = "2 anos"
                        ),
                        category = "Móveis"
                    ),
                    com.example.taskgoapp.core.data.models.Product(
                        id = 2L,
                        name = "Furadeira sem fio 18V",
                        description = "Furadeira 18V com 2 baterias, ideal para trabalhos domésticos e profissionais.",
                        price = 299.90,
                        seller = com.example.taskgoapp.core.data.models.User(
                            id = 2L,
                            name = "Maria Santos",
                            email = "maria@email.com",
                            phone = "(11) 88888-8888",
                            accountType = com.example.taskgoapp.core.data.models.AccountType.SELLER,
                            rating = 4.6,
                            reviewCount = 89,
                            city = "Rio de Janeiro",
                            timeOnTaskGo = "1 ano"
                        ),
                        category = "Ferramentas"
                    )
                ))
            }
            
            override fun getProductById(id: Long): Flow<com.example.taskgoapp.core.data.models.Product?> = flow {
                delay(300)
                emit(null)
            }
        }
    }
    val products by productsRepository.getProducts().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories = remember { 
        listOf(
            ServiceCategory(
                id = 1L,
                name = "Montagem de Móveis",
                icon = "ic_assembly",
                description = "Montagem profissional de móveis"
            ),
            ServiceCategory(
                id = 2L,
                name = "Reformas",
                icon = "ic_renovation",
                description = "Reformas e melhorias residenciais"
            ),
            ServiceCategory(
                id = 3L,
                name = "Jardinagem",
                icon = "ic_gardening",
                description = "Serviços de jardinagem e paisagismo"
            ),
            ServiceCategory(
                id = 4L,
                name = "Elétrica",
                icon = "ic_electrical",
                description = "Serviços elétricos residenciais"
            ),
            ServiceCategory(
                id = 5L,
                name = "Limpeza",
                icon = "ic_cleaning",
                description = "Serviços de limpeza doméstica"
            )
        )
    }
    
    val filteredProducts = if (selectedCategory != null) {
        products.filter { it.category == selectedCategory?.name }
    } else {
        products
    }
    
    val searchFilteredProducts = if (searchQuery.isNotEmpty()) {
        filteredProducts.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    } else {
        filteredProducts
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_title),
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Bell),
                            contentDescription = stringResource(R.string.ui_notifications),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Cart),
                            contentDescription = stringResource(R.string.ui_cart),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToMessages,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Messages),
                            contentDescription = stringResource(R.string.messages_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.home_search_placeholder)
                )
            }
            
            // Categories Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.take(5)) { category ->
                        TGChip(
                            text = category.name,
                            selected = selectedCategory?.id == category.id,
                            onClick = { 
                                selectedCategory = if (selectedCategory?.id == category.id) null else category
                            }
                        )
                    }
                }
            }
            
            // Banner/Ads Section
            item {
                BannerCard(
                    title = stringResource(R.string.home_banner_title),
                    description = stringResource(R.string.home_banner_subtitle),
                    onBuyBanner = onNavigateToBuyBanner
                )
            }
            
            // Featured Products
            item {
                Text(
                    text = stringResource(R.string.home_featured_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(searchFilteredProducts.take(4)) { product ->
                ProductCard(
                    product = product,
                    onClick = { onNavigateToProduct(product.id) }
                )
            }
            
            // Quick Actions
            item {
                Text(
                    text = "Ações Rápidas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.services_create_order),
                        onClick = onNavigateToCreateWorkOrder,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = stringResource(R.string.services_proposals),
                        onClick = onNavigateToProposals,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BannerCard(
    title: String,
    description: String,
    onBuyBanner: () -> Unit
) {
    var selectedBannerType by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Button(
                    onClick = onBuyBanner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(stringResource(R.string.home_banner_subtitle))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TGChip(
                    text = stringResource(R.string.home_banner_small),
                    selected = selectedBannerType == "small",
                    onClick = { selectedBannerType = "small" },
                    modifier = Modifier.weight(1f)
                )
                TGChip(
                    text = stringResource(R.string.home_banner_large),
                    selected = selectedBannerType == "large",
                    onClick = { selectedBannerType = "large" },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder image
            Card(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Products),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R$ %.2f".format(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
