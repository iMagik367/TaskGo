package com.taskgoapp.taskgo.feature.feed.presentation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.TGIcon
import com.taskgoapp.taskgo.core.design.TGIcons
import com.taskgoapp.taskgo.feature.feed.presentation.components.PostCard
import com.taskgoapp.taskgo.feature.feed.presentation.components.RadiusFilterDialog
import com.taskgoapp.taskgo.feature.feed.presentation.components.StoriesSectionNew
import com.taskgoapp.taskgo.feature.feed.presentation.components.RatePostDialog
import com.taskgoapp.taskgo.feature.feed.presentation.components.BlockUserDialog
import com.taskgoapp.taskgo.feature.feed.presentation.components.StoriesViewerScreen
import com.taskgoapp.taskgo.feature.feed.presentation.components.CreateStoryScreen
import com.taskgoapp.taskgo.feature.feed.presentation.components.StoryAnalyticsScreen
import com.taskgoapp.taskgo.feature.feed.presentation.StoriesViewModel
import com.taskgoapp.taskgo.feature.feed.presentation.components.InlinePostCreator
import com.taskgoapp.taskgo.core.theme.*
import androidx.compose.ui.res.stringResource
import com.taskgoapp.taskgo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    storiesViewModel: StoriesViewModel = hiltViewModel(),
    onNavigateToMessages: () -> Unit = {},
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val storiesUiState by storiesViewModel.uiState.collectAsState()
    var showRadiusFilterDialog by remember { mutableStateOf(false) }
    var showCreateStory by remember { mutableStateOf(false) }
    var selectedUserIdForStory by remember { mutableStateOf<String?>(null) }
    var showStoryAnalytics by remember { mutableStateOf<String?>(null) } // storyId quando analytics deve ser exibido

    // Observa stories com a localização atual do feed (estilo IG)
    LaunchedEffect(uiState.userLocation, uiState.currentRadius) {
        storiesViewModel.loadStories(
            radiusKm = uiState.currentRadius,
            userLocation = uiState.userLocation
        )
    }

    // Estados para dialogs de ações do post
    var postToRate by remember { mutableStateOf<com.taskgoapp.taskgo.core.model.Post?>(null) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) } // userId, userName

    
    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    title = "Feed",
                    backgroundColor = TaskGoGreen,
                    titleColor = Color.White,
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showRadiusFilterDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtro de raio",
                                    tint = Color.White,
                                    modifier = Modifier.size(TGIcons.Sizes.Medium)
                                )
                            }
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
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.posts.isEmpty() -> {
                // Loading inicial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TaskGoGreen)
                }
            }
            
            uiState.error != null && uiState.posts.isEmpty() -> {
                // Erro
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
                        onClick = {
                            viewModel.clearError()
                            viewModel.refreshFeed()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                    ) {
                        Text("Tentar novamente")
                    }
                }
            }
            
            else -> {
                // Feed principal com Stories e Posts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Campo de criação de post inline (apenas para prestadores e vendedores)
                    if (uiState.canPost) {
                        item {
                            InlinePostCreator(
                                userAvatarUrl = uiState.currentUserAvatarUrl,
                                userName = uiState.currentUserName,
                                isLoading = uiState.isLoading,
                                onPostCreated = { text, mediaUris ->
                                    viewModel.createPost(text, mediaUris)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    
                    // Seção de Stories (apenas prestadores e vendedores podem criar stories)
                    item {
                        StoriesSectionNew(
                            currentUserAvatarUrl = uiState.currentUserAvatarUrl,
                            currentUserName = uiState.currentUserName,
                            currentUserId = viewModel.currentUserId,
                            stories = storiesUiState.stories,
                            onCreateStoryClick = if (uiState.canPost) {
                                { showCreateStory = true }
                            } else {
                                null
                            },
                            onStoryClick = { userId ->
                                selectedUserIdForStory = userId
                            }
                        )
                    }
                    
                    // Divisor
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = TaskGoBackgroundGray
                        )
                    }
                    
                    // Lista de posts
                    if (uiState.posts.isEmpty()) {
                        item {
                            // Empty state
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Nenhum post encontrado",
                                    style = MaterialTheme.typography.titleMedium,
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
                            items = uiState.posts,
                            key = { it.id }
                        ) { post ->
                            PostCard(
                                post = post,
                                onLikeClick = { viewModel.likePost(post.id) },
                                onUnlikeClick = { viewModel.unlikePost(post.id) },
                                onDeleteClick = if (post.userId == viewModel.currentUserId) {
                                    { viewModel.deletePost(post.id) }
                                } else {
                                    null
                                },
                                onUserClick = onNavigateToUserProfile,
                                currentUserId = viewModel.currentUserId,
                                onCommentClick = { /* Dialog será aberto automaticamente pelo PostCard */ },
                                onShareClick = { /* Dialog será aberto automaticamente pelo PostCard */ },
                                onInterestClick = { hasInterest ->
                                    viewModel.setPostInterest(post.id, hasInterest)
                                },
                                onRatePostClick = {
                                    postToRate = post
                                },
                                onBlockUserClick = {
                                    userToBlock = post.userId to post.userName
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    
                    // Loading mais posts
                    if (uiState.isLoading && uiState.posts.isNotEmpty()) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally),
                                color = TaskGoGreen
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialog para filtrar raio
    if (showRadiusFilterDialog) {
        RadiusFilterDialog(
            currentRadius = uiState.currentRadius,
            onRadiusChanged = { newRadius ->
                viewModel.updateRadius(newRadius)
            },
            onDismiss = { showRadiusFilterDialog = false }
        )
    }
    
    // Dialog de criação de Story
    if (showCreateStory) {
        CreateStoryScreen(
            onDismiss = { showCreateStory = false },
            onStoryCreated = {
                storiesViewModel.loadStories()
                showCreateStory = false
            }
        )
    }
    
    // Viewer de Stories
    selectedUserIdForStory?.let { userId ->
        val userStories = storiesUiState.stories.filter { it.userId == userId }
        val isOwnStory = userId == viewModel.currentUserId
        if (userStories.isNotEmpty()) {
            StoriesViewerScreen(
                stories = userStories,
                initialIndex = 0,
                onDismiss = { 
                    selectedUserIdForStory = null
                    showStoryAnalytics = null
                },
                onStoryViewed = { storyId ->
                    storiesViewModel.markStoryAsViewed(storyId)
                },
                onUserClick = onNavigateToUserProfile,
                currentUserId = viewModel.currentUserId,
                onSwipeUp = if (isOwnStory) { storyId: String ->
                    // Abrir analytics quando swipe up em story própria
                    showStoryAnalytics = storyId
                } else null,
                onTrackAction = { storyId: String, action: String ->
                    // Track story actions
                    storiesViewModel.trackStoryAction(storyId, action)
                }
            )
        }
    }
    
    // Tela de Analytics de Story (swipe up)
    showStoryAnalytics?.let { storyId ->
        val story = storiesUiState.stories.find { it.id == storyId }
        val userStories = storiesUiState.stories.filter { it.userId == story?.userId }
        val ownerUserId = story?.userId ?: viewModel.currentUserId ?: ""
        
        if (ownerUserId.isNotBlank()) {
            StoryAnalyticsScreen(
                storyId = storyId,
                ownerUserId = ownerUserId,
                userStories = userStories,
                onDismiss = { showStoryAnalytics = null }
            )
        }
    }
    
    // Dialog de avaliação de post
    postToRate?.let { post ->
        var existingRating by remember { mutableStateOf<com.taskgoapp.taskgo.core.model.PostRating?>(null) }
        
        LaunchedEffect(post.id) {
            withContext(Dispatchers.IO) {
                existingRating = viewModel.getUserPostRating(post.id)
            }
        }
        
        RatePostDialog(
            onDismiss = { postToRate = null },
            onConfirm = { rating, comment ->
                viewModel.ratePost(post.id, rating, comment)
                postToRate = null
            },
            existingRating = existingRating?.rating,
            existingComment = existingRating?.comment
        )
    }
    
    // Dialog de bloqueio de usuário
    userToBlock?.let { (userId, userName) ->
        BlockUserDialog(
            userName = userName,
            onDismiss = { userToBlock = null },
            onConfirm = {
                viewModel.blockUser(userId)
                userToBlock = null
            }
        )
    }
}
