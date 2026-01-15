package com.taskgoapp.taskgo.feature.feed.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.taskgoapp.taskgo.core.theme.*

/**
 * Dialog para avaliar um post (1-5 estrelas)
 */
@Composable
fun RatePostDialog(
    onDismiss: () -> Unit,
    onConfirm: (rating: Int, comment: String?) -> Unit,
    existingRating: Int? = null,
    existingComment: String? = null
) {
    var selectedRating by remember { mutableStateOf(existingRating ?: 0) }
    var comment by remember { mutableStateOf(existingComment ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Avaliar Post",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Estrelas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "$i estrelas",
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedRating = i },
                            tint = if (i <= selectedRating) Color(0xFFFFD700) else Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Campo de comentário (opcional)
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentário (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    placeholder = { Text("Deixe um comentário sobre este post...") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            if (selectedRating > 0) {
                                onConfirm(selectedRating, comment.takeIf { it.isNotBlank() })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedRating > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Text("Avaliar")
                    }
                }
            }
        }
    }
}
