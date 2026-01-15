package com.taskgoapp.taskgo.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.StoryAnalytics
import com.taskgoapp.taskgo.domain.repository.StoriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryAnalyticsUiState(
    val analytics: StoryAnalytics? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StoryAnalyticsViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StoryAnalyticsUiState())
    val uiState: StateFlow<StoryAnalyticsUiState> = _uiState.asStateFlow()
    
    fun loadAnalytics(storyId: String, ownerUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            storiesRepository.observeStoryAnalytics(storyId, ownerUserId)
                .catch { e ->
                    android.util.Log.e("StoryAnalyticsViewModel", "Erro ao carregar analytics: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar analytics: ${e.message}"
                    )
                }
                .collect { analytics ->
                    _uiState.value = _uiState.value.copy(
                        analytics = analytics,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }
    
    fun trackAction(storyId: String, userId: String, action: String) {
        viewModelScope.launch {
            storiesRepository.trackStoryAction(storyId, userId, action)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
