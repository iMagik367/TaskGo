package com.taskgoapp.taskgo.feature.home.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.data.models.*
import com.taskgoapp.taskgo.core.data.models.ServiceCategory as DataServiceCategory
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.HomeBanner
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.location.calculateDistance
import com.taskgoapp.taskgo.core.maps.ProvidersMapView
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import androidx.core.content.ContextCompat
import android.location.Location as AndroidLocation
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

enum class FilterType {
    SERVICES, PRODUCTS
}

data class HomeBannerContent(
    val id: String,
    @DrawableRes val imageRes: Int? = null,
    val imageUrl: String? = null,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToService: (String) -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToCreateWorkOrder: () -> Unit,
    onNavigateToProposals: () -> Unit,
    onNavigateToBuyBanner: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLocalProviders: () -> Unit = {},
    onNavigateToDiscountedProducts: () -> Unit = {},
    onNavigateToProviderProfile: (String, Boolean) -> Unit = { _, _ -> },
    onNavigateToLocalServiceOrders: () -> Unit = {},
    variant: String? = null
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val products by viewModel.products.collectAsState()
    val services by viewModel.services.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val productCategories by viewModel.productCategories.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val accountType by viewModel.accountType.collectAsState()
    val remoteBanners by viewModel.homeBanners.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DataServiceCategory?>(null) }
    var selectedProductFilter by remember { mutableStateOf<String?>(null) } // Filtro de produtos: promoções, valores, etc
    val isLoading = variant == "loading" || uiState.isLoading
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
    
    var selectedFilterType by remember { mutableStateOf<FilterType?>(FilterType.SERVICES) }
    
    // Obter localização do usuário para filtrar produtos em destaque
    val locationManager = remember { LocationManager(context) }
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }
    
    // Carregar localização do usuário
    LaunchedEffect(Unit) {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                userLocation = locationManager.getCurrentLocation()
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Erro ao obter localização: ${e.message}", e)
        }
    }
    
    // Filtrar produtos baseado nos filtros selecionados
    val filteredProducts = remember(products, searchQuery, selectedProductFilter, userLocation) {
        products
            .filter { product ->
                // Aplicar filtro de busca
                if (searchQuery.isNotBlank()) {
                    val matchesSearch = product.title.contains(searchQuery, ignoreCase = true) ||
                        product.description?.contains(searchQuery, ignoreCase = true) == true
                    if (!matchesSearch) return@filter false
                }
                
                // Aplicar filtros específicos de produtos
                when (selectedProductFilter) {
                    "Em Promoção" -> {
                        // Produtos com desconto (assumindo que há um campo discountPercentage ou similar)
                        // Por enquanto, vamos filtrar produtos em destaque como "promoção"
                        product.featured == true
                    }
                    "Até R$ 50" -> {
                        product.price <= 50.0
                    }
                    "R$ 50 - R$ 100" -> {
                        product.price >= 50.0 && product.price <= 100.0
                    }
                    "R$ 100 - R$ 250" -> {
                        product.price >= 100.0 && product.price <= 250.0
                    }
                    "Acima de R$ 250" -> {
                        product.price > 250.0
                    }
                    "Mais Vendidos" -> {
                        // Por enquanto, produtos em destaque (quando houver campo de vendas, usar isso)
                        product.featured == true
                    }
                    "Novos" -> {
                        // Por enquanto, todos os produtos (quando houver campo de data, usar isso)
                        true
                    }
                    "Melhor Avaliados" -> {
                        // Produtos com rating >= 4.0
                        (product.rating ?: 0.0) >= 4.0
                    }
                    "Todos", null -> {
                        // Mostrar produtos em destaque por padrão
                        product.featured == true
                    }
                    else -> true
                }
            }
            .sortedByDescending { product ->
                // Ordenar por relevância baseado no filtro
                when (selectedProductFilter) {
                    "Melhor Avaliados" -> product.rating ?: 0.0
                    "Mais Vendidos" -> if (product.featured == true) 1.0 else 0.0
                    else -> product.rating ?: 0.0
                }
            }
            .let { filtered ->
                // Filtrar por localização se disponível (raio de 100km)
                val productLat = userLocation?.latitude
                val productLng = userLocation?.longitude
                
                if (productLat != null && productLng != null) {
                    filtered.filter { product ->
                        val prodLat = product.latitude
                        val prodLng = product.longitude
                        if (prodLat != null && prodLng != null) {
                            val distance = calculateDistance(
                                productLat,
                                productLng,
                                prodLat,
                                prodLng
                            )
                            distance <= 100.0
                        } else {
                            true // Se não tem localização, mostrar mesmo assim
                        }
                    }
                } else {
                    filtered
                }
            }
    }

    val resolveBannerAction: (String?) -> () -> Unit = { route ->
        when (route?.uppercase()) {
            "LOCAL_PROVIDERS" -> onNavigateToLocalProviders
            "LOCAL_ORDERS", "SERVICE_ORDERS" -> onNavigateToLocalServiceOrders
            "DISCOUNTS", "PROMOTIONS" -> onNavigateToDiscountedProducts
            "CREATE_ORDER" -> onNavigateToCreateWorkOrder
            "PROPOSALS" -> onNavigateToProposals
            "BUY_BANNER" -> onNavigateToBuyBanner
            "MESSAGES" -> onNavigateToMessages
            "CART" -> onNavigateToCart
            else -> onNavigateToDiscountedProducts
        }
    }
    
    val fallbackBanners = remember(accountType) {
        when (accountType) {
            com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> listOf(
                HomeBannerContent(
                    id = "ordens_servico_destaque",
                    imageRes = R.drawable.banner_ordens_servico,
                    title = "",
                    subtitle = "",
                    actionLabel = "",
                    onClick = onNavigateToLocalServiceOrders
                ),
                HomeBannerContent(
                    id = "produtos_descontos",
                    imageRes = R.drawable.banner_produtos_descontos,
                    title = "",
                    subtitle = "",
                    actionLabel = "",
                    onClick = onNavigateToDiscountedProducts
                )
            )
            com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR,
            com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> listOf(
                HomeBannerContent(
                    id = "prestadores_locais",
                    imageRes = R.drawable.banner_prestadores_locais,
                    title = "",
                    subtitle = "",
                    actionLabel = "",
                    onClick = onNavigateToLocalProviders
                ),
                HomeBannerContent(
                    id = "produtos_descontos",
                    imageRes = R.drawable.banner_produtos_descontos,
                    title = "",
                    subtitle = "",
                    actionLabel = "",
                    onClick = onNavigateToDiscountedProducts
                )
            )
        }
    }
    
    val remoteHeroBanners = remember(remoteBanners, accountType) {
        val matchesAccount: (com.taskgoapp.taskgo.core.model.HomeBanner) -> Boolean = { banner ->
            when (banner.audience) {
                com.taskgoapp.taskgo.core.model.HomeBanner.Audience.TODOS -> true
                com.taskgoapp.taskgo.core.model.HomeBanner.Audience.PRESTADOR ->
                    accountType == com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR
                com.taskgoapp.taskgo.core.model.HomeBanner.Audience.CLIENTE ->
                    accountType != com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR
            }
        }
        remoteBanners
            .filter { matchesAccount(it) && !it.imageUrl.isNullOrBlank() }
            .sortedByDescending { it.priority }
            .map { banner ->
                HomeBannerContent(
                    id = banner.id,
                    imageUrl = banner.imageUrl,
                    title = "",
                    subtitle = "",
                    actionLabel = "",
                    onClick = resolveBannerAction(banner.actionRoute)
                )
            }
    }
    
    val heroBanners = remember(remoteHeroBanners, fallbackBanners) {
        if (remoteHeroBanners.isNotEmpty()) remoteHeroBanners else fallbackBanners
    }
    
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
                        onClick = onNavigateToSearch,
                        modifier = Modifier.size(48.dp)
                    ) {
                        TGIcon(
                            iconRes = TGIcons.Search,
                            contentDescription = "Busca Universal",
                            size = TGIcons.Sizes.Large,
                            tint = TaskGoBackgroundWhite
                        )
                    }
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(48.dp)
                    ) {
                        TGIcon(
                            iconRes = TGIcons.Bell,
                            contentDescription = stringResource(R.string.ui_notifications),
                            size = TGIcons.Sizes.Large,
                            tint = TaskGoBackgroundWhite
                        )
                    }
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.size(48.dp)
                    ) {
                        TGIcon(
                            iconRes = TGIcons.Cart,
                            contentDescription = stringResource(R.string.ui_cart),
                            size = TGIcons.Sizes.Large,
                            tint = TaskGoBackgroundWhite
                        )
                    }
                    IconButton(
                        onClick = onNavigateToMessages,
                        modifier = Modifier.size(48.dp)
                    ) {
                        TGIcon(
                            iconRes = TGIcons.Messages,
                            contentDescription = stringResource(R.string.messages_title),
                            size = TGIcons.Sizes.Large,
                            tint = TaskGoBackgroundWhite
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
            // Filter Type Selector (Serviços / Produtos)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTypeChip(
                        text = "Serviços",
                        selected = selectedFilterType == FilterType.SERVICES,
                        onClick = { 
                            selectedFilterType = if (selectedFilterType == FilterType.SERVICES) null else FilterType.SERVICES
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTypeChip(
                        text = "Produtos",
                        selected = selectedFilterType == FilterType.PRODUCTS,
                        onClick = { 
                            selectedFilterType = if (selectedFilterType == FilterType.PRODUCTS) null else FilterType.PRODUCTS
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Filtros - Diferentes para Produtos e Serviços
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedFilterType == FilterType.PRODUCTS) "Filtros de Produtos" else "Filtros de Serviços",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        if (selectedFilterType == FilterType.PRODUCTS) {
                            // Filtros específicos para produtos: valores, promoções, etc
                            val productFilters = listOf(
                                "Todos",
                                "Em Promoção",
                                "Até R$ 50",
                                "R$ 50 - R$ 100",
                                "R$ 100 - R$ 250",
                                "Acima de R$ 250",
                                "Mais Vendidos",
                                "Novos",
                                "Melhor Avaliados"
                            )
                            productFilters.forEach { filterName ->
                                TGChip(
                                    text = filterName,
                                    selected = selectedProductFilter == filterName,
                                    onClick = { 
                                        selectedProductFilter = if (selectedProductFilter == filterName) null else filterName
                                    }
                                )
                            }
                        } else {
                            // Filtros de categorias de serviços
                            if (categories.isNotEmpty()) {
                                categories.take(8).forEach { category ->
                                    TGChip(
                                        text = category.name,
                                        selected = selectedCategory?.id == category.id,
                                        onClick = { 
                                            selectedCategory = if (selectedCategory?.id == category.id) null else category
                                        }
                                    )
                                }
                            } else {
                                // Placeholder chips quando não há categorias
                                listOf("Todos", "Mais Procurados", "Novos", "Em Destaque").forEach { categoryName ->
                                    TGChip(
                                        text = categoryName,
                                        selected = selectedCategory?.name == categoryName,
                                        onClick = { 
                                            selectedCategory = if (selectedCategory?.name == categoryName) null else 
                                                DataServiceCategory(id = 0, name = categoryName, icon = "", description = "")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Mapa com Prestadores e Lojas em Tempo Real
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Lojas Próximas",
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    ProvidersMapView(
                        userLocation = userLocation,
                        stores = stores,
                        onStoreClick = { storeId ->
                            // Navegar para perfil da loja
                            onNavigateToProviderProfile(storeId, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }
            }
            
            item {
                HomeBannerCarousel(
                    banners = heroBanners,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Banner/Ads Section - DESATIVADO (será lançado futuramente)
            // item {
            //     BannerCard(
            //         title = "Divulgue seu negócio!",
            //         description = "Compre um banner e alcance mais clientes",
            //         onBuyBanner = onNavigateToBuyBanner
            //     )
            // }
            
            // Featured Section - Produtos ou Serviços baseado no filtro selecionado
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedFilterType == FilterType.SERVICES) "Serviços em Destaque" else stringResource(R.string.home_featured_title),
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Products List - Scroll Horizontal (APENAS quando produtos selecionado)
                    if (selectedFilterType == FilterType.PRODUCTS) {
                        if (filteredProducts.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredProducts.take(6).size) { index ->
                                    val product = filteredProducts[index]
                                    ProductCard(
                                        product = product,
                                        onClick = { onNavigateToProduct(product.id) },
                                        modifier = Modifier.width(180.dp)
                                    )
                                }
                            }
                        } else {
                            // Empty state para produtos em destaque
                            EmptyProductsState(onSearchClick = onNavigateToSearch)
                        }
                    } else if (selectedFilterType == FilterType.SERVICES) {
                        // Services List - Scroll Horizontal
                        if (services.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(services.take(6).size) { index ->
                                    val service = services[index]
                                    ServiceCard(
                                        service = service,
                                        onClick = { onNavigateToService(service.id) },
                                        modifier = Modifier.width(180.dp)
                                    )
                                }
                            }
                        } else {
                            // Empty state para serviços em destaque
                            EmptyServicesState(onSearchClick = onNavigateToSearch)
                        }
                    }
                }
            }
            
            // Dynamic Banner (se solicitado)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBannerCarousel(
    banners: List<HomeBannerContent>,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })
    
    if (banners.size > 1) {
        LaunchedEffect(banners.size) {
            while (true) {
                delay(5500)
                val next = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }
    
    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) { page ->
            HomeBannerCard(
                banner = banners[page],
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                banners.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 26.dp else 10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isSelected) TaskGoGreen else TaskGoSurfaceGray
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBannerCard(
    banner: HomeBannerContent,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { banner.onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            when {
                banner.imageUrl != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(banner.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                banner.imageRes != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(banner.imageRes)
                            .crossfade(true)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TaskGoGreen.copy(alpha = 0.2f))
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBuyBanner() },
        colors = CardDefaults.cardColors(
            containerColor = TaskGoGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TGIcon(
                        iconRes = TGIcons.Bell,
                        contentDescription = null,
                        size = TGIcons.Sizes.Medium,
                        tint = Color.White
                    )
                    Text(
                        text = title,
                        style = FigmaProductName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = FigmaProductDescription,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Button(
                onClick = onBuyBanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = TaskGoGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Comprar",
                    style = FigmaButtonText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FilterTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = text,
                style = FigmaButtonText,
                color = if (selected) TaskGoBackgroundWhite else TaskGoTextBlack
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TaskGoGreen,
            selectedLabelColor = TaskGoBackgroundWhite,
            containerColor = TaskGoSurfaceGray,
            labelColor = TaskGoTextBlack
        )
    )
}

@Composable
fun EmptyProductsState(
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoSurfaceGray
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TGIcon(
                iconRes = TGIcons.Products,
                contentDescription = null,
                size = 64.dp,
                tint = TaskGoTextGray
            )
            Text(
                text = "Nenhum produto em destaque",
                style = FigmaProductName,
                color = TaskGoTextGray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Use a busca para encontrar produtos ou serviços",
                style = FigmaProductDescription,
                color = TaskGoTextGray
            )
            TextButton(onClick = onSearchClick) {
                Text("Ir para busca", color = TaskGoGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyServicesState(
    onSearchClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoSurfaceGray
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TGIcon(
                iconRes = TGIcons.Services,
                contentDescription = null,
                size = 64.dp,
                tint = TaskGoTextGray
            )
            Text(
                text = "Nenhum serviço cadastrado",
                style = FigmaProductName,
                color = TaskGoTextGray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Use a busca para encontrar produtos ou serviços",
                style = FigmaProductDescription,
                color = TaskGoTextGray
            )
            TextButton(onClick = onSearchClick) {
                Text("Explorar serviços", color = TaskGoGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val imageUri = remember(product.imageUris) {
                product.imageUris.firstOrNull { it.isNotBlank() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TaskGoSurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
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
                        size = TGIcons.Sizes.ExtraLarge,
                        tint = TaskGoTextGray
                    )
                }

                if (product.featured == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        color = TaskGoOrange,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Destaque",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = product.title,
                    style = FigmaProductName,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                product.sellerName?.takeIf { it.isNotBlank() }?.let { seller ->
                    Text(
                        text = seller,
                        style = FigmaProductDescription,
                        color = TaskGoTextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                product.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                        modifier = Modifier.size(18.dp)
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

@Composable
private fun ServiceCard(
    service: ServiceFirestore,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Imagem do serviço
            if (service.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(service.images.first())
                        .size(200)
                        .build(),
                    contentDescription = service.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(TaskGoBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔧",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            // Título
            Text(
                text = service.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextBlack,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            // Descrição
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            // Preço e Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(service.price),
                    style = MaterialTheme.typography.titleSmall,
                    color = TaskGoGreen,
                    fontWeight = FontWeight.Bold
                )
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
                        text = "%.1f".format(service.rating ?: 0.0),
                        style = FigmaRatingText,
                        color = TaskGoTextDark
                    )
                }
            }
        }
    }
}
