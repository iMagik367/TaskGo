package com.taskgoapp.taskgo.feature.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.taskgoapp.taskgo.core.design.TGIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taskgoapp.taskgo.R
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.core.design.PrimaryButton
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.MessageThread
import com.taskgoapp.taskgo.core.theme.FigmaButtonText
import com.taskgoapp.taskgo.core.theme.FigmaProductDescription
import com.taskgoapp.taskgo.core.theme.FigmaProductName
import com.taskgoapp.taskgo.core.theme.FigmaSectionTitle
import com.taskgoapp.taskgo.core.theme.FigmaStatusText
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundGray
import com.taskgoapp.taskgo.core.theme.TaskGoBackgroundWhite
import com.taskgoapp.taskgo.core.theme.TaskGoBorder
import com.taskgoapp.taskgo.core.theme.TaskGoGreen
import com.taskgoapp.taskgo.core.theme.TaskGoTextBlack
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray
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
    onNavigateToCart: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToServiceOrders: () -> Unit,
    variant: String? = null
) {
    // Dados vêm do backend via ViewModel
    val viewModel: MessagesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val threads by viewModel.threads.collectAsState()
    val accountType by viewModel.accountType.collectAsState()
    
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.messages_title),
                onBackClick = null // Sem botão de voltar
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            if (uiState.isLoading || variant == "loading") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Erro ao carregar mensagens",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (threads.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(threads) { thread ->
                        MessageThreadCard(
                            thread = thread,
                            onClick = { onNavigateToChat(thread.id.toLongOrNull() ?: 0L) }
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
                            tint = TaskGoTextGray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(R.string.messages_empty_title),
                            style = FigmaSectionTitle,
                            color = TaskGoTextBlack
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.messages_empty_message),
                            style = FigmaProductDescription,
                            color = TaskGoTextGray,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Botão condicional baseado no tipo de conta
                        when (accountType) {
                            AccountType.PARCEIRO,
                            AccountType.PARCEIRO -> {
                                PrimaryButton(
                                    text = stringResource(R.string.messages_find_service_orders),
                                    onClick = onNavigateToServiceOrders
                                )
                            }
                            AccountType.CLIENTE -> {
                        PrimaryButton(
                                    text = stringResource(R.string.messages_find_providers),
                                    onClick = onNavigateToProviders
                        )
                            }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = TaskGoBackgroundWhite
        ),
        border = BorderStroke(1.dp, TaskGoBorder)
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
                    .background(TaskGoBackgroundGray),
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
                            tint = TaskGoTextBlack
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
                        style = FigmaProductName,
                        color = TaskGoTextBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatTime(thread.lastTime),
                        style = FigmaStatusText,
                        color = TaskGoTextGray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = thread.lastMessage,
                    style = FigmaProductDescription,
                    color = TaskGoTextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
