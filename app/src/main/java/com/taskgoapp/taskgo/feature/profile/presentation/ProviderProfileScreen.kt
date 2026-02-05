package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.design.SecondaryButton
import com.taskgoapp.taskgo.core.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileScreen(
    providerId: String,
    isStore: Boolean = false,
    onBackClick: () -> Unit,
    onRateClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onServiceClick: (String) -> Unit,
    viewModel: ProviderProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val services by viewModel.services.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    
    LaunchedEffect(providerId) {
        viewModel.loadProfile(providerId, isStore)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isStore) "Perfil da Loja" else "Perfil do Prestador",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
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
                        onClick = { onRateClick(providerId) },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Enviar Mensagem",
                        onClick = { onMessageClick(providerId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header com foto e informações básicas
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Foto do perfil
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uiState.profile?.photoURL)
                            .size(120)
                            .build(),
                        contentDescription = "Foto do perfil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(TaskGoBorder),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Nome
                    Text(
                        text = uiState.profile?.displayName ?: "Carregando...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TaskGoTextBlack
                    )
                    
                    // Tipo (Prestador ou Loja)
                    Text(
                        text = if (isStore) "Loja" else "Prestador de Serviços",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray
                    )
                    
                    // Avaliação e número de avaliações
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Avaliação",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${uiState.profile?.rating ?: 0.0}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${reviews.size} avaliações)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TaskGoTextGray
                        )
                    }
                }
            }
            
            // Informações de contato
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informações de Contato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        uiState.profile?.email?.let { email ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.size(20.dp),
                                    tint = TaskGoGreen
                                )
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        uiState.profile?.phone?.let { phone ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Telefone",
                                    modifier = Modifier.size(20.dp),
                                    tint = TaskGoGreen
                                )
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        uiState.profile?.address?.let { address ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Endereço",
                                    modifier = Modifier.size(20.dp),
                                    tint = TaskGoGreen
                                )
                                Column {
                                    Text(
                                        text = "${address.street}, ${address.number}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${address.neighborhood}, ${address.city} - ${address.state}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TaskGoTextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Serviços oferecidos (para prestadores) ou Produtos (para lojas)
            if (services.isNotEmpty()) {
                item {
                    Text(
                        text = if (isStore) "Produtos" else "Serviços Oferecidos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(services) { service ->
                    ServiceCard(
                        service = service,
                        isStore = isStore,
                        onClick = { onServiceClick(service.id) }
                    )
                }
            }
            
            // Avaliações
            if (reviews.isNotEmpty()) {
                item {
                    Text(
                        text = "Avaliações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(reviews) { review ->
                    ReviewCard(review = review)
                }
            }
            
            // Estatísticas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Estatísticas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(
                                label = if (isStore) "Produtos" else "Serviços",
                                value = services.size.toString()
                            )
                            StatItem(
                                label = "Avaliações",
                                value = reviews.size.toString()
                            )
                            StatItem(
                                label = "Avaliação Média",
                                value = "${uiState.profile?.rating ?: 0.0}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore,
    isStore: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (service.images.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(service.images.first())
                        .size(80)
                        .build(),
                    contentDescription = "Imagem do serviço",
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
                    text = service.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray,
                    maxLines = 2
                )
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(service.price),
                    style = MaterialTheme.typography.titleSmall,
                    color = TaskGoGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.reviewerName.ifEmpty { "Cliente" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(review.rating.coerceIn(0, 5)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Estrela",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFD700)
                        )
                    }
                }
            }
            
            review.comment?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray
                )
            }
            
            review.createdAt?.let { date ->
                Text(
                    text = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TaskGoGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TaskGoTextGray
        )
    }
}

