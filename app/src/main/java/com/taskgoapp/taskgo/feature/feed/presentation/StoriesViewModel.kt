package com.taskgoapp.taskgo.feature.feed.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.model.StoryLocation
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
    val userStories: Map<String, List<Story>> = emptyMap(), // userId -> List<Story>
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String = "",
    val currentUserAvatarUrl: String? = null
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
    
    val currentUserId: String?
        get() = authRepository.getCurrentUser()?.uid
    
    init {
        loadCurrentUserProfile()
        loadStories(50.0, null)
    }
    
    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                user?.let {
                    _uiState.value = _uiState.value.copy(
                        currentUserId = it.id,
                        currentUserName = it.name,
                        currentUserAvatarUrl = it.avatarUri
                    )
                }
            }
        }
    }
    
    fun loadStories(radiusKm: Double = 50.0, userLocation: Pair<Double, Double>? = null) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                storiesRepository.observeStories(userId, radiusKm, userLocation)
                    .catch { e ->
                        android.util.Log.e("StoriesViewModel", "Erro ao carregar stories: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar stories: ${e.message}"
                        )
                    }
                    .collect { stories ->
                        // Agrupar stories por usuário
                        val grouped = stories.groupBy { it.userId }
                        _uiState.value = _uiState.value.copy(
                            stories = stories,
                            userStories = grouped,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("StoriesViewModel", "Erro ao observar stories: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar stories: ${e.message}"
                )
            }
        }
    }
    
    fun observeUserStories(userId: String): Flow<List<Story>> {
        val currentUserId = currentUserId ?: return flowOf(emptyList())
        return storiesRepository.observeUserStories(userId, currentUserId)
            .catch { e ->
                android.util.Log.e("StoriesViewModel", "Erro ao carregar stories do usuário: ${e.message}", e)
                emit(emptyList())
            }
    }
    
    suspend fun createStory(
        mediaUri: Uri,
        mediaType: String,
        caption: String? = null,
        location: StoryLocation? = null
    ): Result<String> {
        val userId = currentUserId ?: return Result.Error(Exception("Usuário não autenticado"))
        val userName = _uiState.value.currentUserName.ifEmpty { "Usuário" }
        val userAvatarUrl = _uiState.value.currentUserAvatarUrl
        
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Fazer upload da mídia
            val uploadResult = feedMediaRepository.uploadStoryMedia(mediaUri, userId, mediaType)
            
            when (uploadResult) {
                is Result.Success -> {
                    val mediaUrl = uploadResult.data
                    val createdAt = Date()
                    val expiresAt = Date(createdAt.time + 24 * 60 * 60 * 1000) // 24 horas
                    
                    val story = Story(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        userName = userName,
                        userAvatarUrl = userAvatarUrl,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType,
                        caption = caption,
                        createdAt = createdAt,
                        expiresAt = expiresAt,
                        location = location
                    )
                    
                    val result = storiesRepository.createStory(story)
                    
                    // Adicionar story localmente à lista para aparecer imediatamente
                    // O observeStories vai sincronizar com o Firestore depois
                    if (result is Result.Success) {
                        val currentStories = _uiState.value.stories
                        val updatedStories = currentStories + story
                        val grouped = updatedStories.groupBy { it.userId }
                        _uiState.value = _uiState.value.copy(
                            stories = updatedStories,
                            userStories = grouped,
                            isLoading = false
                        )
                        android.util.Log.d("StoriesViewModel", "Story adicionado localmente: ${story.id}, Total stories: ${updatedStories.size}")
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    
                    result
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao fazer upload: ${uploadResult.exception.message}"
                    )
                    Result.Error(uploadResult.exception)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    Result.Error(Exception("Resultado desconhecido do upload"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StoriesViewModel", "Erro ao criar story: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Erro ao criar story: ${e.message}"
            )
            Result.Error(e)
        }
    }
    
    fun markStoryAsViewed(storyId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            storiesRepository.markStoryAsViewed(storyId, userId)
        }
    }
    
    fun trackStoryAction(storyId: String, action: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            storiesRepository.trackStoryAction(storyId, userId, action)
        }
    }
    
    fun deleteStory(storyId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = storiesRepository.deleteStory(storyId, userId)
            _uiState.value = _uiState.value.copy(isLoading = false)
            
            when (result) {
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.exception.message)
                }
                else -> {
                    // Story deletada com sucesso
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

