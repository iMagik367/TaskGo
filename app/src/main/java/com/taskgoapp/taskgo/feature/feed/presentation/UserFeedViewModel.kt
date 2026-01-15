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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                getUserPostsUseCase(userId).collect { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("UserFeedViewModel", "Erro ao carregar posts do usuário: ${e.message}", e)
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
