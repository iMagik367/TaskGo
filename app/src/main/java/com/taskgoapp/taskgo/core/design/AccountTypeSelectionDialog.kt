package com.taskgoapp.taskgo.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun AccountTypeSelectionDialog(
    onAccountTypeSelected: (AccountType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAccountType by remember { 
        mutableStateOf(AccountType.CLIENTE) 
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecione o tipo de conta",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Escolha o tipo de conta que melhor descreve você:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TaskGoTextGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccountTypeOption(
                        accountType = AccountType.CLIENTE,
                        title = "Cliente",
                        description = "Contratar serviços e comprar produtos",
                        isSelected = selectedAccountType == AccountType.CLIENTE,
                        onClick = { selectedAccountType = AccountType.CLIENTE }
                    )
                    
                    AccountTypeOption(
                        accountType = AccountType.PARCEIRO,
                        title = "Parceiro",
                        description = "Oferecer serviços e vender produtos",
                        isSelected = selectedAccountType == AccountType.PARCEIRO,
                        onClick = { selectedAccountType = AccountType.PARCEIRO }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TaskGoTextBlack
                        ),
                        border = BorderStroke(1.dp, TaskGoTextGray)
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = { onAccountTypeSelected(selectedAccountType) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaskGoGreen
                        )
                    ) {
                        Text("Continuar", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTypeOption(
    accountType: AccountType,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TaskGoGreen.copy(alpha = 0.1f) else TaskGoSurface
        ),
        border = if (isSelected) BorderStroke(2.dp, TaskGoGreen) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = TaskGoGreen
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TaskGoTextBlack,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskGoTextGray
                )
            }
        }
    }
}

