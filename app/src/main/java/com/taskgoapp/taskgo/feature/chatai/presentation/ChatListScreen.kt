package com.taskgoapp.taskgo.feature.chatai.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
import com.taskgoapp.taskgo.core.theme.TaskGoTextGrayLight
import com.taskgoapp.taskgo.feature.chatai.presentation.SearchBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: ChatListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    // Recarregar lista sempre que a tela abre para refletir novas conversas/mensagens
    LaunchedEffect(Unit) {
        viewModel.loadChats()
    }
    
    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    title = "AI TaskGo",
                    onBackClick = onBackClick
                )
                // Campo de busca
                if (uiState.chats.isNotEmpty()) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val chatId = viewModel.createNewChat()
                    onChatClick(chatId)
                },
                containerColor = TaskGoGreen,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Novo chat",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TaskGoBackgroundWhite)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TaskGoGreen)
                    }
                }
                uiState.chats.isEmpty() -> {
                    EmptyChatList(
                        onCreateNewChat = {
                            val chatId = viewModel.createNewChat()
                            onChatClick(chatId)
                        }
                    )
                }
                else -> {
                    if (uiState.filteredChats.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TaskGoTextGrayLight,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Nenhuma conversa encontrada",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TaskGoTextGray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.filteredChats,
                                key = { it.id }
                            ) { chat ->
                                ChatListItem(
                                    chat = chat,
                                    onClick = { onChatClick(chat.id) },
                                    onDelete = { showDeleteDialog = chat.id },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFE5E5E5),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog de confirmação de exclusão
    showDeleteDialog?.let { chatId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Excluir conversa") },
            text = { Text("Tem certeza que deseja excluir esta conversa? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChat(chatId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChatListItem(
    chat: com.taskgoapp.taskgo.feature.chatai.data.ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { 
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    }
    val timeFormat = remember { 
        SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    }
    
    val isToday = remember(chat.timestamp) {
        val today = Calendar.getInstance()
        val chatDate = Calendar.getInstance().apply {
            timeInMillis = chat.timestamp
        }
        today.get(Calendar.YEAR) == chatDate.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == chatDate.get(Calendar.DAY_OF_YEAR)
    }
    
    val displayTime = remember(chat.timestamp, isToday) {
        if (isToday) {
            timeFormat.format(Date(chat.timestamp))
        } else {
            dateFormat.format(Date(chat.timestamp))
        }
    }
    
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circular com ícone de IA
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TaskGoGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = TaskGoGreen,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Conteúdo do chat
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (chat.lastMessage != null) {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Nova conversa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGrayLight,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Timestamp e botão de deletar
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayTime,
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGrayLight
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = TaskGoTextGrayLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyChatList(
    onCreateNewChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TaskGoGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = TaskGoGreen,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Nenhuma conversa ainda",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Comece uma nova conversa com a IA para começar",
            style = MaterialTheme.typography.bodyLarge,
            color = TaskGoTextGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateNewChat,
            colors = ButtonDefaults.buttonColors(
                containerColor = TaskGoGreen
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Nova conversa",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

