package com.taskgoapp.taskgo.feature.feed.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.feature.feed.presentation.components.PostCard
import com.taskgoapp.taskgo.core.theme.*

/**
 * Tela de detalhes de um post específico
 * Acessível via deep link: https://taskgo.app/post/{postId}
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBackClick: () -> Unit,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Carregar post específico quando a tela for aberta
    LaunchedEffect(postId) {
        viewModel.loadPostById(postId)
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Post",
                onBackClick = onBackClick,
                backgroundColor = TaskGoGreen,
                titleColor = androidx.compose.ui.graphics.Color.White
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
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
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Erro ao carregar post",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadPostById(postId) }) {
                        Text("Tentar novamente")
                    }
                }
            }
            else -> {
                val post = uiState.selectedPost
                if (post != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.likePost(post.id) },
                            onUnlikeClick = { viewModel.unlikePost(post.id) },
                            onDeleteClick = if (post.userId == viewModel.currentUserId) {
                                { 
                                    viewModel.deletePost(post.id)
                                    onBackClick()
                                }
                            } else null,
                            onUserClick = onNavigateToUserProfile,
                            currentUserId = viewModel.currentUserId,
                            onCommentClick = { /* Dialog será aberto automaticamente pelo PostCard */ },
                            onShareClick = { /* Dialog será aberto automaticamente pelo PostCard */ },
                            onInterestClick = { hasInterest ->
                                // TODO: Implementar lógica de interesse
                                android.util.Log.d("PostDetailScreen", "Interesse: $hasInterest para post ${post.id}")
                            },
                            onRatePostClick = {
                                // TODO: Implementar lógica de avaliação
                                android.util.Log.d("PostDetailScreen", "Avaliar post ${post.id}")
                            },
                            onBlockUserClick = {
                                // TODO: Implementar lógica de bloqueio
                                android.util.Log.d("PostDetailScreen", "Bloquear usuário ${post.userId}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "Post não encontrado",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
