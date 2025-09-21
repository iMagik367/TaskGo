package com.example.taskgoapp.feature.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.AppTopBar
import com.example.taskgoapp.core.data.models.ChatMessage
import com.example.taskgoapp.core.data.models.MessageThread
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: Long,
    onNavigateBack: () -> Unit
) {
    // Mock data for now
    val thread = remember { 
        MessageThread(
            id = threadId,
            title = "João Silva",
            preview = "Olá! Como posso ajudar?",
            lastMessageTime = java.time.LocalDateTime.now(),
            unreadCount = 0,
            participants = emptyList()
        )
    }
    val messages = remember { 
        listOf(
            ChatMessage(
                id = 1L,
                threadId = threadId,
                sender = com.example.taskgoapp.core.data.models.User(
                    id = 1L,
                    name = "João Silva",
                    email = "joao@email.com",
                    phone = "11999999999",
                    accountType = com.example.taskgoapp.core.data.models.AccountType.SELLER,
                    rating = 4.8,
                    reviewCount = 156,
                    city = "São Paulo",
                    timeOnTaskGo = "2 anos"
                ),
                content = "Olá! Como posso ajudar?",
                timestamp = java.time.LocalDateTime.now().minusHours(1),
                isMine = false
            ),
            ChatMessage(
                id = 2L,
                threadId = threadId,
                sender = com.example.taskgoapp.core.data.models.User(
                    id = 2L,
                    name = "Você",
                    email = "voce@email.com",
                    phone = "11999999999",
                    accountType = com.example.taskgoapp.core.data.models.AccountType.CLIENT,
                    rating = 0.0,
                    reviewCount = 0,
                    city = "São Paulo",
                    timeOnTaskGo = "1 ano"
                ),
                content = "Preciso de ajuda com o produto",
                timestamp = java.time.LocalDateTime.now(),
                isMine = true
            )
        )
    }
    
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = thread.title,
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            ChatInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        messageText = ""
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }
            
            items(messages) { message ->
                ChatBubble(
                    message = message,
                    isMine = message.isMine
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMine: Boolean
) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    // Balões: usuário = A4FFB6, recebido = D9D9D9
    val backgroundColor = if (isMine)
        Color(0xFFA4FFB6)
    else
        Color(0xFFD9D9D9)
    
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            if (!isMine) {
                // Avatar for other person's messages
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .background(backgroundColor)
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 10
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Time
        Text(
            text = formatMessageTime(message.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.messages_input_placeholder)) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.messages_send),
                    tint = Color.White
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val today = now.toLocalDate()
    val messageDate = timestamp.toLocalDate()
    
    return when {
        messageDate == today -> {
            val formatter = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
            formatter.format(Date.from(timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant()))
        }
        messageDate == today.minusDays(1) -> "Ontem"
        else -> {
            val formatter = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
            formatter.format(Date.from(timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant()))
        }
    }
}
