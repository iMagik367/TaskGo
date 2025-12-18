package com.taskgoapp.taskgo.feature.orders.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskgoapp.taskgo.core.theme.*

@Composable
fun DetalhesPedidoScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onRastrearPedido: (String) -> Unit,
    onVerResumo: (String) -> Unit,
    onEnviarPedido: ((String) -> Unit)? = null, // Para vendedores
    variant: String? = null
) {
    val status = when(variant) {
        "pending" -> "Aguardando pagamento"
        "canceled" -> "Pedido cancelado"
        else -> "A caminho"
    }
    val cor = when(variant) {
        "pending" -> TaskGoWarning
        "canceled" -> TaskGoError
        else -> TaskGoSuccess
    }
    val icone = when(variant) {
        "pending" -> Icons.Default.ErrorOutline
        "canceled" -> Icons.Default.Cancel
        else -> Icons.Default.ShoppingCart
    }
    val produtos = listOf(
        "Guarda-roupa 2 portas" to 750.0,
        "Colchão ortopédico" to 350.0
    )

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, null, tint = cor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Pedido #$orderId", style = FigmaSectionTitle, color = TaskGoTextBlack)
            Spacer(Modifier.weight(1f))
            Text(status, color = cor, style = FigmaSectionTitle)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Produtos", style = FigmaProductDescription, color = TaskGoTextGrayLight)
                produtos.forEach { (nome, valor) ->
                    Row {
                        Text(nome, Modifier.weight(1f), color = TaskGoTextBlack)
                        Text("R$ %.2f".format(valor), color = TaskGoPriceGreen)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Subtotal: R$ %.2f".format(produtos.sumOf { it.second }), style = FigmaPrice, color = TaskGoTextBlack)
                Text("Taxa de entrega: R$ 15.00", color = TaskGoTextGray)
                Text("Total: R$ %.2f".format(produtos.sumOf { it.second } + 15), style = FigmaSectionTitle, color = TaskGoPriceGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("Endereço de entrega:", color = TaskGoTextGrayLight, style = FigmaProductDescription)
                Text("[Endereço de entrega]", color = TaskGoTextBlack, style = FigmaProductName)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            // Botão de envio para vendedores (quando pedido está pago)
            if (onEnviarPedido != null && variant != "pending" && variant != "canceled") {
                Button(
                    onClick = { onEnviarPedido(orderId) },
                    colors = ButtonDefaults.buttonColors(containerColor = TaskGoGreen)
                ) {
                    Text("Confirmar Envio")
                }
            }
            Button(onClick = { onRastrearPedido(orderId) }, enabled = variant != "pending") {
                Text("Rastrear pedido")
            }
            OutlinedButton(onClick = { onVerResumo(orderId) }) {
                Text("Ver resumo")
            }
        }
        if(variant == "canceled") {
            Spacer(modifier = Modifier.height(18.dp))
            Text("Este pedido foi cancelado.", color = TaskGoError, style = FigmaProductDescription)
        }
    }
}
