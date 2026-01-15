package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.*

/**
 * Menu de opções do post (3 pontos) - estilo Instagram
 * Para posts próprios: apenas excluir
 * Para posts de outros: Tenho interesse, Não tenho interesse, Avaliar post, Bloquear usuário
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsMenu(
    isOwnPost: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onInterest: (Boolean) -> Unit,
    onRate: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (isOwnPost) {
                // Menu para posts próprios: apenas excluir
                MenuOptionItem(
                    icon = Icons.Default.Delete,
                    title = "Excluir",
                    description = "Excluir este post permanentemente",
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    textColor = MaterialTheme.colorScheme.error
                )
            } else {
                // Menu para posts de outros usuários
                MenuOptionItem(
                    icon = Icons.Default.ThumbUp,
                    title = "Tenho interesse",
                    description = "Você verá mais posts como esse",
                    onClick = {
                        onInterest(true)
                        onDismiss()
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                MenuOptionItem(
                    icon = Icons.Default.ThumbDown,
                    title = "Não tenho interesse",
                    description = "Você verá menos posts como esse",
                    onClick = {
                        onInterest(false)
                        onDismiss()
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                MenuOptionItem(
                    icon = Icons.Default.Star,
                    title = "Avaliar post",
                    description = "Avaliar este post",
                    onClick = {
                        onRate()
                        onDismiss()
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                MenuOptionItem(
                    icon = Icons.Default.Block,
                    title = "Bloquear",
                    description = "Bloquear ${"usuário"}",
                    onClick = {
                        onBlock()
                        onDismiss()
                    },
                    textColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MenuOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    textColor: Color = TaskGoTextBlack,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                fontSize = 16.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TaskGoTextGray,
                fontSize = 14.sp
            )
        }
    }
}
