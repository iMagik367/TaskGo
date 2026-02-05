package com.taskgoapp.taskgo.feature.chatai.presentation

import android.Manifest
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.taskgoapp.taskgo.core.design.*
import com.taskgoapp.taskgo.core.theme.*
import com.taskgoapp.taskgo.feature.chatai.presentation.AttachmentPreview
import com.taskgoapp.taskgo.feature.chatai.presentation.AttachmentSelectionDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AiSupportScreen(
    chatId: String,
    onBackClick: () -> Unit,
    onChatUpdated: ((String, String, Boolean) -> Unit)? = null, // chatId, lastMessage, isFirstMessage
    viewModel: ChatAIViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showVoiceChat by remember { mutableStateOf(false) }
    
    // Inicializar chat quando o chatId mudar
    LaunchedEffect(chatId) {
        viewModel.initializeChat(chatId)
    }
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = "AI TaskGo",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showLanguageMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Traduzir",
                            tint = TaskGoBackgroundWhite
                        )
                    }
                }
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
                items(uiState.messages) { message ->
                    AiMessageBubble(
                        message = message,
                        onTranslate = { viewModel.translateMessage(message, uiState.targetLanguage) }
                    )
                }
                
                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI está digitando...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // Input Area
            EnhancedInputMessage(
                onSend = { text, attachments -> 
                    val isFirstMessage = uiState.messages.isEmpty()
                    viewModel.sendMessage(text, attachments) { title ->
                        // Quando a primeira mensagem for enviada, atualizar o título do chat
                        onChatUpdated?.invoke(chatId, text, isFirstMessage)
                    }
                },
                onMicClick = {
                    if (recordAudioPermission.status is PermissionStatus.Granted) {
                        if (uiState.isRecording) {
                            // Parar gravação e enviar
                            viewModel.stopAudioRecordingAndSend()
                        } else {
                            // Iniciar gravação
                            viewModel.startAudioRecording()
                        }
                    } else {
                        recordAudioPermission.launchPermissionRequest()
                    }
                },
                onVoiceChatClick = {
                    // Abrir tela de conversação por voz
                    showVoiceChat = true
                },
                isRecording = uiState.isRecording,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    
    // Language selection menu
    if (showLanguageMenu) {
        AlertDialog(
            onDismissRequest = { showLanguageMenu = false },
            title = { Text("Selecionar Idioma") },
            text = {
                Column {
                    listOf("pt" to "Português", "en" to "Inglês", "es" to "Espanhol").forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    viewModel.setTargetLanguage(code)
                                    showLanguageMenu = false
                                }
                        ) {
                            RadioButton(
                                selected = uiState.targetLanguage == code,
                                onClick = {
                                    viewModel.setTargetLanguage(code)
                                    showLanguageMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageMenu = false }) {
                    Text("Fechar")
                }
            }
        )
    }
    
    // Voice Chat Screen (conversação por voz)
    if (showVoiceChat) {
        VoiceChatScreen(
            chatId = chatId,
            onDismiss = { showVoiceChat = false }
        )
    }
}

@Composable
fun AiMessageBubble(
    message: AiMessage,
    onTranslate: () -> Unit = {},
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
                
                // Exibir anexos
                if (message.attachments.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(bottom = if (message.text.isNotBlank()) 8.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.attachments.forEach { attachment ->
                            when (attachment.type) {
                                com.taskgoapp.taskgo.feature.chatai.data.AttachmentType.IMAGE -> {
                                    Image(
                                        painter = rememberAsyncImagePainter(attachment.uri),
                                        contentDescription = "Imagem",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                com.taskgoapp.taskgo.feature.chatai.data.AttachmentType.DOCUMENT -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = if (message.isFromAi) 
                                                MaterialTheme.colorScheme.onSurfaceVariant 
                                            else 
                                                MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = attachment.fileName ?: "Documento",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (message.isFromAi) 
                                                MaterialTheme.colorScheme.onSurfaceVariant 
                                            else 
                                                MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.isFromAi) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                if (message.isFromAi) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onTranslate,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Traduzir", style = MaterialTheme.typography.labelSmall)
                    }
                }
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

@Composable
fun EnhancedInputMessage(
    onSend: (String, List<com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment>) -> Unit,
    onMicClick: () -> Unit,
    onVoiceChatClick: (() -> Unit)? = null,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment>>(emptyList()) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Column(modifier = modifier) {
        // Preview de anexos
        if (attachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attachments.forEach { attachment ->
                    AttachmentPreview(
                        attachment = attachment,
                        onRemove = {
                            attachments = attachments.filter { it.id != attachment.id }
                        }
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Botão de microfone (gravação de áudio para texto)
            IconButton(
                onClick = onMicClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Gravar áudio",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Botão de conversação por voz (Speech - igual ChatGPT)
            if (onVoiceChatClick != null) {
                IconButton(
                    onClick = onVoiceChatClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Ícone de speech/voice (conversação por voz)
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Conversação por voz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Botão de anexar
            IconButton(onClick = { showAttachmentMenu = true }) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Anexar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Digite sua mensagem...") },
                maxLines = Int.MAX_VALUE,
                singleLine = false,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = TaskGoBorder,
                    cursorColor = TaskGoGreen
                ),
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = {
                    if (text.isNotBlank() || attachments.isNotEmpty()) {
                        onSend(text, attachments)
                        text = ""
                        attachments = emptyList()
                    }
                },
                containerColor = TaskGoGreen
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
    
    // Menu de seleção de anexos
    if (showAttachmentMenu) {
        AttachmentSelectionDialog(
            onDismiss = { showAttachmentMenu = false },
            onImageSelected = { uri, mimeType ->
                val attachment = com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment(
                    id = System.currentTimeMillis().toString(),
                    uri = uri,
                    type = com.taskgoapp.taskgo.feature.chatai.data.AttachmentType.IMAGE,
                    mimeType = mimeType
                )
                attachments = attachments + attachment
                showAttachmentMenu = false
            },
            onDocumentSelected = { uri, fileName, mimeType ->
                val attachment = com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment(
                    id = System.currentTimeMillis().toString(),
                    uri = uri,
                    type = com.taskgoapp.taskgo.feature.chatai.data.AttachmentType.DOCUMENT,
                    fileName = fileName,
                    mimeType = mimeType
                )
                attachments = attachments + attachment
                showAttachmentMenu = false
            }
        )
    }
}

data class AiMessage(
    val id: Long,
    val text: String,
    val isFromAi: Boolean,
    val timestamp: Long,
    val attachments: List<com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment> = emptyList()
)
