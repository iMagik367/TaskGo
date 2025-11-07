package com.taskgoapp.taskgo.feature.checkout.presentation

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
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun FinalizarPedidoScreen(
    onBackClick: () -> Unit,
    onFinalizar: () -> Unit,
    variant: String? = null // null=normal, payment_error, pending, incomplete, success, processing
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when(variant) {
            "payment_error" -> {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TaskGoError, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Erro no pagamento", style = FigmaTitleLarge, color = TaskGoError)
                Text("Houve um problema ao processar o pagamento. Revise seus dados ou tente novamente.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(8.dp))
            }
            "pending" -> {
                Icon(Icons.Default.HourglassEmpty, null, tint = TaskGoWarning, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Pedido em processamento", style = FigmaTitleLarge, color = TaskGoWarning)
                Text("Aguardando confirmação do pagamento.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(8.dp))
            }
            "success" -> {
                Icon(Icons.Default.CheckCircle, null, tint = TaskGoSuccess, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Pedido realizado com sucesso!", style = FigmaTitleLarge, color = TaskGoSuccess)
                Text("Seu pedido está sendo preparado para envio.", style = FigmaProductDescription, color = TaskGoTextGray, modifier = Modifier.padding(8.dp))
            }
            else -> {
                Text("Finalizar Pedido", style = FigmaSectionTitle, color = TaskGoTextBlack)
                Spacer(Modifier.height(12.dp))
                // Dados vêm do Firestore
                Text("Endereço: [Endereço de entrega]", color = TaskGoTextGray, style = FigmaProductDescription)
                Text("Total: R$ 965,00", color = TaskGoPriceGreen, style = FigmaSectionTitle)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth()) { Text("Finalizar Pedido") }
                OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
            }
        }
    }
}
