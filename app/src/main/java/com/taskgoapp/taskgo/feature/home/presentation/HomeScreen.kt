package com.taskgoapp.taskgo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.data.models.*
import com.taskgoapp.taskgo.core.data.models.ServiceCategory as DataServiceCategory
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.theme.*

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
    onNavigateToCart: () -> Unit = {},
    variant: String? = null
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DataServiceCategory?>(null) }
    val isLoading = variant == "loading" || uiState.isLoading
    val isEmpty = variant == "empty" || products.isEmpty()
    val showSuggestions = variant == "suggestions"
    val showDynamicBanner = variant == "banner"
    
    // Solicitar permissão de notificações quando entrar na home pela primeira vez
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasNotificationPermission = remember {
        com.taskgoapp.taskgo.core.permissions.PermissionHandler.hasNotificationPermission(context)
    }
    val notificationPermissionLauncher = com.taskgoapp.taskgo.core.permissions.rememberNotificationPermissionLauncher(
        onPermissionGranted = {},
        onPermissionDenied = {}
    )
    
    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && notificationPermissionLauncher != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Solicitar permissão após um pequeno delay para não interferir na navegação
            kotlinx.coroutines.delay(1000)
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    val filteredProducts = products
    val searchFilteredProducts = products
    
    // Loading State
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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
                            tint = TaskGoBackgroundWhite,
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
                            tint = TaskGoBackgroundWhite,
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
                            tint = TaskGoBackgroundWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(TGIcons.Package), // Corrigido para um ícone existente
                        contentDescription = null,
                        tint = TaskGoTextGray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Nada encontrado.",
                        style = FigmaProductName,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = "Tente buscar por outro termo.",
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
            }
        } else {
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        categories.take(5).forEach { category ->
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
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack
                    )
                }
                
                searchFilteredProducts.take(4).forEach { product ->
                    item {
                        ProductCard(
                            product = product,
                            onClick = { onNavigateToProduct(product.id.toLongOrNull() ?: 0L) }
                        )
                    }
                }
                
                // Quick Actions
                item {
                    Text(
                        text = "Ações Rápidas",
                        style = FigmaSectionTitle,
                        color = TaskGoTextBlack
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
                // Add suggestions below featured if needed
                // Removido - sugestões virão do backend
                // if (showSuggestions) {
                //     item {
                //         Text(
                //             text = "Para você",
                //             style = FigmaSectionTitle,
                //             color = TaskGoTextBlack
                //         )
                //     }
                //     // TODO: Carregar sugestões do backend
                // }
                // Replace Banner dynamically if asked
                if (showDynamicBanner) {
                    item {
                        BannerCard(
                            title = "Super Banner!",
                            description = "Oferta exclusiva para você.",
                            onBuyBanner = onNavigateToBuyBanner
                        )
                    }
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
            containerColor = TaskGoBackgroundGray
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
                        style = FigmaProductName,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    Text(
                        text = description,
                        style = FigmaProductDescription,
                        color = TaskGoTextGray
                    )
                }
                
                Button(
                    onClick = onBuyBanner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen,
                        contentColor = TaskGoBackgroundWhite
                    )
                ) {
                    Text(
                        text = stringResource(R.string.home_banner_subtitle),
                        style = FigmaButtonText
                    )
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
                    .background(TaskGoSurfaceGray),
                colors = CardDefaults.cardColors(
                    containerColor = TaskGoSurfaceGray
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Products),
                        contentDescription = null,
                        tint = TaskGoTextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = FigmaProductName,
                    color = TaskGoTextBlack
                )
                Text(
                    text = product.description ?: "",
                    style = FigmaProductDescription,
                    color = TaskGoTextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R$ %.2f".format(product.price),
                    style = FigmaPrice,
                    color = TaskGoPriceGreen
                )
            }
        }
    }
}
