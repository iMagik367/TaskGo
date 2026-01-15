package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.feature.feed.presentation.FeedScreen
import com.taskgoapp.taskgo.feature.feed.presentation.FeedViewModel
import com.taskgoapp.taskgo.feature.feed.presentation.StoriesViewModel
import com.taskgoapp.taskgo.feature.feed.presentation.components.PostCard
import com.taskgoapp.taskgo.feature.feed.presentation.components.StoriesSectionNew
import com.taskgoapp.taskgo.feature.feed.presentation.components.InlinePostCreator
import com.taskgoapp.taskgo.feature.feed.presentation.components.CreateStoryScreen
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.util.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMeScreen(
    onBackClick: () -> Unit,
    onNavigateToReviews: (String, String) -> Unit = { _, _ -> },
    viewModel: ProfileViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dashboardMetrics by dashboardViewModel.metrics.collectAsState()
    val context = LocalContext.current
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    // Abas: Cliente apenas Dashboard; Parceiro tem Dashboard e Feed (com Stories e postagens)
    val tabs = remember(uiState.accountType) {
        when (uiState.accountType) {
            AccountType.CLIENTE -> listOf("Dashboard")
            AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> 
                listOf("Dashboard", "Feed") // Parceiro tem Dashboard e Feed (com Stories e postagens)
            else -> listOf("Dashboard", "Feed") // Fallback para legacy
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meus Dados",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow - apenas mostrar se houver mais de uma aba
            if (tabs.size > 1) {
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
            }
            
            // Content baseado na aba selecionada
            // Para clientes, sempre mostrar Dashboard (índice 0)
            when (if (uiState.accountType == AccountType.CLIENTE) 0 else selectedTabIndex) {
                0 -> {
                    // Dashboard - conteúdo original
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
            // Header com foto e nome
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Foto do perfil
                    if (!uiState.avatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uiState.avatarUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto do perfil",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder quando não há foto
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(TaskGoTextGray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.name.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextGray
                            )
                        }
                    }
                    
                    // Nome
                    Text(
                        text = uiState.name.ifBlank { "Usuário" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                }
            }
            
            // Card com informações
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Nome completo
                        InfoRow(
                            label = "Nome completo",
                            value = uiState.name.ifBlank { "Não informado" }
                        )
                        
                        HorizontalDivider()
                        
                        // Tipo da conta
                        InfoRow(
                            label = "Tipo da conta",
                            value = when (uiState.accountType) {
                                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "Parceiro"
                                com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> "Parceiro" // Legacy
                                com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR -> "Parceiro" // Legacy
                                else -> "Cliente"
                            }
                        )
                        
                        HorizontalDivider()
                        
                        // Tempo no TaskGo
                        val timeOnTaskGo = uiState.createdAt?.let { date ->
                            val days = ((System.currentTimeMillis() - date.time) / (1000 * 60 * 60 * 24)).toInt()
                            when {
                                days < 30 -> "$days dias"
                                days < 365 -> "${days / 30} meses"
                                else -> "${days / 365} anos"
                            }
                        } ?: "Recém cadastrado"
                        
                        InfoRow(
                            label = "Tempo no TaskGo",
                            value = timeOnTaskGo
                        )
                        
                        HorizontalDivider()
                        
                        // Avaliação
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avaliação",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TaskGoTextDark,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (index < (uiState.rating?.toInt() ?: 0)) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            TaskGoTextGray.copy(alpha = 0.3f)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Dashboard de Métricas
            item {
                DashboardSection(
                    accountType = uiState.accountType,
                    metrics = dashboardMetrics
                )
            }
            
            // Botão Ver Avaliações
            item {
                Button(
                    onClick = {
                        onNavigateToReviews(uiState.id, uiState.name.ifBlank { "Usuário" })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Ver Avaliações",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
                    }
                }
                1 -> {
                    // Feed - mesclar design do dashboard com feed
                    val feedViewModel: FeedViewModel = hiltViewModel()
                    val storiesViewModel: StoriesViewModel = hiltViewModel()
                    val feedUiState by feedViewModel.uiState.collectAsState()
                    val storiesUiState by storiesViewModel.uiState.collectAsState()
                    
                    // Carregar stories e feed quando a aba Feed for selecionada
                    LaunchedEffect(Unit) {
                        storiesViewModel.loadStories()
                        feedViewModel.loadFeed()
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header do Dashboard (mesmo do tab Dashboard)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Foto do perfil
                                if (!uiState.avatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(uiState.avatarUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Foto do perfil",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .background(TaskGoTextGray.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = uiState.name.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TaskGoTextGray
                                        )
                                    }
                                }
                                
                                // Nome
                                Text(
                                    text = uiState.name.ifBlank { "Usuário" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextDark
                                )
                            }
                        }
                        
                        // Card com informações resumidas
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    InfoRow(
                                        label = "Tipo da conta",
                                        value = when (uiState.accountType) {
                                            AccountType.PARCEIRO -> "Parceiro"
                                            AccountType.PRESTADOR -> "Parceiro" // Legacy
                                            AccountType.VENDEDOR -> "Parceiro" // Legacy
                                            else -> "Cliente"
                                        }
                                    )
                                    
                                    HorizontalDivider()
                                    
                                    // Avaliação resumida
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Avaliação",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = TaskGoTextDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            repeat(5) { index ->
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (index < (uiState.rating?.toInt() ?: 0)) {
                                                        TaskGoGreen
                                                    } else {
                                                        TaskGoTextGray.copy(alpha = 0.3f)
                                                    },
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Conteúdo do Feed (sem topbar)
                        item {
                            com.taskgoapp.taskgo.feature.feed.presentation.components.InlinePostCreator(
                                userAvatarUrl = feedUiState.currentUserAvatarUrl,
                                userName = feedUiState.currentUserName,
                                isLoading = feedUiState.isLoading,
                                onPostCreated = { text, mediaUris ->
                                    feedViewModel.createPost(text, mediaUris)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Stories
                        item {
                            var showCreateStory by remember { mutableStateOf(false) }
                            
                            com.taskgoapp.taskgo.feature.feed.presentation.components.StoriesSectionNew(
                                currentUserAvatarUrl = feedUiState.currentUserAvatarUrl,
                                currentUserName = feedUiState.currentUserName,
                                currentUserId = feedViewModel.currentUserId,
                                stories = storiesUiState.stories,
                                onCreateStoryClick = {
                                    showCreateStory = true
                                },
                                onStoryClick = { userId ->
                                    // Ver story se necessário
                                }
                            )
                            
                            if (showCreateStory) {
                                com.taskgoapp.taskgo.feature.feed.presentation.components.CreateStoryScreen(
                                    onDismiss = { showCreateStory = false },
                                    onStoryCreated = {
                                        showCreateStory = false
                                        // Stories serão recarregados automaticamente pelo observeStories
                                    }
                                )
                            }
                        }
                        
                        // Divisor
                        item {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = TaskGoBackgroundGray
                            )
                        }
                        
                        // Posts
                        if (feedUiState.posts.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Nenhum post encontrado",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TaskGoTextGray
                                    )
                                    Text(
                                        text = "Seja o primeiro a postar na sua região!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TaskGoTextGray
                                    )
                                }
                            }
                        } else {
                            items(
                                items = feedUiState.posts,
                                key = { it.id }
                            ) { post ->
                                PostCard(
                                    post = post,
                                    onUserClick = { userId ->
                                        // Navegar para perfil se necessário
                                    },
                                    onLikeClick = {
                                        feedViewModel.likePost(post.id)
                                    },
                                    onUnlikeClick = {
                                        feedViewModel.unlikePost(post.id)
                                    },
                                    onDeleteClick = if (post.userId == feedViewModel.currentUserId) {
                                        {
                                            feedViewModel.deletePost(post.id)
                                        }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TaskGoTextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TaskGoTextGray
        )
    }
}

@Composable
private fun DashboardSection(
    accountType: AccountType,
    metrics: DashboardMetrics
) {
    if (metrics.isLoading) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TaskGoGreen)
            }
        }
        return
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            
            when (accountType) {
                AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> {
                    // Unificar dashboards: Parceiro vê dashboard combinado de serviços + produtos
                    PartnerDashboard(metrics) // Dashboard unificado para Parceiro
                }
                else -> ClientDashboard(metrics)
            }
        }
    }
}

@Composable
private fun ProviderDashboard(metrics: DashboardMetrics) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Métricas principais em grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Serviços",
                value = metrics.servicesCount.toString(),
                icon = "🔧",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Ordens",
                value = metrics.ordersReceived.toString(),
                icon = "📋",
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Concluídas",
                value = metrics.completedOrders.toString(),
                icon = "✅",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Propostas",
                value = metrics.proposalsSent.toString(),
                icon = "💼",
                modifier = Modifier.weight(1f)
            )
        }
        
        HorizontalDivider()
        
        // Receita
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Receita",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.totalRevenue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Este mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.monthlyRevenue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                }
            }
            
            // Gráfico simples de receita
            RevenueChart(
                totalRevenue = metrics.totalRevenue,
                monthlyRevenue = metrics.monthlyRevenue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
private fun SellerDashboard(metrics: DashboardMetrics) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Produtos",
                value = metrics.productsCount.toString(),
                icon = "📦",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Vendidos",
                value = metrics.productsSold.toString(),
                icon = "💰",
                modifier = Modifier.weight(1f)
            )
        }
        
        HorizontalDivider()
        
        // Vendas
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Vendas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.totalSales),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Este mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.monthlySales),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                }
            }
            
            // Gráfico simples de vendas
            RevenueChart(
                totalRevenue = metrics.totalSales,
                monthlyRevenue = metrics.monthlySales,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
private fun PartnerDashboard(metrics: DashboardMetrics) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Métricas principais em grid - Serviços e Produtos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Serviços",
                value = metrics.servicesCount.toString(),
                icon = "🔧",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Produtos",
                value = metrics.productsCount.toString(),
                icon = "📦",
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Ordens",
                value = metrics.ordersReceived.toString(),
                icon = "📋",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Vendidos",
                value = metrics.productsSold.toString(),
                icon = "💰",
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Concluídas",
                value = metrics.completedOrders.toString(),
                icon = "✅",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Propostas",
                value = metrics.proposalsSent.toString(),
                icon = "💼",
                modifier = Modifier.weight(1f)
            )
        }
        
        HorizontalDivider()
        
        // Receita Total (Serviços + Vendas)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Receita Total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total (Serviços + Vendas)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.totalRevenue + metrics.totalSales),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Este mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.monthlyRevenue + metrics.monthlySales),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                }
            }
            
            // Gráfico simples de receita total
            RevenueChart(
                totalRevenue = metrics.totalRevenue + metrics.totalSales,
                monthlyRevenue = metrics.monthlyRevenue + metrics.monthlySales,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
private fun ClientDashboard(metrics: DashboardMetrics) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Ordens",
                value = metrics.serviceOrdersCreated.toString(),
                icon = "📋",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Compras",
                value = metrics.productsPurchased.toString(),
                icon = "🛒",
                modifier = Modifier.weight(1f)
            )
        }
        
        HorizontalDivider()
        
        // Gastos
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Gastos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.totalSpent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Este mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                    Text(
                        text = currencyFormat.format(metrics.monthlySpent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextDark
                    )
                }
            }
            
            // Gráfico simples de gastos
            RevenueChart(
                totalRevenue = metrics.totalSpent,
                monthlyRevenue = metrics.monthlySpent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoGreen.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray
            )
        }
    }
}

@Composable
private fun RevenueChart(
    totalRevenue: Double,
    monthlyRevenue: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TaskGoSurfaceGray, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 8.dp.toPx()
            
            // Gráfico de barras simples
            val maxValue = maxOf(totalRevenue, monthlyRevenue).coerceAtLeast(1.0)
            val totalHeight = ((totalRevenue / maxValue) * (height - padding * 2)).toFloat()
            val monthlyHeight = ((monthlyRevenue / maxValue) * (height - padding * 2)).toFloat()
            
            // Barra total
            drawRect(
                color = TaskGoGreen.copy(alpha = 0.6f),
                topLeft = Offset(padding, height - padding - totalHeight),
                size = Size((width - padding * 3) / 2, totalHeight)
            )
            
            // Barra mensal
            drawRect(
                color = TaskGoGreen,
                topLeft = Offset(padding * 2 + (width - padding * 3) / 2, height - padding - monthlyHeight),
                size = Size((width - padding * 3) / 2, monthlyHeight)
            )
        }
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray
            )
            Text(
                text = "Mensal",
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray
            )
        }
    }
}


