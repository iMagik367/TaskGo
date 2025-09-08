package com.example.taskgoapp.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgoapp.data.repository.AuthRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            try {
                authRepository.login(email.trim(), password)
                _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
            } catch (t: Throwable) {
                _uiState.value = LoginUiState(isLoading = false, errorMessage = t.message ?: "Falha ao entrar")
            }
        }
    }
}


