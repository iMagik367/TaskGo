package br.com.taskgo.taskgo.feature.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.example.taskgoapp.core.design.TGIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskgoapp.R
import com.example.taskgoapp.core.design.AppTopBar
import com.example.taskgoapp.core.data.models.MessageThread
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onBackClick: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    onNavigateToCreateWorkOrder: () -> Unit,
    onNavigateToProposals: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    val threads = remember { 
        listOf(
            MessageThread(
                id = 1L,
                title = "João Silva",
                preview = "Olá! Como posso ajudar?",
                lastMessageTime = java.time.LocalDateTime.now().minusHours(2),
                unreadCount = 1,
                participants = emptyList()
            ),
            MessageThread(
                id = 2L,
                title = "Maria Santos",
                preview = "Seu produto foi enviado",
                lastMessageTime = java.time.LocalDateTime.now().minusDays(1),
                unreadCount = 0,
                participants = emptyList()
            )
        )
    }
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.messages_title),
                onBackClick = null, // Removido botão de voltar
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Bell),
                            contentDescription = stringResource(R.string.notifications_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Cart),
                            contentDescription = stringResource(R.string.ui_cart),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToChat(0L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Messages),
                            contentDescription = stringResource(R.string.messages_title),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
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
            
            if (threads.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(threads) { thread ->
                        MessageThreadCard(
                            thread = thread,
                            onClick = { onNavigateToChat(thread.id) }
                        )
                    }
                }
            } else {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(TGIcons.Messages),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(R.string.messages_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.messages_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onNavigateToCreateWorkOrder
                        ) {
                            Icon(
                                painter = painterResource(TGIcons.Add),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.messages_create_order))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageThreadCard(
    thread: MessageThread,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                    painter = painterResource(when (thread.title) {
                        "Ordem de serviço" -> TGIcons.Edit
                        "Compra de Furadeira" -> TGIcons.Cart
                        "Serviço de Montagem" -> TGIcons.Services
                        else -> TGIcons.Profile
                    }),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Thread Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatTime(thread.lastMessageTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = thread.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(TGIcons.Profile),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = thread.participants.firstOrNull()?.name ?: "Usuário",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (thread.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                                text = thread.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Agora"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        else -> {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
            formatter.format(date)
        }
    }
}
