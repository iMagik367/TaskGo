package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.model.StoryView
import com.taskgoapp.taskgo.feature.feed.presentation.StoryAnalyticsViewModel
import com.taskgoapp.taskgo.core.theme.*

/**
 * Tela de Analytics de Story (estilo Instagram)
 * Exibida quando o usuário faz swipe up em uma story própria
 */
@Composable
fun StoryAnalyticsScreen(
    storyId: String,
    ownerUserId: String,
    userStories: List<Story> = emptyList(), // Stories do usuário para preview
    onDismiss: () -> Unit,
    viewModel: StoryAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Interações, 1 = Descoberta
    
    LaunchedEffect(storyId, ownerUserId) {
        viewModel.loadAnalytics(storyId, ownerUserId)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A)) // Fundo escuro estilo Instagram
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header com X e título
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.size(40.dp)) // Espaçamento para alinhar título
                }
                
                // Preview de stories (horizontal scrollável)
                if (userStories.isNotEmpty()) {
                    StoryPreviewsSection(
                        stories = userStories,
                        selectedStoryId = storyId,
                        currentViews = uiState.analytics?.views?.size ?: 0
                    )
                }
                
                // Abas: Interações e Descoberta
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    TabButton(
                        text = "Interações",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Descoberta",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Conteúdo das abas
                when (selectedTab) {
                    0 -> InteractionsTab(uiState.analytics)
                    1 -> DiscoveryTab(uiState.analytics)
                }
            }
            
            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StoryPreviewsSection(
    stories: List<Story>,
    selectedStoryId: String,
    currentViews: Int
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            val isSelected = story.id == selectedStoryId
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color(0xFFFF1744) else Color(0xFF424242)
                    )
                    .padding(if (isSelected) 2.dp else 0.dp)
            ) {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Contador de visualizações
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(4.dp)
                            .background(Color(0xFFFF1744).copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$currentViews",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFF999999),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Linha de seleção
        if (isSelected) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterHorizontally),
                thickness = 2.dp,
                color = Color(0xFF0095F6) // Azul Instagram
            )
        }
    }
}

@Composable
private fun InteractionsTab(analytics: com.taskgoapp.taskgo.core.model.StoryAnalytics?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Interações",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            MetricRow(
                label = "Ações executadas a partir desse story",
                value = "0", // TODO: Implementar tracking de ações
                showInfo = false
            )
        }
        
        item {
            MetricRow(
                label = "Visitas ao perfil",
                value = "${analytics?.interactions?.profileVisits ?: 0}",
                showInfo = false
            )
        }
        
        // Lista de visualizações
        item {
            Text(
                text = "Visualizações (${analytics?.views?.size ?: 0})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        
        if (analytics?.views.isNullOrEmpty()) {
            item {
                Text(
                    text = "Nenhuma visualização ainda",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        } else {
            items(analytics?.views ?: emptyList(), key = { it.userId }) { view ->
                StoryViewRow(
                    view = view,
                    onSendMessage = { /* TODO: Implementar mensagem direta */ },
                    onMoreOptions = { /* TODO: Implementar opções */ }
                )
            }
        }
    }
}

@Composable
private fun DiscoveryTab(analytics: com.taskgoapp.taskgo.core.model.StoryAnalytics?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Descoberta",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            // Contas alcançadas (destaque)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "${analytics?.accountsReached ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Contas alcançadas",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        item {
            MetricRow(
                label = "Impressões",
                value = "${analytics?.impressions ?: 0}",
                showInfo = true
            )
        }
        
        item {
            MetricRow(
                label = "Seguidores",
                value = "${analytics?.followers ?: 0}",
                showInfo = true
            )
        }
        
        item {
            MetricRow(
                label = "Navegação",
                value = "${analytics?.navigation ?: 0}",
                showInfo = true
            )
        }
        
        item {
            MetricRow(
                label = "Voltar",
                value = "${analytics?.back ?: 0}",
                showInfo = true
            )
        }
        
        item {
            MetricRow(
                label = "Alinhamentos",
                value = "${analytics?.alignments ?: 0}",
                showInfo = true
            )
        }
        
        item {
            MetricRow(
                label = "No story",
                value = "${analytics?.views?.size ?: 0}",
                showInfo = true
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    showInfo: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
            if (showInfo) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Informação",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StoryViewRow(
    view: StoryView,
    onSendMessage: () -> Unit,
    onMoreOptions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = view.userAvatarUrl ?: "",
                contentDescription = view.userName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column {
                Text(
                    text = view.userName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatViewTime(view.viewedAt),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoreOptions,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mais opções",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = onSendMessage,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar mensagem",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatViewTime(date: java.util.Date): String {
    val now = java.util.Date()
    val diff = now.time - date.time
    val hours = (diff / (1000 * 60 * 60)).toInt()
    
    return when {
        hours < 1 -> "Agora"
        hours == 1 -> "1 hora atrás"
        hours < 24 -> "$hours horas atrás"
        else -> "${hours / 24} dias atrás"
    }
}
