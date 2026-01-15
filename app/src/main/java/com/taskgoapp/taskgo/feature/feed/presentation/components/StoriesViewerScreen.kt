package com.taskgoapp.taskgo.feature.feed.presentation.components

import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.theme.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun StoriesViewerScreen(
    stories: List<Story>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onStoryViewed: (String) -> Unit,
    onUserClick: ((String) -> Unit)? = null,
    currentUserId: String? = null, // ID do usuário atual para verificar se é dono
    onSwipeUp: ((String) -> Unit)? = null, // Callback para swipe up (apenas stories próprias)
    onTrackAction: ((String, String) -> Unit)? = null // Callback para tracking de ações (storyId, action)
) {
    if (stories.isEmpty()) {
        onDismiss()
        return
    }
    
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, stories.size - 1)) }
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val currentStory = stories.getOrNull(currentIndex)
    val isVideo = currentStory?.mediaType == "video"

    // Player de vídeo dedicado para o story atual (limpa ao trocar de story)
    val exoPlayer = remember(currentIndex) {
        ExoPlayer.Builder(context).build().apply {
            currentStory?.let { story ->
                setMediaItem(MediaItem.fromUri(story.mediaUrl))
                prepare()
                playWhenReady = true
            }
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    
    val isOwnStory = currentStory != null && currentUserId != null && currentStory.userId == currentUserId
    
    // Marcar como visto ao entrar em cada story
    LaunchedEffect(currentIndex) {
        currentStory?.let { onStoryViewed(it.id) }
        progress = 0f
        isPaused = false
    }

    // Auto-avance: imagem 5s, vídeo pela duração (máx 30s) ou 30s fallback
    LaunchedEffect(currentIndex, isPaused) {
        val story = stories.getOrNull(currentIndex) ?: return@LaunchedEffect
        progress = 0f

        if (story.mediaType == "video") {
            while (true) {
                if (!isPaused) {
                    val duration = exoPlayer.duration.takeIf { it > 0 }?.coerceAtMost(30_000L) ?: 30_000L
                    val position = exoPlayer.currentPosition
                    progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                    if (exoPlayer.playbackState == Player.STATE_ENDED || position >= duration) {
                        onStoryViewed(story.id)
                        if (currentIndex < stories.size - 1) {
                            onTrackAction?.invoke(story.id, "next_auto")
                            currentIndex++
                            break
                        } else {
                            onDismiss()
                            break
                        }
                    }
                    exoPlayer.playWhenReady = true
                } else {
                    exoPlayer.playWhenReady = false
                }
                delay(50)
            }
        } else {
            val duration = 5_000L
            while (progress < 1f && !isPaused) {
                delay(50)
                progress += 50f / duration.toFloat()
            }
            if (progress >= 1f) {
                onStoryViewed(story.id)
                if (currentIndex < stories.size - 1) {
                    onTrackAction?.invoke(story.id, "next_auto")
                    isPaused = false
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }
    
    if (currentStory == null) {
        onDismiss()
        return
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
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            // Detectar swipe up (dragAmount.y negativo significativo)
                            if (dragAmount.y < -50 && isOwnStory && onSwipeUp != null) {
                                // Swipe up detectado - abrir analytics
                                currentStory?.let { story ->
                                    onSwipeUp(story.id)
                                    onTrackAction?.invoke(story.id, "swipe_up") // Track action
                                }
                                return@detectDragGestures
                            }
                            
                            // Para swipe down (dragAmount.y positivo)
                            if (dragAmount.y > 100) {
                                // Swipe down significativo - fechar viewer
                                currentStory?.let { story ->
                                    onTrackAction?.invoke(story.id, "back") // Track back action
                                }
                                onDismiss()
                                return@detectDragGestures
                            }
                            
                            dragOffsetY += dragAmount.y
                        },
                        onDragEnd = {
                            dragOffsetY = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            if (isVideo) exoPlayer.playWhenReady = false
                            tryAwaitRelease()
                            isPaused = false
                            if (isVideo) exoPlayer.playWhenReady = true
                        },
                        onTap = { offset ->
                            val story = stories.getOrNull(currentIndex) ?: return@detectTapGestures
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                // Tap na esquerda - voltar
                                if (currentIndex > 0) {
                                    onTrackAction?.invoke(story.id, "back")
                                    currentIndex--
                                    progress = 0f
                                } else {
                                    onTrackAction?.invoke(story.id, "back")
                                    onDismiss()
                                }
                            } else {
                                // Tap na direita - avançar
                                if (currentIndex < stories.size - 1) {
                                    onTrackAction?.invoke(story.id, "next_tap")
                                    currentIndex++
                                    progress = 0f
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Barra de progresso no topo
            StoryProgressBar(
                stories = stories,
                currentIndex = currentIndex,
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(4.dp)
            )
            
            // Header com avatar e nome do usuário
            StoryHeader(
                story = currentStory,
                onClose = onDismiss,
                onUserClick = {
                    // Track profile visit action when user clicks on header
                    onUserClick?.invoke(currentStory.userId)
                    if (currentUserId != null && currentStory.userId != currentUserId) {
                        // Usuário visualizou o perfil de outro usuário a partir da story
                        onTrackAction?.invoke(currentStory.id, "navigation")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Conteúdo da story (imagem ou vídeo)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentStory.mediaType == "video") {
                    // Player de vídeo com ExoPlayer (comportamento IG-like)
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = currentStory.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Caption (se houver)
                currentStory.caption?.let { caption ->
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
            
            // Indicador de pausa
            if (isPaused) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pausado",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                )
            }
        }
    }
}

@Composable
private fun StoryProgressBar(
    stories: List<Story>,
    currentIndex: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        stories.forEachIndexed { index, _ ->
            val segmentProgress = when {
                index < currentIndex -> 1f
                index == currentIndex -> progress
                else -> 0f
            }
            
            LinearProgressIndicator(
                progress = segmentProgress,
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun StoryHeader(
    story: Story,
    onClose: () -> Unit,
    onUserClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onUserClick?.invoke(story.userId) },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = story.userAvatarUrl ?: "",
                contentDescription = story.userName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column {
                Text(
                    text = story.userName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatStoryTime(story.createdAt),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
        
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar",
                tint = Color.White
            )
        }
    }
}

private fun formatStoryTime(date: java.util.Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    val hours = (diff / (1000 * 60 * 60)).toInt()
    
    return when {
        hours < 1 -> "Agora"
        hours == 1 -> "1 hora atrás"
        hours < 24 -> "$hours horas atrás"
        else -> "${hours / 24} dias atrás"
    }
}

