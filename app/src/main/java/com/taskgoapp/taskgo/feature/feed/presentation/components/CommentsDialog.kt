package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.feature.feed.presentation.FeedViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog de comentários estilo Instagram com suporte completo a teclado
 * Layout ajusta automaticamente quando o teclado abre
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsDialog(
    postId: String,
    postUserId: String,
    currentUserId: String?,
    onDismiss: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    var isCreatingComment by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    
    // Observar comentários em tempo real com tratamento de erro
    val comments by remember(postId) {
        viewModel.observePostComments(postId)
            .catch { e ->
                android.util.Log.e("CommentsDialog", "Erro ao observar comentários: ${e.message}", e)
                emptyList<CommentItem>()
            }
    }.collectAsState(initial = emptyList())
    
    // Handler para criar comentário com feedback completo
    val createCommentHandler = rememberCreateCommentHandler(
        viewModel = viewModel,
        postId = postId,
        comments = comments,
        onStart = {
            isCreatingComment = true
            errorMessage = null
        },
        onSuccess = {
            commentText = ""
            isCreatingComment = false
            keyboardController?.hide()
            // Scroll para o último comentário após um delay
            scope.launch {
                kotlinx.coroutines.delay(500) // Aguardar comentário aparecer no Firestore
                if (comments.isNotEmpty()) {
                    try {
                        listState.animateScrollToItem(comments.size - 1)
                    } catch (e: Exception) {
                        // Ignorar erro de scroll se a lista mudou
                    }
                }
            }
        },
        onError = { error ->
            isCreatingComment = false
            errorMessage = error
        }
    )
    
    // Scroll automático para o último comentário quando novos comentários são adicionados
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty() && !isCreatingComment) {
            scope.launch {
                try {
                    listState.animateScrollToItem(comments.size - 1)
                } catch (e: Exception) {
                    // Ignorar erro de scroll
                }
            }
        }
    }
    
    // Sheet state para controlar a altura e ajuste ao teclado
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    LaunchedEffect(Unit) {
        // Expandir completamente e focar no input ao abrir
        sheetState.expand()
        focusRequester.requestFocus()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header (fixo no topo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comentários",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = TaskGoTextGray
                    )
                }
            }
            
            HorizontalDivider()
            
            // Lista de comentários (ajusta quando teclado abre)
            if (comments.isEmpty() && !isCreatingComment) {
                // Estado vazio (estilo Instagram)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp)
                    ) {
                        // Ícone de comentários
                        Icon(
                            imageVector = Icons.Default.ModeComment,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TaskGoTextGray.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Bora começar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TaskGoTextBlack
                        )
                        Text(
                            text = "Deixe um comentário para que outras pessoas também participem.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TaskGoTextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            isOwnComment = comment.userId == currentUserId
                        )
                    }
                    
                    // Indicador de loading ao criar comentário
                    if (isCreatingComment) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = TaskGoGreen,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Enviando comentário...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TaskGoTextGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Mensagem de erro
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            HorizontalDivider()
            
            // Input de comentário (fixo na parte inferior, ajusta quando teclado abre)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar do usuário (se disponível)
                currentUserId?.let {
                    AsyncImage(
                        model = null, // TODO: Buscar avatar do usuário atual
                        contentDescription = "Seu avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { 
                        if (it.length <= 500) {
                            commentText = it
                            errorMessage = null // Limpar erro ao digitar
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Comente como Você",
                            color = TaskGoTextGray
                        )
                    },
                    maxLines = 3,
                    singleLine = false,
                    enabled = !isCreatingComment,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank() && !isCreatingComment) {
                                createCommentHandler(commentText.trim())
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoTextGray.copy(alpha = 0.5f),
                        cursorColor = TaskGoGreen,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank() && !isCreatingComment) {
                            createCommentHandler(commentText.trim())
                        }
                    },
                    enabled = commentText.isNotBlank() && !isCreatingComment,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (commentText.isNotBlank() && !isCreatingComment) TaskGoGreen else TaskGoTextGray.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    if (isCreatingComment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar comentário",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Função auxiliar para criar comentário com tratamento completo de erros
 */
@Composable
private fun rememberCreateCommentHandler(
    viewModel: FeedViewModel,
    postId: String,
    comments: List<CommentItem>,
    onStart: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
): (String) -> Unit {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingCommentText by remember { mutableStateOf<String?>(null) }
    val currentUserIdFromViewModel = viewModel.currentUserId
    
    // Observar mudanças no estado de erro
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            if (error.contains("comentário", ignoreCase = true) && pendingCommentText != null) {
                onError(error)
                pendingCommentText = null
            }
        }
    }
    
    // Observar quando novo comentário aparece na lista
    LaunchedEffect(comments) {
        if (pendingCommentText != null && currentUserIdFromViewModel != null) {
            // Verificar se o comentário apareceu na lista
            // Procurar por comentário com mesmo texto e mesmo userId
            val matchingComment = comments.find { 
                it.text == pendingCommentText && 
                it.userId == currentUserIdFromViewModel
            }
            
            if (matchingComment != null) {
                android.util.Log.d("CommentsDialog", "Comentário detectado na lista: ${matchingComment.id}")
                onSuccess()
                pendingCommentText = null
            }
        }
    }
    
    return { text ->
        onStart()
        pendingCommentText = text
        
        scope.launch {
            try {
                android.util.Log.d("CommentsDialog", "Criando comentário: $text")
                
                // Criar comentário aguardando resultado imediato
                when (val result = viewModel.createCommentAwait(postId, text)) {
                    is com.taskgoapp.taskgo.core.model.Result.Success -> {
                        // Feedback otimista; sucesso final confirmado ao aparecer no listener
                        onSuccess()
                        pendingCommentText = null
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Error -> {
                        val msg = result.exception.message ?: "Erro ao criar comentário"
                        android.util.Log.e("CommentsDialog", "Erro ao criar comentário: $msg", result.exception)
                        onError(msg)
                        pendingCommentText = null
                    }
                    else -> {
                        // Loading não deve ocorrer aqui; tratar como erro genérico
                        onError("Erro ao criar comentário. Tente novamente.")
                        pendingCommentText = null
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CommentsDialog", "Erro ao criar comentário: ${e.message}", e)
                onError("Erro ao criar comentário. Tente novamente.")
                pendingCommentText = null
            }
        }
    }
}

/**
 * Item de comentário individual
 */
@Composable
private fun CommentItem(
    comment: CommentItem,
    isOwnComment: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        AsyncImage(
            model = comment.userAvatarUrl ?: "",
            contentDescription = comment.userName,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Column(modifier = Modifier.weight(1f)) {
            // Nome e texto do comentário
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack,
                    fontSize = 14.sp
                )
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextBlack,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Timestamp e ações
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCommentDate(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray,
                    fontSize = 12.sp
                )
                // Botão de curtir comentário (opcional)
                TextButton(
                    onClick = { /* TODO: Implementar curtir comentário */ },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text = if (comment.isLiked) "Descurtir" else "Curtir",
                        style = MaterialTheme.typography.bodySmall,
                        color = TaskGoTextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Data model para comentário
 */
data class CommentItem(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val text: String,
    val createdAt: Date,
    val isLiked: Boolean = false,
    val likesCount: Int = 0
)

private fun formatCommentDate(date: Date?): String {
    if (date == null) return ""
    
    val now = Date()
    val diff = now.time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "agora"
        minutes < 60 -> "há ${minutes}min"
        hours < 24 -> "há ${hours}h"
        days < 7 -> "há ${days}d"
        else -> {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            format.format(date)
        }
    }
}
