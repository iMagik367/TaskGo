package com.taskgoapp.taskgo.core.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgoapp.taskgo.core.theme.*

/**
 * Composable que bloqueia funcionalidades até documentos serem cadastrados
 */
@Composable
fun DocumentVerificationBlock(
    isVerified: Boolean,
    onVerifyClick: () -> Unit,
    content: @Composable () -> Unit
) {
    if (isVerified) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TaskGoGreen
                )
                
                Text(
                    text = "Verificação Necessária",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TaskGoTextBlack,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Para criar produtos e serviços, você precisa completar a verificação de identidade enviando seus documentos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onVerifyClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TaskGoGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Verificar Identidade",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

