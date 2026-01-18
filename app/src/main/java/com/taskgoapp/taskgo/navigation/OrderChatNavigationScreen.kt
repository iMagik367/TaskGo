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
            val orderRepository = FirestoreOrderRepository(firestore, authRepository)
            val userRepository = FirestoreUserRepository(firestore)
            
            val threadId = messagesViewModel.getOrCreateThreadForOrder(orderId, orderRepository, userRepository)
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

