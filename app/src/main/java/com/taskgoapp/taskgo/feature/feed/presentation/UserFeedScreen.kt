package com.taskgoapp.taskgo.feature.feed.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.feature.feed.presentation.components.PostCard
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun UserFeedScreen(
    userId: String,
    viewModel: UserFeedViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember(userId) { userId }
    
    LaunchedEffect(currentUserId) {
        viewModel.loadUserPosts(currentUserId)
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading && uiState.posts.isEmpty() -> {
                // Loading inicial
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TaskGoGreen
                )
            }
            
            uiState.error != null && uiState.posts.isEmpty() -> {
                // Erro
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                            viewModel.loadUserPosts(currentUserId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Text("Tentar novamente")
                    }
                }
            }
            
            uiState.posts.isEmpty() -> {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nenhum post ainda",
                        style = MaterialTheme.typography.titleMedium,
                        color = TaskGoTextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Este usuário ainda não publicou nada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextGray
                    )
                }
            }
            
            else -> {
                // Lista de posts
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.posts,
                        key = { it.id }
                    ) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { /* Like será implementado quando necessário */ },
                            onUnlikeClick = { /* Unlike será implementado quando necessário */ },
                            onDeleteClick = null, // Feed do perfil não permite deletar
                            currentUserId = null, // TODO: Obter currentUserId se necessário
                            onCommentClick = { /* Dialog será aberto automaticamente pelo PostCard */ },
                            onShareClick = { /* Dialog será aberto automaticamente pelo PostCard */ },
                            onInterestClick = { /* TODO: Implementar */ },
                            onRatePostClick = { /* TODO: Implementar */ },
                            onBlockUserClick = { /* TODO: Implementar */ }
                        )
                    }
                    
                    // Loading mais posts (se implementar paginação)
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
}
