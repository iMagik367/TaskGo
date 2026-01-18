package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Componente reutilizável para exibir avatar e nome do usuário
 * Carrega automaticamente os dados do usuário pelo userId usando FirestoreUserRepository
 */
@Composable
fun UserAvatarNameLoader(
    userId: String?,
    onUserClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    avatarSize: androidx.compose.ui.unit.Dp = 40.dp,
    showName: Boolean = true
) {
    val userRepository = remember {
        FirestoreUserRepository(com.taskgoapp.taskgo.core.firebase.FirestoreHelper.getInstance())
    }
    
    var userName by remember { mutableStateOf<String?>(null) }
    var userPhotoUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(userId) {
        if (userId != null && userId.isNotBlank()) {
            scope.launch {
                try {
                    var user = userRepository.getUser(userId)
                    // Se não encontrou, tentar novamente após um delay (pode ser que a Cloud Function ainda não tenha criado)
                    if (user == null) {
                        kotlinx.coroutines.delay(1000)
                        user = userRepository.getUser(userId)
                    }
                    userName = user?.displayName
                    userPhotoUrl = user?.photoURL
                    android.util.Log.d("UserAvatarNameLoader", "Usuário carregado: ${user?.displayName}, foto: ${user?.photoURL != null}")
                } catch (e: Exception) {
                    android.util.Log.e("UserAvatarNameLoader", "Erro ao carregar usuário: ${e.message}", e)
                }
            }
        }
    }
    
    UserAvatarName(
        userName = userName,
        userPhotoUrl = userPhotoUrl,
        onUserClick = onUserClick,
        modifier = modifier,
        avatarSize = avatarSize,
        showName = showName
    )
}


