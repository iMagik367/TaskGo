package com.taskgoapp.taskgo.feature.chatai.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.design.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSupportScreen(
    onBackClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<AiMessage>()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Sem mensagens mockadas iniciais
    
    fun sendMessage() {
        if (messageText.isBlank()) return
        
        val userMessage = AiMessage(
            id = messages.size + 1L,
            text = messageText,
            isFromAi = false,
            timestamp = System.currentTimeMillis()
        )
        
        messages = messages + userMessage
        messageText = ""
        
        // TODO: Integrar com backend/serviço real de IA se aplicável
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "AI TaskGo",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    AiMessageBubble(message = message)
                }
            }
            
            // Input Area
            InputMessage(
                message = messageText,
                onMessageChange = { messageText = it },
                onSend = { sendMessage() },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun AiMessageBubble(
    message: AiMessage,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale("pt", "BR")) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromAi) Alignment.Start else Alignment.End
    ) {
        Surface(
            color = if (message.isFromAi) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isFromAi) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI TaskGo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromAi) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = dateFormat.format(Date(message.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

fun generateAiResponse(userMessage: String): AiMessage {
    val responses = listOf(
        "Entendo sua dúvida! Deixe-me ajudar você com isso.",
        "Ótima pergunta! Aqui está a resposta que você precisa:",
        "Vou te explicar como funciona:",
        "Para resolver isso, você pode seguir estes passos:",
        "Essa é uma dúvida comum. Aqui está a solução:",
        "Perfeito! Deixe-me te orientar sobre isso.",
        "Vou te dar algumas dicas importantes:",
        "Essa é uma excelente pergunta! Aqui está a resposta:"
    )
    
    val randomResponse = responses.random()
    
    return AiMessage(
        id = System.currentTimeMillis(),
        text = randomResponse,
        isFromAi = true,
        timestamp = System.currentTimeMillis()
    )
}

data class AiMessage(
    val id: Long,
    val text: String,
    val isFromAi: Boolean,
    val timestamp: Long
)
