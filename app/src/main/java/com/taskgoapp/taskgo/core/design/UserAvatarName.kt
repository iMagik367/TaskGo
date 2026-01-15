package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.taskgoapp.taskgo.core.theme.TaskGoTextGray

/**
 * Componente reutilizável para exibir avatar e nome do usuário
 * Recebe userName e userPhotoUrl como parâmetros (carregamento deve ser feito pelo caller)
 */
@Composable
fun UserAvatarName(
    userName: String? = null,
    userPhotoUrl: String? = null,
    onUserClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    avatarSize: androidx.compose.ui.unit.Dp = 40.dp,
    showName: Boolean = true
) {
    Row(
        modifier = modifier
            .then(if (onUserClick != null) Modifier.clickable { onUserClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar
        if (!userPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(userPhotoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar do usuário",
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar do usuário",
                    modifier = Modifier.size(avatarSize * 0.6f),
                    tint = Color.Gray
                )
            }
        }
        
        // Nome (se habilitado)
        if (showName && userName != null) {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TaskGoTextBlack,
                maxLines = 1
            )
        }
    }
}


