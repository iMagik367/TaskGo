package com.taskgoapp.taskgo.feature.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.taskgoapp.taskgo.core.model.ChatMessage
import com.taskgoapp.taskgo.core.model.MessageThread
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.domain.repository.MessageRepository
import com.taskgoapp.taskgo.core.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatViewModelUiState(
    val isLoading: Boolean = false,
    val thread: MessageThread? = null,
    val messages: List<ChatMessage> = emptyList(),
    val order: OrderFirestore? = null,
    val currentUserRole: String? = null,
    val showAcceptButton: Boolean = false,
    val acceptButtonText: String = "",
    val isAccepting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: com.taskgoapp.taskgo.domain.repository.MessageRepository,
    private val orderRepository: com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository,
    private val userRepository: com.taskgoapp.taskgo.data.repository.FirestoreUserRepository,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatViewModelUiState())
    val uiState: StateFlow<ChatViewModelUiState> = _uiState.asStateFlow()
    
    private val database = FirebaseDatabase.getInstance()
    private var threadListener: ValueEventListener? = null
    
    fun loadThread(threadId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Carregar thread
                val thread = messageRepository.getThread(threadId)
                if (thread == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Thread não encontrada"
                    )
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(thread = thread)
                
                // Obter orderId da thread no Firebase Realtime Database
                // Note: threadsRef aponta para "conversations" no MessageRepositoryImpl
                val conversationsRef = database.reference.child("conversations")
                val threadSnapshot = conversationsRef.child(threadId).get().await()
                val threadData = threadSnapshot.getValue(Map::class.java) as? Map<*, *>
                val orderId = threadData?.get("orderId") as? String
                
                // Carregar ordem se orderId existir
                if (orderId != null) {
                    loadOrderAndUpdateState(orderId)
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
                // Observar mensagens em uma coroutine separada (após inicialização)
                launch {
                    messageRepository.observeMessages(threadId).collect { messages ->
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Erro ao carregar thread: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar conversa: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadOrderAndUpdateState(orderId: String) {
        val order = orderRepository.getOrder(orderId)
        _uiState.value = _uiState.value.copy(order = order)
        
        // Verificar role do usuário atual
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId != null) {
            val user = userRepository.getUser(currentUserId)
            val userRole = user?.role ?: "client"
            _uiState.value = _uiState.value.copy(currentUserRole = userRole)
            
            // Determinar se deve mostrar botão de aceitação
            updateAcceptButtonVisibility(order, userRole, currentUserId)
        }
    }
    
    private fun updateAcceptButtonVisibility(
        order: OrderFirestore?,
        userRole: String,
        currentUserId: String
    ) {
        if (order == null) {
            _uiState.value = _uiState.value.copy(showAcceptButton = false)
            return
        }
        
        // Verificar se a ordem está em um status que permite aceitação
        // Aceitação pode ocorrer quando há proposta (status "proposed") ou quando foi aceita parcialmente (status "accepted")
        // Não pode aceitar se já está em andamento, concluída ou cancelada
        val canAccept = order.status == "proposed" || 
                       (order.status == "accepted" && (!order.acceptedByProvider || !order.acceptedByClient))
        
        if (!canAccept) {
            _uiState.value = _uiState.value.copy(showAcceptButton = false)
            return
        }
        
        // Verificar se já existe proposta (providerId deve estar definido)
        if (order.providerId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(showAcceptButton = false)
            return
        }
        
        // Provider vê botão "Aceitar Serviço"
        if (userRole == "provider" && order.providerId == currentUserId) {
            val alreadyAccepted = order.acceptedByProvider
            _uiState.value = _uiState.value.copy(
                showAcceptButton = !alreadyAccepted,
                acceptButtonText = if (alreadyAccepted) "Serviço Aceito" else "Aceitar Serviço"
            )
            return
        }
        
        // Client vê botão "Aceitar Orçamento"
        if (userRole == "client" && order.clientId == currentUserId) {
            val alreadyAccepted = order.acceptedByClient
            _uiState.value = _uiState.value.copy(
                showAcceptButton = !alreadyAccepted,
                acceptButtonText = if (alreadyAccepted) "Orçamento Aceito" else "Aceitar Orçamento"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(showAcceptButton = false)
    }
    
    fun acceptService() {
        val order = _uiState.value.order ?: return
        val currentUserRole = _uiState.value.currentUserRole ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAccepting = true, error = null)
            
            try {
                val result = if (currentUserRole == "provider") {
                    orderRepository.acceptServiceByProvider(order.id)
                } else {
                    orderRepository.acceptQuoteByClient(order.id)
                }
                
                // Type-safe check: Kotlin smart cast preserva Result.Success<Unit> dentro do bloco
                when (result) {
                    is com.taskgoapp.taskgo.core.model.Result.Success -> {
                        // result é automaticamente Result.Success<Unit> aqui devido ao smart cast
                        // Recarregar ordem para atualizar estado
                        val updatedOrder = orderRepository.getOrder(order.id)
                        _uiState.value = _uiState.value.copy(
                            order = updatedOrder,
                            isAccepting = false
                        )
                        updateAcceptButtonVisibility(
                            updatedOrder,
                            currentUserRole,
                            firebaseAuth.currentUser?.uid ?: ""
                        )
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isAccepting = false,
                            error = result.exception.message ?: "Erro ao aceitar"
                        )
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Loading -> {
                        // Nada a fazer, já está em loading
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Erro ao aceitar: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isAccepting = false,
                    error = "Erro ao aceitar: ${e.message}"
                )
            }
        }
    }
    
    fun sendMessage(text: String) {
        val threadId = _uiState.value.thread?.id ?: return
        
        viewModelScope.launch {
            try {
                messageRepository.sendMessage(threadId, text)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Erro ao enviar mensagem: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao enviar mensagem: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        threadListener?.let {
            val conversationsRef = database.reference.child("conversations")
            conversationsRef.removeEventListener(it)
        }
    }
}

