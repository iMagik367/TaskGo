package com.example.taskgoapp.feature.moderation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgoapp.core.data.repository.ModerationRepository
import com.example.taskgoapp.core.model.Report
import com.example.taskgoapp.core.model.ReportType
import com.example.taskgoapp.core.model.ReportedContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModerationUiState>(ModerationUiState.Initial)
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()

    private val _myReports = MutableStateFlow<List<Report>>(emptyList())
    val myReports: StateFlow<List<Report>> = _myReports.asStateFlow()

    init {
        loadMyReports()
    }

    fun createReport(type: ReportType, description: String, content: ReportedContent) {
        viewModelScope.launch {
            _uiState.value = ModerationUiState.Loading
            try {
                val response = moderationRepository.createReport(type, description, content)
                if (response.success) {
                    _uiState.value = ModerationUiState.ReportCreated
                    loadMyReports() // Recarregar lista de denúncias
                } else {
                    _uiState.value = ModerationUiState.Error(response.message ?: "Erro desconhecido")
                }
            } catch (e: Exception) {
                _uiState.value = ModerationUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun updateDocument(documentId: String, documentUrl: String) {
        viewModelScope.launch {
            _uiState.value = ModerationUiState.Loading
            try {
                val response = moderationRepository.updateDocument(documentId, documentUrl)
                if (response.success) {
                    _uiState.value = ModerationUiState.DocumentUpdated
                } else {
                    _uiState.value = ModerationUiState.Error(response.message ?: "Erro desconhecido")
                }
            } catch (e: Exception) {
                _uiState.value = ModerationUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _uiState.value = ModerationUiState.Loading
            try {
                val response = moderationRepository.deleteReport(reportId)
                if (response.success) {
                    _uiState.value = ModerationUiState.ReportDeleted
                    loadMyReports() // Recarregar lista de denúncias
                } else {
                    _uiState.value = ModerationUiState.Error(response.message ?: "Erro desconhecido")
                }
            } catch (e: Exception) {
                _uiState.value = ModerationUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    private fun loadMyReports() {
        viewModelScope.launch {
            try {
                val response = moderationRepository.getMyReports()
                if (response.success) {
                    _myReports.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                // Tratar erro silenciosamente ou notificar UI se necessário
            }
        }
    }

    fun resetState() {
        _uiState.value = ModerationUiState.Initial
    }
}

sealed class ModerationUiState {
    object Initial : ModerationUiState()
    object Loading : ModerationUiState()
    object ReportCreated : ModerationUiState()
    object ReportDeleted : ModerationUiState()
    object DocumentUpdated : ModerationUiState()
    data class Error(val message: String) : ModerationUiState()
}