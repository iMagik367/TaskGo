package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.design.SecondaryButton
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.feature.feed.presentation.UserFeedScreen
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    onMessageClick: (String) -> Unit = {},
    onRateClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onServiceClick: (String) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: PublicUserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val services by viewModel.services.collectAsState()
    val products by viewModel.products.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isOwnProfile = userId == currentUserId
    
    var selectedTabIndex by remember(userId) { mutableStateOf(0) }
    
    // Determinar tabs baseado no tipo de conta
    // Parceiro mostra tanto Serviços quanto Produtos
    val tabs = remember(uiState.accountType) {
        when (uiState.accountType) {
            AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> 
                listOf("Feed", "Serviços", "Produtos", "Sobre") // Unificado para Parceiro
            else -> listOf("Feed", "Sobre")
        }
    }
    
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.userProfile?.displayName ?: "Perfil",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            // Barra de ações (apenas se não for o próprio perfil)
            if (!isOwnProfile) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryButton(
                            text = "Avaliar",
                            onClick = { onRateClick(userId) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Postar",
                            onClick = { onPostClick(userId) },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = "Mensagem",
                            onClick = { onMessageClick(userId) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TaskGoGreen)
                }
            }
            
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadProfile(userId) },
                        colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                    ) {
                        Text("Tentar novamente")
                    }
                }
            }
            
            uiState.userProfile != null -> {
                val userProfile = uiState.userProfile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Header do perfil (avatar, nome, tipo, avaliações)
                    ProfileHeader(
                        userProfile = userProfile,
                        accountType = uiState.accountType,
                        reviews = reviews,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // TabRow
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
                    
                    // Conteúdo baseado na aba selecionada
                    when (selectedTabIndex) {
                        0 -> {
                            // Feed - posts do usuário
                            UserFeedScreen(
                                userId = userId,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        1 -> {
                            // Aba Serviços - para Parceiro e Prestador
                            when (uiState.accountType) {
                                AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> {
                                    ServicesTabContent(
                                        services = services,
                                        onServiceClick = onServiceClick,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    // Cliente não tem serviços
                                    EmptyTabContent(
                                        message = "Este usuário não oferece serviços",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        2 -> {
                            // Aba Produtos - para Parceiro e Vendedor
                            when (uiState.accountType) {
                                AccountType.PARCEIRO, AccountType.PRESTADOR, AccountType.VENDEDOR -> {
                                    ProductsTabContent(
                                        products = products,
                                        onProductClick = onProductClick,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {
                                    // Cliente não tem produtos
                                    EmptyTabContent(
                                        message = "Este usuário não vende produtos",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        3 -> {
                            // Aba Sobre (ajustado índice para 4 tabs: Feed, Serviços, Produtos, Sobre)
                            AboutTabContent(
                                userProfile = userProfile,
                                reviews = reviews,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    userProfile: com.taskgoapp.taskgo.data.firestore.models.UserFirestore,
    accountType: AccountType?,
    reviews: List<com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore>,
    modifier: Modifier = Modifier
) {
    val averageRating = remember(reviews) {
        if (reviews.isEmpty()) 0.0
        else reviews.map { it.rating.toDouble() }.average()
    }
    
    Card(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userProfile.photoURL)
                    .size(120)
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            // Nome
            Text(
                text = userProfile.displayName ?: "Usuário",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextBlack
            )
            
            // Tipo de conta
            Text(
                text = when (accountType) {
                    AccountType.PARCEIRO -> "Parceiro"
                    AccountType.PRESTADOR -> "Parceiro" // Legacy
                    AccountType.VENDEDOR -> "Parceiro" // Legacy
                    else -> "Cliente"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TaskGoTextGray
            )
            
            // Avaliação (se houver)
            if (reviews.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = TaskGoStarYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", averageRating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    Text(
                        text = "(${reviews.size} avaliações)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ServicesTabContent(
    services: List<com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore>,
    onServiceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (services.isEmpty()) {
        EmptyTabContent(
            message = "Nenhum serviço oferecido ainda",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(services, key = { it.id }) { service ->
                ServiceCard(
                    service = service,
                    onClick = { onServiceClick(service.id) }
                )
            }
        }
    }
}

@Composable
private fun ProductsTabContent(
    products: List<com.taskgoapp.taskgo.data.firestore.models.ProductFirestore>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        EmptyTabContent(
            message = "Nenhum produto à venda ainda",
            modifier = modifier
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product.id) }
                )
            }
        }
    }
}

@Composable
private fun AboutTabContent(
    userProfile: com.taskgoapp.taskgo.data.firestore.models.UserFirestore,
    reviews: List<com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Informações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    
                    userProfile.address?.city?.let { city ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TaskGoTextGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextBlack
                            )
                        }
                    }
                    
                    userProfile.phone?.let { phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = TaskGoTextGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TaskGoTextBlack
                            )
                        }
                    }
                }
            }
        }
        
        if (reviews.isNotEmpty()) {
            item {
                Text(
                    text = "Avaliações (${reviews.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack
                )
            }
            
            items(reviews, key = { it.id }) { review ->
                ReviewCard(review = review)
            }
        }
    }
}

@Composable
private fun EmptyTabContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TaskGoTextGray
        )
    }
}

@Composable
private fun ServiceCard(
    service: com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagem do serviço (se houver)
            if (service.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(service.images.first())
                        .size(80)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = service.title ?: "Serviço",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack
                )
                Text(
                    text = service.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: com.taskgoapp.taskgo.data.firestore.models.ProductFirestore,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagem do produto
            if (product.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrls.first())
                        .size(200)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Informações
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.title.ifBlank { "Produto" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack,
                    maxLines = 2
                )
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(product.price),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoGreen
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.reviewerName.ifBlank { "Anônimo" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = TaskGoStarYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", review.rating.toFloat()),
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextBlack
                    )
                }
            }
            
            review.comment?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextBlack
                )
            }
        }
    }
}





