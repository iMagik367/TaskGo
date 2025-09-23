package br.com.taskgo.feature.auth.presentation

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.taskgo.core.data.repository.auth.AuthRepository
import br.com.taskgo.core.data.repository.auth.SignInResult
import br.com.taskgo.core.data.repository.auth.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage,
            userData = result.data
        ) }
    }

    fun resetState() {
        _state.update { AuthState() }
    }

    fun signInWithGoogle(launch: (IntentSender) -> Unit) {
        viewModelScope.launch {
            val signInIntentSender = repository.signInWithGoogle()
            signInIntentSender?.let(launch)
        }
    }

    fun signInWithFacebook() {
        viewModelScope.launch {
            repository.signInWithFacebook().collect { result ->
                onSignInResult(result)
            }
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.signInWithEmailPassword(email, password)
            onSignInResult(result)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _state.update { AuthState() }
        }
    }

    fun handleGoogleSignInResult(intent: Intent) {
        val signInResult = repository.getGoogleSignInResultFromIntent(intent)
        onSignInResult(signInResult)
    }
}

data class AuthState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val userData: UserData? = null
)