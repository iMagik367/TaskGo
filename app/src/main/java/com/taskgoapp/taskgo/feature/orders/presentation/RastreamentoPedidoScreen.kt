package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun RastreamentoPedidoScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onVerDetalhes: (String) -> Unit,
    variant: String? = null // null=transito, 'delivered', 'delayed', 'canceled'
) {
    val eventosTransito = listOf(
        "Pedido recebido",
        "Pagamento aprovado",
        "Pedido despachado",
        "A caminho do endereço"
    )
    val eventosEntregue = eventosTransito + "Pedido entregue"
    val eventosAtrasado = eventosTransito + "Entrega em atraso"
    val eventosCancelado = eventosTransito.take(2) + "Pedido cancelado"
    val eventos = when(variant) {
        "delivered" -> eventosEntregue
        "delayed" -> eventosAtrasado
        "canceled" -> eventosCancelado
        else -> eventosTransito
    }
    val icone = when(variant) {
        "delivered" -> Icons.Default.CheckCircle
        "delayed" -> Icons.Default.ErrorOutline
        "canceled" -> Icons.Default.Cancel
        else -> Icons.Default.LocalShipping
    }
    val cor = when(variant) {
        "delivered" -> TaskGoSuccess
        "delayed" -> TaskGoWarning
        "canceled" -> TaskGoError
        else -> TaskGoPrimary
    }
    val status = when(variant) {
        "delivered" -> "Entregue"
        "delayed" -> "Entrega em atraso"
        "canceled" -> "Pedido cancelado"
        else -> "Em trânsito"
    }
    Column(
            modifier = Modifier
                .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icone, contentDescription = status, tint = cor, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Rastreamento do Pedido", style = FigmaSectionTitle, color = TaskGoTextBlack)
        Text("Pedido #$orderId", style = FigmaProductDescription, color = TaskGoTextGray)
        Spacer(modifier = Modifier.height(20.dp))
        Text("Status: $status", style = FigmaSectionTitle, color = cor)
        Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaskGoBackgroundWhite
                    ),
                    border = BorderStroke(1.dp, TaskGoBorder)
                ) {
                    Column(
                Modifier.padding(16.dp)
            ) {
                eventos.forEachIndexed { i, evento ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (i == eventos.lastIndex) icone else Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (i == eventos.lastIndex) cor else TaskGoTextGrayLight,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            evento,
                            style = FigmaProductDescription,
                            color = if (i == eventos.lastIndex) cor else TaskGoTextGray
                        )
                    }
                    if (i != eventos.lastIndex) HorizontalDivider()
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onVerDetalhes(orderId) }) {
            Text("Ver detalhes do pedido")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onBackClick) {
            Text("Voltar")
        }
    }
}
