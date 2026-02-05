package com.taskgoapp.taskgo.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.domain.usecase.GetUserPostsUseCase
import com.taskgoapp.taskgo.domain.usecase.DeletePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserFeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UserFeedViewModel @Inject constructor(
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val deletePostUseCase: DeletePostUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserFeedUiState())
    val uiState: StateFlow<UserFeedUiState> = _uiState.asStateFlow()
    
    /**
     * Carrega posts de um usuário específico
     */
    fun loadUserPosts(userId: String) {
        android.util.Log.d("UserFeedViewModel", "🔵 loadUserPosts: Iniciando para userId=$userId")
        
        if (userId.isBlank()) {
            android.util.Log.e("UserFeedViewModel", "🔴 ERRO: userId está vazio!")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "UserId não pode estar vazio"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                android.util.Log.d("UserFeedViewModel", "🟢 Chamando getUserPostsUseCase...")
                val flow = getUserPostsUseCase(userId)
                android.util.Log.d("UserFeedViewModel", "   Flow obtido, iniciando coleta...")
                
                flow.collect { posts ->
                    android.util.Log.d("UserFeedViewModel", "🟡 COLECT: Recebidos ${posts.size} posts do UseCase")
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                    android.util.Log.d("UserFeedViewModel", "✅ UI atualizada com ${posts.size} posts")
                }
            } catch (e: Exception) {
                android.util.Log.e("UserFeedViewModel", "🔴 EXCEPTION em loadUserPosts", e)
                android.util.Log.e("UserFeedViewModel", "   Tipo: ${e.javaClass.simpleName}, Mensagem: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar posts: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Deleta um post
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = deletePostUseCase(postId)
            if (result is Result.Error) {
                android.util.Log.e("UserFeedViewModel", "Erro ao deletar post: ${result.exception.message}", result.exception)
                _uiState.value = _uiState.value.copy(error = "Erro ao deletar post: ${result.exception.message}")
            }
        }
    }
    
    /**
     * Limpa o erro
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
