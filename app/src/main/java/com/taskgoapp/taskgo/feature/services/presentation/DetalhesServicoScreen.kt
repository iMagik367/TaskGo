package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.FullScreenVideoPlayer
import com.taskgoapp.taskgo.core.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalhesServicoScreen(
    serviceId: String,
    onBackClick: () -> Unit,
    onEditService: (String) -> Unit = {},
    onNavigateToReviews: ((String) -> Unit)? = null,
    onNavigateToProviderProfile: ((String) -> Unit)? = null,
    onNavigateToChat: ((String) -> Unit)? = null,
    viewModel: ServiceDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    LaunchedEffect(serviceId) {
        if (serviceId.isNotBlank()) {
            Log.d("DetalhesServicoScreen", "Carregando serviço: $serviceId")
            viewModel.loadService(serviceId)
        }
    }
    
    // Verificar se o usuário atual é o dono do serviço
    val canEdit = remember(uiState.service, currentUserId) {
        uiState.service?.providerId == currentUserId && currentUserId.isNotBlank()
    }
    
    fun handleSendMessage(providerId: String) {
        scope.launch {
            try {
                val threadId = viewModel.getOrCreateThreadForProvider(providerId)
                onNavigateToChat?.invoke(threadId)
            } catch (e: Exception) {
                Log.e("DetalhesServicoScreen", "Erro ao criar/buscar thread", e)
                // Mostrar erro ao usuário se necessário
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Detalhes do Serviço",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            if (uiState.service != null && canEdit) {
                FloatingActionButton(
                    onClick = { onEditService(serviceId) },
                    containerColor = TaskGoGreen
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Serviço",
                        tint = Color.White
                    )
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
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            uiState.service != null -> {
                val service = uiState.service!!
                val priceFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    // Imagens do serviço
                    if (service.images.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = TaskGoBackgroundWhite
                            ),
                            border = BorderStroke(1.dp, TaskGoBorder)
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(service.images) { imageUrl ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageUrl)
                                            .size(600)
                                            .build(),
                                        contentDescription = "Imagem do serviço",
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(280.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Informações principais
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
                        border = BorderStroke(1.dp, TaskGoBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Título
                            Text(
                                text = service.title.ifEmpty { "Sem título" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoTextBlack
                            )

                            // Preço
                            Text(
                                text = priceFormat.format(service.price),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TaskGoPriceGreen
                            )

                            // Categoria - destacada
                            if (service.category.isNotBlank()) {
                                Surface(
                                    color = TaskGoGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = service.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TaskGoGreen,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Status
                            Surface(
                                color = if (service.active) TaskGoSuccess.copy(alpha = 0.1f) else TaskGoError.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (service.active) "Ativo" else "Inativo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (service.active) TaskGoSuccess else TaskGoError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Informações do Prestador - SEMPRE mostrar se houver providerId
                    if (service.providerId != null && service.providerId.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable {
                                    onNavigateToProviderProfile?.invoke(service.providerId!!)
                                },
                            colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Usar UserAvatarNameLoader para carregar dados do prestador
                                com.taskgoapp.taskgo.core.design.UserAvatarNameLoader(
                                    userId = service.providerId,
                                    onUserClick = {
                                        onNavigateToProviderProfile?.invoke(service.providerId!!)
                                    },
                                    avatarSize = 56.dp,
                                    showName = true,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Ícone de navegação
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Ver perfil",
                                    tint = TaskGoTextGray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Botão Enviar Mensagem (apenas se não for o próprio usuário)
                        if (service.providerId != currentUserId) {
                            Button(
                                onClick = {
                                    handleSendMessage(service.providerId!!)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TaskGoGreen
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enviar Mensagem",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Descrição
                    if (service.description.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                    Text(
                                    text = "Descrição",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextBlack
                    )
                    Text(
                                    text = service.description,
                                    style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray
                    )
                }
            }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vídeos
                    if (service.videos.isNotEmpty()) {
            Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
            ) {
                Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                                    text = "Vídeos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextBlack
                                )
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(service.videos) { videoUrl ->
                                        VideoThumbnailCard(
                                            videoUrl = videoUrl,
                                            modifier = Modifier.size(200.dp, 150.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tags
                    if (service.tags.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = TaskGoSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskGoTextBlack
                        )
                        Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    service.tags.forEach { tag ->
                                        Surface(
                                            color = TaskGoGreen.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TaskGoGreen,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Informações de data
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
                        border = BorderStroke(1.dp, TaskGoBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (service.createdAt != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                        imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                        tint = TaskGoTextGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                        text = "Criado em: ${java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(service.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                color = TaskGoTextGray
                            )
                        }
                    }

                            if (service.updatedAt != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TaskGoTextGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Atualizado em: ${java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(service.updatedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TaskGoTextGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(images[currentIndex])
                        .build(),
                    contentDescription = "Imagem em tela cheia",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Navigation arrows (if multiple images)
            if (images.size > 1) {
                // Previous arrow
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Imagem anterior",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Next arrow
                if (currentIndex < images.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Próxima imagem",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Image counter
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${images.size}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoThumbnailCard(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    var showVideoPlayer by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.clickable { showVideoPlayer = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TaskGoBackgroundWhite),
        border = BorderStroke(1.dp, TaskGoBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Vídeo",
                    modifier = Modifier.size(48.dp),
                    tint = TaskGoGreen
                )
                Text(
                    text = "Tocar vídeo",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
        }
    }
    
    // Fullscreen video player
    if (showVideoPlayer) {
        FullScreenVideoPlayer(
            videoUrl = videoUrl,
            onDismiss = { showVideoPlayer = false }
        )
    }
}

