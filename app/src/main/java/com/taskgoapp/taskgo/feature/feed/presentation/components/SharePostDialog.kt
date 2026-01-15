package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

/**
 * Dialog para copiar link do post
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostDialog(
    postId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var linkCopied by remember { mutableStateOf(false) }
    
    // Gerar link do post para domínio principal (taskgoapps.com) e deep link do app
    val postLink = "https://taskgoapps.com/post/$postId"
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compartilhar",
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
            
            // Link do post
            OutlinedTextField(
                value = postLink,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TaskGoGreen,
                    unfocusedBorderColor = TaskGoTextGray,
                    disabledTextColor = TaskGoTextBlack,
                    disabledBorderColor = TaskGoTextGray
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            // Botão copiar link
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(postLink))
                    linkCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaskGoGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (linkCopied) "Link copiado!" else "Copiar link",
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (linkCopied) {
                Text(
                    text = "Link copiado para a área de transferência",
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoGreen
                )
            }
        }
    }
}
