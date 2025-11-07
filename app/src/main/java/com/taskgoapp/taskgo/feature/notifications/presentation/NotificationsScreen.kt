package com.taskgoapp.taskgo.feature.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.design.AppTopBar
import com.taskgoapp.taskgo.R

data class NotificationItem(
    val id: Long,
    val title: String,
    val description: String,
    val timestamp: String,
    val icon: String,
    val isRead: Boolean = false
)

data class NotificationSetting(
    val id: Long,
    val title: String,
    val description: String,
    val isEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNavigateToNotificationsSettings: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit,
    variant: String? = null
) {
    var showSettings by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val notifications = remember(variant) {
        val base = listOf(
            NotificationItem(
                id = 1L,
                title = "Pedido Enviado",
                description = "Sua compra de Guarda Roupa foi despachada",
                timestamp = "Hoje, 8:12",
                icon = "📦"
            ),
            NotificationItem(
                id = 2L,
                title = "Proposta Aprovada",
                description = "Você aceitou a proposta de Carlos Montador",
                timestamp = "Ontem, 17:20",
                icon = "✅"
            ),
            NotificationItem(
                id = 3L,
                title = "Atualização Disponível",
                description = "Uma nova versão do aplicativo está disponível para download",
                timestamp = "11 de julho",
                icon = "🔄"
            ),
            NotificationItem(
                id = 4L,
                title = "Ordem de Serviço Publicada",
                description = "Sua ordem Montagem de guarda roupa 2 portas foi enviada",
                timestamp = "10 de julho",
                icon = "📋"
            )
        )
        when (variant) {
            "empty" -> emptyList()
            "unread" -> base.map { it.copy(isRead = false) }
            else -> base
        }
    }

    val notificationSettings = remember {
        mutableStateOf(
            listOf(
                NotificationSetting(
                    id = 1L,
                    title = "Promoções",
                    description = "Receba ofertas e promoções",
                    isEnabled = true
                ),
                NotificationSetting(
                    id = 2L,
                    title = "Som de notificação",
                    description = "Reproduzir som ao receber notificação",
                    isEnabled = false
                ),
                NotificationSetting(
                    id = 3L,
                    title = "Notificação push",
                    description = "Mostrar notificações na tela bloqueada",
                    isEnabled = true
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        AppTopBar(
            title = stringResource(R.string.notifications_title),
            onBackClick = onBackClick,
            backgroundColor = MaterialTheme.colorScheme.primary,
            titleColor = MaterialTheme.colorScheme.onPrimary,
            backIconColor = MaterialTheme.colorScheme.onPrimary,
            actions = {
                IconButton(
                    onClick = { onNavigateToNotificationsSettings() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_notifications_settings),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        )

        if (showSettings || variant == "settings") {
            // Notification Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                notificationSettings.value.forEachIndexed { index, setting ->
                    NotificationSettingItem(
                        setting = setting,
                        onToggle = { isEnabled ->
                            val newSettings = notificationSettings.value.toMutableList()
                            newSettings[index] = setting.copy(isEnabled = isEnabled)
                            notificationSettings.value = newSettings
                        }
                    )
                    if (index < notificationSettings.value.size - 1) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        showSuccessDialog = true
                        showSettings = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.notifications_save_changes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Notifications List
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Sem notificações", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItemCard(
                        notification = notification,
                        onClick = { onNotificationClick(notification) }
                    )
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TaskGo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.notifications_settings_saved),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showSuccessDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun NotificationItemCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.icon,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = notification.timestamp,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Unread indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun NotificationSettingItem(
    setting: NotificationSetting,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Switch
        Switch(
            checked = setting.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = setting.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = setting.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
