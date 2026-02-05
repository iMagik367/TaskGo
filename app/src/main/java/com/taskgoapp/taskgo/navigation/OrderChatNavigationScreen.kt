package com.taskgoapp.taskgo.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.feature.messages.presentation.MessagesViewModel

@Composable
fun OrderChatNavigationScreen(
    orderId: String,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val messagesViewModel: MessagesViewModel = hiltViewModel()
    
    LaunchedEffect(orderId) {
        try {
            // Instanciar repositories diretamente
            val firestore = com.taskgoapp.taskgo.core.firebase.FirestoreHelper.getInstance()
            val authRepository = FirebaseAuthRepository(com.google.firebase.auth.FirebaseAuth.getInstance())
            val firestoreUserRepository = FirestoreUserRepository(firestore)
            
            // FirestoreOrderRepository precisa de UserRepository, mas getOrCreateThreadForOrder precisa de FirestoreUserRepository
            // Vamos usar UserRepositoryImpl via Hilt - mas como não temos acesso direto, vamos criar um wrapper simples
            val authUser = authRepository.getCurrentUser()
            val userRepositoryWrapper = object : com.taskgoapp.taskgo.domain.repository.UserRepository {
                override fun observeCurrentUser() = kotlinx.coroutines.flow.flow {
                    val uid = authUser?.uid
                    if (uid != null) {
                        firestoreUserRepository.observeUser(uid).collect { userFirestore ->
                            if (userFirestore != null) {
                                emit(with(com.taskgoapp.taskgo.data.mapper.UserMapper) { userFirestore.toModel() })
                            } else {
                                emit(null)
                            }
                        }
                    } else {
                        emit(null)
                    }
                }
                override suspend fun updateUser(user: com.taskgoapp.taskgo.core.model.UserProfile) {
                    // Converter UserProfile para UserFirestore manualmente
                    val userFirestore = com.taskgoapp.taskgo.data.firestore.models.UserFirestore(
                        uid = user.id,
                        email = user.email,
                        displayName = user.name,
                        photoURL = user.avatarUri,
                        phone = user.phone,
                        city = user.city,
                        state = user.state,
                        role = user.accountType?.name ?: "client",
                        createdAt = null,
                        updatedAt = null
                    )
                    firestoreUserRepository.updateUser(userFirestore).getOrThrow()
                }
                override suspend fun updateAvatar(avatarUri: String) {
                    android.util.Log.w("OrderChatNavigation", "updateAvatar não implementado")
                }
            }
            
            val orderRepository = FirestoreOrderRepository(firestore, authRepository, userRepositoryWrapper)
            val threadId = messagesViewModel.getOrCreateThreadForOrder(orderId, orderRepository, firestoreUserRepository)
            onNavigateToChat(threadId)
        } catch (exception: Exception) {
            android.util.Log.e("OrderChatNavigation", "Erro ao obter/criar thread: ${exception.message}", exception)
            onNavigateBack()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

