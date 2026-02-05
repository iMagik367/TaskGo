package com.taskgoapp.taskgo.feature.services.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun DetalhesPropostaScreen(
    propostaId: String,
    onBackClick: () -> Unit,
    onConfirmar: () -> Unit,
    variant: String? = null // null, aceita, recusada, erro, pendente, sucesso
) {
    val statusLabel = when(variant) {
        "aceita" -> "Proposta aceita"
        "sucesso" -> "Sucesso! Proposta aceita."
        "recusada" -> "Proposta recusada"
        "pendente" -> "Aguardando resposta"
        "erro" -> "Erro ao processar"
        else -> "Detalhes da proposta"
    }
    val statusColor = when(variant) {
        "aceita","sucesso" -> TaskGoSuccess
        "recusada" -> TaskGoError
        "pendente" -> TaskGoWarning
        "erro" -> TaskGoError
        else -> TaskGoPrimary
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(statusLabel, style = FigmaSectionTitle, color = statusColor)
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TaskGoBackgroundWhite
            ),
            border = BorderStroke(1.dp, TaskGoBorder)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Serviço: [Dados do serviço]", color=TaskGoTextBlack)
                Text("Valor: [Valor da proposta]", color=TaskGoTextBlack)
                Text("Proposta enviada por: [Nome do prestador]", color=TaskGoTextGray)
                if(variant=="recusada"||variant=="erro") {
                    Spacer(Modifier.height(10.dp))
                    Text("Motivo: Proposta recusada pelo cliente.", color=TaskGoError)
                }
                if(variant=="pendente") {
                    Spacer(Modifier.height(10.dp))
                    Text("Aguardando confirmação do cliente.", color=TaskGoWarning)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        if(variant==null||variant=="pendente"){
            Button(onClick=onConfirmar) { Text("Aceitar proposta") }
        }
        OutlinedButton(onClick=onBackClick) { Text("Voltar") }
    }
}
