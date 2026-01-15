package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest as CoilImageRequest
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.FilterBar
import com.taskgoapp.taskgo.core.design.FilterBottomSheet
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.core.design.TGIcon
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onNavigateToServiceDetail: (String) -> Unit,
    onNavigateToCreateWorkOrder: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: ServicesViewModel = hiltViewModel()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val uiState by viewModel.uiState.collectAsState()
    val serviceOrders by viewModel.serviceOrders.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val categories by viewModel.serviceCategories.collectAsState()
    val categoriesFull by viewModel.serviceCategoriesFull.collectAsState()
    val accountType by viewModel.accountType.collectAsState()
    val providerServices by viewModel.availableServices.collectAsState()
    val serviceOrdersFirestore by viewModel.serviceOrdersFirestore.collectAsState()
    val filteredProviders by viewModel.filteredProviders.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val isProvider = accountType == AccountType.PARCEIRO || accountType == AccountType.PRESTADOR // Suporta legacy
    val subtitleText = if (isProvider) {
        "Acompanhe as ordens de serviço solicitadas pelos clientes"
    } else {
        "Encontre prestadores de serviços disponíveis na sua região"
    }
    
    // Atualizar busca quando o usuário digitar
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }
    
    // Adicionar "Todos" no início das categorias
    val categoriesWithAll = remember(categories) {
        listOf("Todos") + categories.filter { it != "Todos" }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.services_title),
                subtitle = subtitleText,
                onBackClick = null,
                backgroundColor = TaskGoGreen,
                titleColor = Color.White,
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
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Botão "Criar Ordem de Serviço" (apenas para cliente e vendedor)
            if (accountType == AccountType.CLIENTE || accountType == AccountType.VENDEDOR) { // VENDEDOR legacy
                Button(
                    onClick = onNavigateToCreateWorkOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Criar Ordem de Serviço",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Barra de Busca
            com.taskgoapp.taskgo.core.design.SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Buscar serviços...",
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Barra de Filtros e botões de ordenação (apenas para CLIENTE, não para PARCEIRO)
            if (!isProvider) {
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
            }
            
            // Lista de categorias (Cliente/Vendedor) ou ordens por categoria (Prestador)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Erro ao carregar serviços",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!isProvider && selectedCategory == null) {
                // Mostrar categorias em grid para Cliente/Vendedor (sempre mostrar, mesmo se vazio)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (categoriesFull.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Carregando categorias...",
                                    color = TaskGoTextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(categoriesFull) { category ->
                            ServiceCategoryCard(
                                category = category,
                                onCategoryClick = { 
                                    selectedCategory = category.name
                                    viewModel.updateSelectedCategory(category.name)
                                }
                            )
                        }
                    }
                }
            } else if (!isProvider && selectedCategory != null) {
                // Mostrar prestadores que têm a categoria selecionada em preferredCategories
                if (filteredProviders.isEmpty()) {
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
                                color = TaskGoTextGray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Não há prestadores cadastrados nesta categoria",
                                color = TaskGoTextGray,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
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
                        contentPadding = PaddingValues(bottom = 32.dp),
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
                        items(filteredProviders) { provider ->
                            ProviderCard(
                                provider = provider,
                                onProviderClick = { onNavigateToServiceDetail(provider.uid) }
                            )
                        }
                    }
                }
            } else if (isProvider) {
                // Para PARCEIRO: mostrar diretamente as ordens de serviço (filtradas por preferredCategories, raio 100km)
                // As ordens já são filtradas automaticamente por preferredCategories no ViewModel
                // O Firestore listener já faz atualização automática em tempo real
                if (serviceOrdersFirestore.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhuma ordem de serviço disponível",
                                color = TaskGoTextGray,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "As novas ordens serão exibidas automaticamente quando disponíveis",
                                color = TaskGoTextGray,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(serviceOrdersFirestore) { order ->
                            val isOwnOrder = currentUserId == order.clientId
                            ServiceOrderCardFirestore(
                                order = order,
                                isOwnOrder = isOwnOrder,
                                onServiceClick = { onNavigateToServiceDetail(order.id) }
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
            },
            showPriceFilter = isProvider // Filtro de preço apenas para Prestadores
        )
    }
}

@Composable
private fun ServiceOrderCardFirestore(
    order: OrderFirestore,
    isOwnOrder: Boolean,
    onServiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val cardColor = if (isOwnOrder) {
        TaskGoGreen.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onServiceClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagem placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TaskGoGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📋", fontSize = 24.sp)
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Título
                Text(
                    text = order.details.takeIf { it.isNotBlank() } ?: order.category ?: "Ordem de Serviço",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Localização
                Text(
                    text = order.location.takeIf { it.isNotBlank() } ?: "Localização não informada",
                    fontSize = 12.sp,
                    color = TaskGoTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Valor
                Text(
                    text = if (order.budget > 0) {
                        currencyFormat.format(order.budget)
                    } else {
                        "Orçamento a combinar"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoGreen
                )
                
                // Badge se for própria ordem
                if (isOwnOrder) {
                    Surface(
                        color = TaskGoGreen.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Sua ordem",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
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
private fun ServiceOrderCard(
    title: String,
    description: String,
    price: String,
    rating: Float,
    onServiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onServiceClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaskGoTextDark
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = TaskGoTextDark.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoGreen
                )
                
                Text(
                    text = "⭐ $rating",
                    fontSize = 14.sp,
                    color = TaskGoTextDark.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProviderServiceCard(
    service: ServiceFirestore,
    onServiceClick: () -> Unit,
    onProviderClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onServiceClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (service.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(service.images.first())
                        .crossfade(true)
                        .build(),
                    contentDescription = service.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(TaskGoSurfaceGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = TGIcons.Services),
                        contentDescription = null,
                        tint = TaskGoTextGray
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = service.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaskGoTextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = service.description,
                    fontSize = 14.sp,
                    color = TaskGoTextDark.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Informações do prestador (clicável)
                if (service.providerId.isNotBlank() && onProviderClick != null) {
                    com.taskgoapp.taskgo.core.design.UserAvatarNameLoader(
                        userId = service.providerId,
                        onUserClick = { onProviderClick(service.providerId) },
                        avatarSize = 32.dp,
                        showName = true,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(service.price),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
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
}

@Composable
fun ServiceCategoryCard(
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

@Composable
private fun ProviderCard(
    provider: UserFirestore,
    onProviderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProviderClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto do prestador
            if (provider.photoURL != null && provider.photoURL.isNotBlank()) {
                AsyncImage(
                    model = CoilImageRequest.Builder(LocalContext.current)
                        .data(provider.photoURL)
                        .crossfade(true)
                        .build(),
                    contentDescription = provider.displayName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TaskGoGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 32.sp)
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = provider.displayName ?: "Parceiro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaskGoTextDark
                )
                
                // Categorias que o prestador oferece
                provider.preferredCategories?.takeIf { it.isNotEmpty() }?.let { categories ->
                    Text(
                        text = categories.joinToString(", "),
                        fontSize = 14.sp,
                        color = TaskGoTextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Rating
                if (provider.rating != null && provider.rating > 0) {
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
                            text = "%.1f".format(provider.rating),
                            fontSize = 14.sp,
                            color = TaskGoTextDark
                        )
                    }
                }
            }
        }
    }
}