package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun StoriesSectionNew(
    currentUserAvatarUrl: String?,
    currentUserName: String,
    currentUserId: String?,
    stories: List<Story>,
    onCreateStoryClick: (() -> Unit)?,
    onStoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Verificar se o usuário atual tem stories próprias
    val currentUserHasStories = remember(stories, currentUserId) {
        currentUserId != null && stories.any { it.userId == currentUserId }
    }
    
    // Agrupar stories por usuário: prioriza story não vista para mostrar o anel colorido
    val uniqueUserStories = stories
        .groupBy { it.userId }
        .mapValues { (_, stories) ->
            stories.firstOrNull { !it.isViewed } ?: stories.first()
        }
        .values
        .toList()
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Meu Story (primeiro) - apenas se pode criar stories
        if (onCreateStoryClick != null) {
            item {
                StoryCircle(
                    userName = "Seu story",
                    avatarUrl = currentUserAvatarUrl,
                    isMyStory = true,
                    hasStories = currentUserHasStories,
                    onClick = onCreateStoryClick
                )
            }
        }
        
        // Stories de outros usuários
        items(uniqueUserStories, key = { it.id }) { story ->
            StoryCircle(
                userName = story.userName,
                avatarUrl = story.userAvatarUrl,
                isMyStory = false,
                hasStories = !story.isViewed,
                onClick = { onStoryClick(story.userId) }
            )
        }
    }
}

@Composable
private fun StoryCircle(
    userName: String,
    avatarUrl: String?,
    isMyStory: Boolean,
    hasStories: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Gradiente vibrante estilo Instagram (roxo, rosa, laranja, amarelo)
    val gradientColors = remember {
        if (hasStories) {
            listOf(
                Color(0xFF833AB4), // Roxo
                Color(0xFFFD1D1D), // Vermelho/Rosa
                Color(0xFFFCB045)  // Laranja/Amarelo
            )
        } else {
            listOf(
                Color(0xFFC7C7CC), // Cinza claro
                Color(0xFFA8A8A8)  // Cinza médio
            )
        }
    }
    
    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            // Borda colorida se tiver stories (usuários ou próprio)
            Box(
                modifier = Modifier
                    .size(if (hasStories) 64.dp else 56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(0f, 0f),
                            end = Offset(64f, 64f)
                        ),
                        shape = CircleShape
                    )
                    .padding(if (hasStories) 2.5.dp else 0.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = userName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                    color = Color(0xFF833AB4),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = userName,
                                modifier = Modifier.size(30.dp),
                                    tint = Color(0xFF999999)
                            )
                        }
                    },
                    success = { state ->
                        SubcomposeAsyncImageContent(
                            painter = state.painter,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                )
            }
            
            // Botão de adicionar (apenas para meu story)
            if (isMyStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(20.dp)
                        .background(Color.White, CircleShape)
                        .padding(1.dp)
                        .background(Color(0xFF0095F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar story",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = userName,
            style = MaterialTheme.typography.bodySmall,
            color = TaskGoTextBlack,
            maxLines = 1,
            fontSize = 11.sp,
            fontWeight = if (hasStories) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

