package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun ConfirmarPropostaScreen(
    onBackClick: () -> Unit,
    onConfirmar: () -> Unit,
    variant: String? = null // null, aceita, recusada, erro, sucesso, pendente
) {
    val statusLabel = when(variant) {
        "aceita" -> "Proposta aceita!"
        "recusada" -> "Proposta recusada"
        "erro" -> "Erro ao processar"
        "sucesso" -> "Ação concluída"
        "pendente" -> "Aguardando confirmação"
        else -> "Confirmar proposta"
    }
    val statusColor = when(variant) {
        "aceita","sucesso" -> TaskGoSuccess
        "recusada" -> TaskGoError
        "pendente" -> TaskGoWarning
        "erro" -> TaskGoError
        else -> TaskGoPrimary
    }
    val statusIcon = when(variant) {
        "aceita","sucesso" -> Icons.Default.CheckCircle
        "recusada" -> Icons.Default.ErrorOutline
        "pendente" -> Icons.Default.HourglassEmpty
        "erro" -> Icons.Default.ErrorOutline
        else -> null
    }
        Column(
        Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        statusIcon?.let { icon ->
            Icon(icon, null, tint=statusColor, modifier=Modifier.size(60.dp))
            Spacer(Modifier.height(10.dp))
        }
        Text(statusLabel, style=FigmaSectionTitle, color=statusColor)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Serviço: [Dados do serviço]", color=TaskGoTextBlack, style=FigmaProductName)
                Text("Proposta: [Valor da proposta]", color=TaskGoTextBlack, style=FigmaPrice)
                Text("Cliente: [Nome do cliente]", color=TaskGoTextGray, style=FigmaProductDescription)
            }
        }
        Spacer(Modifier.height(24.dp))
        if(variant==null) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick=onConfirmar,
                    colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                ) {
                    Text("Aceitar proposta", style=FigmaButtonText, color=TaskGoBackgroundWhite) 
                }
                OutlinedButton(
                    onClick=onBackClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, TaskGoGreen)
                ) { 
                    Text("Recusar", style=FigmaButtonText, color=TaskGoGreen) 
                    }
                }
        } else {
            OutlinedButton(
                onClick=onBackClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(1.dp, TaskGoGreen)
            ) {
                Text("Voltar", style=FigmaButtonText, color=TaskGoGreen) 
            }
        }
    }
}
