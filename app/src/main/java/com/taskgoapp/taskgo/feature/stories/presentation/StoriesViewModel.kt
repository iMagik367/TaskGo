package com.taskgoapp.taskgo.feature.stories.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.data.repository.FeedMediaRepository
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.domain.repository.StoriesRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class StoriesUiState(
    val stories: List<Story> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserAvatarUrl: String? = null,
    val currentUserName: String = ""
)

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository,
    private val feedMediaRepository: FeedMediaRepository,
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StoriesUiState())
    val uiState: StateFlow<StoriesUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentUserProfile()
    }
    
    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUser()?.uid
                _uiState.value = _uiState.value.copy(currentUserId = currentUserId)
                
                userRepository.observeCurrentUser().collect { user ->
                    user?.let {
                        _uiState.value = _uiState.value.copy(
                            currentUserAvatarUrl = it.avatarUri,
                            currentUserName = it.name
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StoriesViewModel", "Erro ao carregar perfil: ${e.message}", e)
            }
        }
    }
    
    fun observeStories(radiusKm: Double = 50.0, userLocation: Pair<Double, Double>? = null) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUser()?.uid
            if (currentUserId == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Usuário não autenticado",
                    isLoading = false
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            storiesRepository.observeStories(currentUserId, radiusKm, userLocation)
                .catch { e ->
                    android.util.Log.e("StoriesViewModel", "Erro ao observar stories: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erro ao carregar stories",
                        isLoading = false
                    )
                }
                .collect { stories ->
                    _uiState.value = _uiState.value.copy(
                        stories = stories,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }
    
    fun createStory(
        mediaUri: android.net.Uri,
        mediaType: String,
        caption: String? = null,
        location: com.taskgoapp.taskgo.core.model.StoryLocation? = null
    ) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUser()?.uid
                    ?: run {
                        _uiState.value = _uiState.value.copy(error = "Usuário não autenticado")
                        return@launch
                    }
                
                val currentUser = _uiState.value
                val userName = currentUser.currentUserName.ifEmpty { "Usuário" }
                val userAvatarUrl = currentUser.currentUserAvatarUrl
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Upload da mídia usando o método específico para stories
                val uploadResult = feedMediaRepository.uploadStoryMedia(
                    uri = mediaUri,
                    userId = currentUserId,
                    mediaType = mediaType
                )
                
                when (uploadResult) {
                    is Result.Success -> {
                        val mediaUrl = uploadResult.data
                        val createdAt = Date()
                        val expiresAt = Date(createdAt.time + 24 * 60 * 60 * 1000) // +24 horas
                        
                        val story = Story(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserId,
                            userName = userName,
                            userAvatarUrl = userAvatarUrl,
                            mediaUrl = mediaUrl,
                            mediaType = mediaType,
                            caption = caption,
                            createdAt = createdAt,
                            expiresAt = expiresAt,
                            location = location
                        )
                        
                        val createResult = storiesRepository.createStory(story)
                        when (createResult) {
                            is Result.Success -> {
                                _uiState.value = _uiState.value.copy(isLoading = false)
                            }
                            is Result.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = createResult.exception.message ?: "Erro ao criar story"
                                )
                            }
                            else -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Erro desconhecido ao criar story"
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = uploadResult.exception.message ?: "Erro ao fazer upload da mídia"
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro desconhecido ao fazer upload"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StoriesViewModel", "Erro ao criar story: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao criar story"
                )
            }
        }
    }
    
    fun markStoryAsViewed(storyId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUser()?.uid
                    ?: return@launch
                
                storiesRepository.markStoryAsViewed(storyId, currentUserId)
            } catch (e: Exception) {
                android.util.Log.e("StoriesViewModel", "Erro ao marcar story como visualizada: ${e.message}", e)
            }
        }
    }
    
    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUser()?.uid
                    ?: run {
                        _uiState.value = _uiState.value.copy(error = "Usuário não autenticado")
                        return@launch
                    }
                
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = storiesRepository.deleteStory(storyId, currentUserId)
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Erro ao deletar story"
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro desconhecido"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StoriesViewModel", "Erro ao deletar story: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao deletar story"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

