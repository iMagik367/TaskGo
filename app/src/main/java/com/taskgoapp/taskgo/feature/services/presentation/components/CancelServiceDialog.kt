package com.taskgoapp.taskgo.feature.services.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun CancelServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (reason: String, refundAmount: Double?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var hasRefund by remember { mutableStateOf(false) }
    var refundAmountText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cancelar Serviço",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TaskGoTextDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { 
                        reason = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo do cancelamento *") },
                    placeholder = { Text("Descreva o motivo do cancelamento") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TaskGoGreen,
                        unfocusedBorderColor = TaskGoDivider
                    ),
                    isError = showError && reason.isBlank()
                )
                
                if (showError && reason.isBlank()) {
                    Text(
                        text = "O motivo é obrigatório",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasRefund,
                        onCheckedChange = { hasRefund = it }
                    )
                    Text(
                        text = "Houve reembolso?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TaskGoTextDark
                    )
                }
                
                if (hasRefund) {
                    OutlinedTextField(
                        value = refundAmountText,
                        onValueChange = { 
                            // Permitir apenas números e ponto
                            refundAmountText = it.filter { char -> char.isDigit() || char == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Valor do reembolso (R$)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaskGoGreen,
                            unfocusedBorderColor = TaskGoDivider
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        showError = true
                        return@Button
                    }
                    
                    val refund = if (hasRefund && refundAmountText.isNotBlank()) {
                        refundAmountText.toDoubleOrNull()
                    } else {
                        null
                    }
                    
                    onConfirm(reason, refund)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancelar Serviço", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Voltar", color = TaskGoTextGray)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = TaskGoBackgroundWhite
    )
}

