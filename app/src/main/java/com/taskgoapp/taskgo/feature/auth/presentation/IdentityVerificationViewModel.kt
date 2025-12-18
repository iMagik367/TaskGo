package com.taskgoapp.taskgo.feature.auth.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.security.FaceVerificationManager
import com.taskgoapp.taskgo.data.repository.FirebaseStorageRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val addressProofUrl: String? = null,
    val faceVerificationResult: String? = null,
    val isVerifyingFace: Boolean = false
)

@HiltViewModel
class IdentityVerificationViewModel @Inject constructor(
    private val storageRepository: FirebaseStorageRepository,
    private val firestoreRepository: FirestoreUserRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IdentityVerificationUiState())
    val uiState: StateFlow<IdentityVerificationUiState> = _uiState
    
    private val faceVerificationManager = FaceVerificationManager(context)
    
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
    
    /**
     * Verifica se a selfie corresponde ao documento usando validação facial
     */
    suspend fun verifyFaceMatch(): Boolean {
        val state = _uiState.value
        // Garantir que temos URI do documento. Se não, tentar baixar pela URL salva no Firestore
        val docUri = state.documentFrontUri ?: run {
            val url = state.documentFrontUrl ?: tryFetchUserDocumentFrontUrl() ?: return false
            val downloaded = tryDownloadToCache(url) ?: return false
            _uiState.value = _uiState.value.copy(documentFrontUri = downloaded)
            downloaded
        }
        val selfie = state.selfieUri ?: return false

        return try {
            _uiState.value = state.copy(isVerifyingFace = true, faceVerificationResult = null)
            
            val result = faceVerificationManager.compareFaces(
                selfieUri = selfie,
                documentUri = docUri
            )
            
            _uiState.value = state.copy(
                isVerifyingFace = false,
                faceVerificationResult = result.message
            )
            
            result.success
        } catch (e: Exception) {
            Log.e("IdentityVerificationViewModel", "Erro na verificação facial: ${e.message}", e)
            _uiState.value = state.copy(
                isVerifyingFace = false,
                faceVerificationResult = "Erro na verificação facial: ${e.message}"
            )
            false
        }
    }

    private suspend fun tryFetchUserDocumentFrontUrl(): String? {
        val currentUser = auth.currentUser ?: return null
        return try {
            val user = firestoreRepository.getUser(currentUser.uid)
            user?.documentFront
        } catch (e: Exception) {
            Log.e("IdentityVerificationViewModel", "Erro ao buscar URL do documento: ${e.message}", e)
            null
        }
    }

    private fun tryDownloadToCache(url: String): Uri? {
        return try {
            val input = java.net.URL(url).openStream()
            val file = java.io.File(context.cacheDir, "doc_front_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> input.copyTo(out) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("IdentityVerificationViewModel", "Erro ao baixar documento: ${e.message}", e)
            null
        }
    }

    fun markIdentityVerified(onComplete: (Boolean) -> Unit = {}) {
        val currentUser = auth.currentUser ?: return onComplete(false)
        viewModelScope.launch {
            try {
                val user = firestoreRepository.getUser(currentUser.uid)
                if (user == null) {
                    onComplete(false); return@launch
                }
                val updated = user.copy(
                    verified = true,
                    updatedAt = Date()
                )
                firestoreRepository.updateUser(updated).fold(
                    onSuccess = {
                        onComplete(true)
                    },
                    onFailure = {
                        onComplete(false)
                    }
                )
            } catch (e: Exception) {
                Log.e("IdentityVerificationViewModel", "Erro ao marcar verificado: ${e.message}", e)
                onComplete(false)
            }
        }
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
                // Verificar correspondência facial antes de fazer upload
                val faceMatch = verifyFaceMatch()
                if (!faceMatch) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "A selfie não corresponde ao documento. Por favor, tire uma nova selfie segurando seu documento."
                    )
                    return@launch
                }
                
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
    
    override fun onCleared() {
        super.onCleared()
        faceVerificationManager.release()
    }
}


