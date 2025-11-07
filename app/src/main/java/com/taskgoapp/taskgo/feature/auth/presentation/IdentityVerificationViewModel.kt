package com.taskgoapp.taskgo.feature.auth.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.repository.FirebaseStorageRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class IdentityVerificationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val documentFrontUri: Uri? = null,
    val documentBackUri: Uri? = null,
    val selfieUri: Uri? = null,
    val addressProofUri: Uri? = null,
    val documentFrontUrl: String? = null,
    val documentBackUrl: String? = null,
    val selfieUrl: String? = null,
    val addressProofUrl: String? = null
)

@HiltViewModel
class IdentityVerificationViewModel @Inject constructor(
    private val storageRepository: FirebaseStorageRepository,
    private val firestoreRepository: FirestoreUserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IdentityVerificationUiState())
    val uiState: StateFlow<IdentityVerificationUiState> = _uiState
    
    fun setDocumentFront(uri: Uri) {
        _uiState.value = _uiState.value.copy(documentFrontUri = uri)
    }
    
    fun setDocumentBack(uri: Uri) {
        _uiState.value = _uiState.value.copy(documentBackUri = uri)
    }
    
    fun setSelfie(uri: Uri) {
        _uiState.value = _uiState.value.copy(selfieUri = uri)
    }
    
    fun setAddressProof(uri: Uri) {
        _uiState.value = _uiState.value.copy(addressProofUri = uri)
    }
    
    fun submitVerification() {
        val currentUser = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Usuário não autenticado"
            )
            return
        }
        
        val state = _uiState.value
        if (state.documentFrontUri == null || state.documentBackUri == null || state.selfieUri == null) {
            _uiState.value = state.copy(
                errorMessage = "Por favor, envie todos os documentos necessários"
            )
            return
        }
        
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                // Upload documentos
                val documentFrontResult = storageRepository.uploadDocument(
                    currentUser.uid,
                    "front",
                    state.documentFrontUri!!
                )
                
                val documentBackResult = storageRepository.uploadDocument(
                    currentUser.uid,
                    "back",
                    state.documentBackUri!!
                )
                
                val selfieResult = storageRepository.uploadSelfie(
                    currentUser.uid,
                    state.selfieUri!!
                )
                
                val addressProofResult = state.addressProofUri?.let {
                    storageRepository.uploadAddressProof(currentUser.uid, it)
                }
                
                // Verificar se todos os uploads foram bem-sucedidos
                val documentFrontUrl = documentFrontResult.getOrNull()
                val documentBackUrl = documentBackResult.getOrNull()
                val selfieUrl = selfieResult.getOrNull()
                val addressProofUrl = addressProofResult?.getOrNull()
                
                if (documentFrontUrl == null || documentBackUrl == null || selfieUrl == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Erro ao fazer upload dos documentos. Tente novamente."
                    )
                    return@launch
                }
                
                // Atualizar Firestore
                val user = firestoreRepository.getUser(currentUser.uid)
                val updatedUser = user?.copy(
                    documentFront = documentFrontUrl,
                    documentBack = documentBackUrl,
                    selfie = selfieUrl,
                    addressProof = addressProofUrl,
                    updatedAt = Date()
                ) ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Usuário não encontrado"
                    )
                    return@launch
                }
                
                firestoreRepository.updateUser(updatedUser).fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            documentFrontUrl = documentFrontUrl,
                            documentBackUrl = documentBackUrl,
                            selfieUrl = selfieUrl,
                            addressProofUrl = addressProofUrl
                        )
                        Log.d("IdentityVerificationViewModel", "Verificação enviada com sucesso")
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Erro ao salvar documentos: ${exception.message}"
                        )
                        Log.e("IdentityVerificationViewModel", "Erro ao salvar documentos", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}"
                )
                Log.e("IdentityVerificationViewModel", "Erro inesperado", e)
            }
        }
    }
}


