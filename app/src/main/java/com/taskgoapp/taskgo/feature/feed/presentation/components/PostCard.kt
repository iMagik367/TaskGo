package com.taskgoapp.taskgo.feature.feed.presentation.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.design.FullScreenVideoPlayer
import com.taskgoapp.taskgo.core.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onUnlikeClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null, // null se não for post do usuário atual
    onUserClick: ((String) -> Unit)? = null, // Callback para quando clicar no nome/avatar do usuário
    currentUserId: String? = null, // ID do usuário atual para verificar se é post próprio
    onCommentClick: (() -> Unit)? = null, // Callback para abrir dialog de comentários
    onShareClick: (() -> Unit)? = null, // Callback para abrir dialog de compartilhar
    onInterestClick: ((Boolean) -> Unit)? = null, // Callback para "Tenho interesse" / "Não tenho interesse"
    onRatePostClick: (() -> Unit)? = null, // Callback para avaliar post
    onBlockUserClick: (() -> Unit)? = null, // Callback para bloquear usuário
    modifier: Modifier = Modifier
) {
    var showFullScreenVideo by remember { mutableStateOf<String?>(null) }
    var showDeleteMenu by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showPostMenu by remember { mutableStateOf(false) }
    val isOwnPost = currentUserId != null && post.userId == currentUserId
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Header: Avatar, Nome, Data, Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = onUserClick != null) {
                        onUserClick?.invoke(post.userId)
                    }
            ) {
                // Avatar circular
                AsyncImage(
                    model = post.userAvatarUrl ?: "",
                    contentDescription = "Avatar de ${post.userName}",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatPostDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Menu de três pontos (sempre visível - opções diferentes para posts próprios vs outros)
            IconButton(
                onClick = { showPostMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mais opções",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Carrossel de Mídias (largura total, estilo Instagram)
        if (post.mediaUrls.isNotEmpty()) {
            val hasMultipleMedia = post.mediaUrls.size > 1
            val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
            
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Aspecto quadrado como Instagram
                ) { page ->
                    val mediaUrl = post.mediaUrls[page]
                    val mediaType = post.mediaTypes.getOrNull(page) ?: "image"
                    
                    if (mediaType == "video") {
                        VideoThumbnail(
                            videoUrl = mediaUrl,
                            onVideoClick = { showFullScreenVideo = mediaUrl },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Imagem ${page + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Indicadores de página (dots) - apenas se houver múltiplas imagens
                if (hasMultipleMedia) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(post.mediaUrls.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        // Ações: Like, Comentário, Compartilhar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like
            IconButton(
                onClick = {
                    if (post.isLiked) {
                        onUnlikeClick()
                    } else {
                        onLikeClick()
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (post.isLiked) "Descurtir" else "Curtir",
                    tint = if (post.isLiked) Color(0xFFED4956) else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Comentário
            IconButton(
                onClick = { 
                    showCommentsDialog = true
                    onCommentClick?.invoke()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ModeComment,
                    contentDescription = "Comentar",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Compartilhar
            IconButton(
                onClick = { 
                    showShareDialog = true
                    onShareClick?.invoke()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Compartilhar",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Contador de Likes
        if (post.likesCount > 0) {
            Text(
                text = "${post.likesCount} curtidas",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        
        // Texto do post com nome do usuário em destaque
        if (post.text.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = post.userName + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(enabled = onUserClick != null) {
                        onUserClick?.invoke(post.userId)
                    }
                )
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Contador de Comentários
        if (post.commentsCount > 0) {
            Text(
                text = "Ver todos os ${post.commentsCount} comentários",
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clickable { 
                        showCommentsDialog = true
                        onCommentClick?.invoke()
                    }
            )
        }
        
        // Timestamp
        Text(
            text = formatPostDate(post.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = TaskGoTextGray,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        
        // Divisor
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.3f)
        )
    }
    
    // Menu de opções do post (3 pontos)
    if (showPostMenu) {
        PostOptionsMenu(
            isOwnPost = isOwnPost,
            onDismiss = { showPostMenu = false },
            onDelete = {
                showPostMenu = false
                showDeleteMenu = true
            },
            onInterest = { hasInterest ->
                onInterestClick?.invoke(hasInterest)
                showPostMenu = false
            },
            onRate = {
                onRatePostClick?.invoke()
                showPostMenu = false
            },
            onBlock = {
                onBlockUserClick?.invoke()
                showPostMenu = false
            }
        )
    }
    
    // Menu de delete (confirmação)
    if (showDeleteMenu && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteMenu = false },
            title = { Text("Deletar post") },
            text = { Text("Tem certeza que deseja deletar este post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteMenu = false
                    }
                ) {
                    Text("Deletar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMenu = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Dialog de comentários
    if (showCommentsDialog) {
        CommentsDialog(
            postId = post.id,
            postUserId = post.userId,
            currentUserId = currentUserId,
            onDismiss = { showCommentsDialog = false }
        )
    }
    
    // Dialog de compartilhar
    if (showShareDialog) {
        SharePostDialog(
            postId = post.id,
            onDismiss = { showShareDialog = false }
        )
    }
    
    // Player de vídeo em tela cheia
    showFullScreenVideo?.let { videoUrl ->
        FullScreenVideoPlayer(
            videoUrl = videoUrl,
            onDismiss = { showFullScreenVideo = null }
        )
    }
}

@Composable
private fun VideoThumbnail(
    videoUrl: String,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onVideoClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = "Tocar vídeo",
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "Tocar vídeo",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        }
    }
}

private fun formatPostDate(date: Date?): String {
    if (date == null) return ""
    
    val now = Date()
    val diff = now.time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "agora"
        minutes < 60 -> "há ${minutes}min"
        hours < 24 -> "há ${hours}h"
        days < 7 -> "há ${days}d"
        else -> {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            format.format(date)
        }
    }
}
